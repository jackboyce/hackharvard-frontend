package com.squeegee.ruffinit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.provider.MediaStore
import android.view.View
import android.view.ViewManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.jetbrains.anko.sdk25.coroutines.onClick
import android.net.Uri
import android.os.Bundle

import android.os.Environment.DIRECTORY_PICTURES
import android.support.v4.content.FileProvider
import android.widget.Switch
import com.getbase.floatingactionbutton.FloatingActionButton
import com.getbase.floatingactionbutton.FloatingActionsMenu
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.Deferred
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.*


class MapActivity: BaseActivity(), GoogleApiClient.ConnectionCallbacks {

    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var map: GoogleMap
    lateinit var mCurrentPhotoPath: String
    lateinit var imageUri: Uri
    val REQUEST_TAKE_PHOTO = 1
    private lateinit var gapi: GoogleApiClient

    override fun createView(manager: ViewManager): View {
        return manager.relativeLayout {
            id = R.id.contentPanel

            val mapFrag = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(id, mapFrag)
                .commit()

            mapFrag.getMapAsync {
                map = it
                map.isIndoorEnabled = false

                Dexter.withActivity(this@MapActivity)
                    .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                    .withListener(object: PermissionListener {
                        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                            try {
                                LocationServices.getFusedLocationProviderClient(ctx).lastLocation.addOnSuccessListener {
                                    map.moveCamera(CameraUpdateFactory.newLatLng(LatLng(it.latitude, it.longitude)))
                                }
                                map.isMyLocationEnabled = true
                            } catch (e: SecurityException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                        }

                        override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                        }
                    })
//                plotData()
            }

            floatingActionsMenu {
                elevation = dip(4).toFloat()
                addButton(FloatingActionButton(ctx).apply {
                    setIcon(R.drawable.ic_add)
                    title = "Report Missing"
                    onClick { dispatchTakePictureIntent() }
                })
            }.lparams {
                alignParentRight()
                alignParentBottom()
                margin = dip(20)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        gapi = GoogleApiClient.Builder(this)
            .addApi(LocationServices.API)
            .addConnectionCallbacks(this)
            .build()
    }

    override fun onStart() {
        super.onStart()
        gapi.connect()
    }

    override fun onStop() {
        super.onStop()
        gapi.disconnect()
    }

    override fun onConnected(b: Bundle?) {
        // TODO: Do runtime permission request
        Dexter.withActivity(this)
            .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener {
                override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                    try {
                        LocationServices.getFusedLocationProviderClient(ctx).lastLocation.addOnSuccessListener {
                            plotData(it)
                        }
                    } catch (e: SecurityException) {
                        e.printStackTrace()
                    }
                }

                override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                }

                override fun onPermissionDenied(response: PermissionDeniedResponse?) {
                }
            })
    }

    override fun onConnectionSuspended(reason: Int) {

    }

    fun plotData(loc: Location) {
        val nearby = Report.retrieveNearby(LatLng(loc.latitude, loc.longitude), 50.0)
        nearby.forEach {
            // TODO: Use (poly)lines to connect very similar reports.
            map.addMarker(MarkerOptions()
                .position(it.location)
                .visible(true)
            )
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val extras = data.extras
//            val imageBitmap = extras!!.get("data") as Bitmap
            uploadImage(imageUri)
        }
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                null
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                val photoURI: Uri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                imageUri = photoURI
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = getExternalFilesDir(DIRECTORY_PICTURES)
        val image = File.createTempFile(
            imageFileName, // prefix
            ".jpg", // extension
            storageDir
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun uploadImage(uri: Uri) {
        try {
            val imageStream = contentResolver.openInputStream(uri)
            val imageLength = imageStream!!.available()

            async(CommonPool) {
                try {
                    LocationServices.getFusedLocationProviderClient(ctx).lastLocation.addOnSuccessListener { loc ->
                        val now = System.currentTimeMillis()
                        val imageName = ImageManager.uploadImage(
                            now.toString() + ImageManager.randomString(5),
                            imageStream,
                            imageLength.toLong(),
                            LatLng(loc.latitude, loc.longitude)
                        )
                        async(UI) {
                            toast("Image Uploaded Successfully. Name = " + imageName)
                        }
                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            if (e.message != null) toast(e.message!!)
        }
    }

    fun fillInfoDialog(report: Report) {
        alert("Optional Extra Info") {
            var neutered: Switch? = null
            var injured: Switch? = null
            customView {
                neutered = switch {
                    text = "Neutered?"
                }
                injured = switch {
                    text = "Injured?"
                }
            }

            positiveButton("Report Animal") {
                report.extraInfo = ReportInfo(
                    neutered = neutered!!.isChecked,
                    injured = injured!!.isChecked
                )
                report.publish()
            }
            negativeButton("Cancel") {}
        }.show()
    }
}

data class ReportInfo(
    val neutered: Boolean = false,
    val injured: Boolean = false
)
