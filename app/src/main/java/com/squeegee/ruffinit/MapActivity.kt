package com.squeegee.ruffinit

import android.app.Activity
import android.content.Intent
import android.provider.MediaStore
import android.view.View
import android.view.ViewManager
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import org.jetbrains.anko.button
import org.jetbrains.anko.editText
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.toast
import org.jetbrains.anko.verticalLayout
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment

import android.os.Environment.DIRECTORY_PICTURES
import android.os.Handler
import android.support.v4.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast




class MapActivity: BaseActivity() {

    val REQUEST_IMAGE_CAPTURE = 1;
    lateinit var map: GoogleMap
    lateinit var mCurrentPhotoPath: String
    lateinit var imageUri: Uri
    val REQUEST_TAKE_PHOTO = 1;

    override fun createView(manager: ViewManager): View {
        return manager.verticalLayout {
            id = R.id.contentPanel

            val mapFrag = SupportMapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(id, mapFrag)
                .commit()

            mapFrag.getMapAsync {
                map = it
                afterLoad()
            }

            button("Camera") {
                onClick { dispatchTakePictureIntent() }
            }
            button("Print location") {
                onClick {
                    System.out.println(mCurrentPhotoPath)
                    UploadImage()
                }
            }
        }
    }

    fun afterLoad() {

    }

    fun addCoordinate(lat: Double, lon: Double, str: String) {
        map.addMarker(MarkerOptions().position(LatLng(lat, lon)).title(str))
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
//        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
//            val extras = data.extras
//            val imageBitmap = extras!!.get("data") as Bitmap
//            mImageView.setImageBitmap(imageBitmap)
//        }
//    }


    private fun dispatchTakePictureIntent() {
        var takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            var photoFile: File? = null;
            try {
                photoFile = createImageFile();
            } catch (ex: IOException) {
                // Error occurred while creating the File
                throw IOException("ERROR")
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                var photoURI: Uri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                imageUri = photoURI
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
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
                imageFileName, /* prefix */
                ".jpg", /* suffix */
                storageDir      /* directory */
        )

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath()
        return image
    }

    private fun UploadImage() {
        try {
            val imageStream = contentResolver.openInputStream(this.imageUri)
            val imageLength = imageStream!!.available()

            val handler = Handler()

            val th = Thread(Runnable {
                try {

                    val imageName = ImageManager.UploadImage(imageStream, imageLength)


                    handler.post(Runnable {
                        Toast.makeText(
                                this@MapActivity,
                                "Image Uploaded Successfully. Name = " + imageName,
                                Toast.LENGTH_SHORT
                        ).show()
                    })


                } catch (ex: Exception) {
                    val exceptionMessage = ex.message
                    handler.post(Runnable { Toast.makeText(this@MapActivity, exceptionMessage, Toast.LENGTH_SHORT).show() })
                }
            })
            th.start()
        } catch (ex: Exception) {

            Toast.makeText(this, ex.message, Toast.LENGTH_SHORT).show()
        }

    }

}