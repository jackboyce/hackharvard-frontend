package com.squeegee.ruffinit

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.provider.MediaStore
import android.view.View
import android.view.ViewManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import org.jetbrains.anko.sdk25.coroutines.onClick
import android.net.Uri
import android.os.Bundle

import android.os.Environment.DIRECTORY_PICTURES
import android.support.v4.content.FileProvider
import android.view.Gravity
import android.widget.Switch
import android.widget.Toast
import com.bumptech.glide.Glide
import com.getbase.floatingactionbutton.FloatingActionButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
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
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.recyclerView

class MapActivity: BaseActivity(), GoogleApiClient.ConnectionCallbacks, GoogleMap.OnInfoWindowClickListener , GoogleMap.InfoWindowAdapter{

    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var map: GoogleMap
    lateinit var mCurrentPhotoPath: String
    var imageFile: File? = null
    val REQUEST_TAKE_PHOTO = 1
    private lateinit var gapi: GoogleApiClient
    private var currentLoc: LatLng? = null

    override fun createView(manager: ViewManager): View = manager.slidingUpPanelLayout {
        setGravity(Gravity.BOTTOM)
        panelHeight = dip(64)

        // Main content
        relativeLayout {
            id = R.id.contentPanel

            val mapFrag = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(id, mapFrag)
                .commit()

            mapFrag.getMapAsync {
                map = it
                map.setOnInfoWindowClickListener(this@MapActivity);
                map.isIndoorEnabled = false
//                plotData()
            }

            floatingActionsMenu {
                elevation = dip(4).toFloat()
                addButton(FloatingActionButton(ctx).apply {
                    setIcon(R.drawable.ic_camera_alt)
                    post { title = "Report Missing" }
                    onClick { dispatchTakePictureIntent() }
                })
            }.lparams {
                alignParentRight()
                alignParentBottom()
                margin = dip(20)
            }
        }

        // Bottom panel
        verticalLayout {
            textView("Reported Lost")
            recyclerView {

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
                            currentLoc = LatLng(it.latitude, it.longitude)
                            plotData(it)
                            map.animateCamera(CameraUpdateFactory.newLatLng(currentLoc))
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
            .check()
    }

    override fun onConnectionSuspended(reason: Int) {

    }

    fun plotData(loc: Location) {
        val nearby = Animal.retrieveNearby(LatLng(loc.latitude, loc.longitude))
        nearby.forEach { nearby ->
            // Use (poly)lines to connect very similar reports.
            nearby.reports.forEach { a ->
                map.addMarker(MarkerOptions()
                    .position(a.location)
                    .visible(true)
                    .title(a.remoteUrl))
                map.setInfoWindowAdapter(this)

                nearby.reports.filter { it !== a }.forEach { b ->
                    var line: Polyline = map.addPolyline(PolylineOptions()
                        .add(a.location, b.location)
                        .width(5f)
                        .color(Color.RED))
                }
            }
        }
    }

    override fun getInfoContents(marker: Marker): View {
        return AnkoContext.create(ctx, this).linearLayout {
            //imageView()
            val imView = imageView {

            }.lparams(width=dip(100), height=dip(100))

            Glide.with(this).load(marker.title).into(imView)
        }
    }


    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    override fun onInfoWindowClick(marker: Marker) {
        toast("Info window clicked")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
//            val imageBitmap = extras!!.get("data") as Bitmap
//            val name = uploadImage(Uri.parse(imageFile!!.absolutePath))
            fillInfoDialog(Report(
                localUrl = imageFile!!.absolutePath,
                remoteUrl = imageFile!!.name, // TODO: Add server prefix
                location = currentLoc ?: LatLng(0.0, 0.0)
            ))
        }
    }


    private fun dispatchTakePictureIntent() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            imageFile = try {
                createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                null
            }

            // Continue only if the File was successfully created
            if (imageFile != null) {
                val uri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", imageFile)
//                imageUri = Uri.parse(photoFile.absolutePath)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "${System.currentTimeMillis()}_${currentLoc!!.latitude}_${currentLoc!!.longitude}"
        getExternalFilesDir(DIRECTORY_PICTURES).resolve("ruffin_it/").mkdirs()
        val image = getExternalFilesDir(DIRECTORY_PICTURES).resolve("ruffin_it/$imageFileName.jpg")
        image.createNewFile()

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.absolutePath
        return image
    }

    private fun uploadImage(uri: Uri): String {
        val now = System.currentTimeMillis()
        val imageName = now.toString() + ImageManager.randomString(5)
        try {
            val imageStream = contentResolver.openInputStream(uri)
            val imageLength = imageStream!!.available()

            async(CommonPool) {
//                try {
//                    LocationServices.getFusedLocationProviderClient(ctx).lastLocation.addOnSuccessListener { loc ->
                        ImageManager.uploadImage(
                            imageName,
                            imageStream,
                            imageLength.toLong()
                        )

                        async(UI) {
                            toast("Image Uploaded Successfully. Name = " + imageName)
                        }
//                    }
//                } catch (e: SecurityException) {
//                    e.printStackTrace()
//                }
            }
        } catch (e: Exception) {
            if (e.message != null) toast(e.message!!)
        }
        return imageName
    }

    fun fillInfoDialog(report: Report) {
        alert("Optional Extra Info") {
            var neutered: Switch? = null
            var injured: Switch? = null
            customView {
                verticalLayout {
                    neutered = switch {
                        text = "Neutered?"
                    }
                    injured = switch {
                        text = "Injured?"
                    }
                }
            }

            positiveButton("Report Animal") {
                report.extraInfo = ReportInfo(
                    neutered = neutered!!.isChecked,
                    injured = injured!!.isChecked
                )
                async(CommonPool) { report.publish(ctx) }
            }
            negativeButton("Cancel") {}
        }.show()
    }
}

data class ReportInfo(
    val neutered: Boolean = false,
    val injured: Boolean = false
)
