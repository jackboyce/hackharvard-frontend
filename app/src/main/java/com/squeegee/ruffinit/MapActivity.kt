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
import com.bumptech.glide.load.model.Headers
import com.getbase.floatingactionbutton.FloatingActionButton
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener
import khttp.get
import khttp.post
import khttp.structures.authorization.BasicAuthorization
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.*
import org.jetbrains.anko.custom.async
import org.jetbrains.anko.recyclerview.v7.recyclerView
import org.json.JSONArray
import org.json.JSONObject

class MapActivity: BaseActivity(), GoogleApiClient.ConnectionCallbacks, GoogleMap.OnInfoWindowClickListener , GoogleMap.InfoWindowAdapter{

    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var map: GoogleMap
    lateinit var mCurrentPhotoPath: String
    var imageFile: File? = null
    val REQUEST_TAKE_PHOTO = 1
    private lateinit var gapi: GoogleApiClient
    private var currentLoc: LatLng? = null

    override fun createView(manager: ViewManager): View = manager.slidingUpPanelLayout {

//        async(CommonPool) {
//            val r = get("http://40.71.253.77:8080/test")
//            println("JSON")
//            val test = r.jsonObject.get("test")
//            println(test)
//        }
//        /get_dog_by_geo/
//            async(CommonPool) {
//                val r = get("http://40.71.253.77:8080/get_all_dogs/")
//                //println("JSON")
//                val jsonReport = r.jsonObject
//                async(UI) {
//                    println("JSON PRINTING INNER")
//                }
//                println("JSON PRINTING")
//                println(jsonReport.toString())
//                        for (i in 0 until jsonReport.length(length)) {
//                            var scopeJson = jsonReport.getJSONObject(i)
//                            var tempReport = Report(
//                                    state=((scopeJson as JSONObject).get("state") as String),
//                                    location= LatLng(((scopeJson as JSONObject).get("geo_long") as Double), ((scopeJson as JSONObject).get("geo_lat") as Double)),
//                                    remoteUrl=((scopeJson as JSONObject).get("img_url") as String)
//                            )
//                            println(tempReport.state)
//                            println(tempReport.location)
//                            println(tempReport.remoteUrl)
//                            drawMarker(tempReport)
//                        }
//                println(test.toString())
//            }
//            async(CommonPool) {
//                val r = get("http://40.71.253.77:8080/get_all_dogs/")
//                //println("JSON")
////                val jsonReport = (r.jsonObject.get("animals") as JSONArray)
//                val jsonReport = r.jsonObject
//                println(jsonReport)
////                for (i in 0 until jsonReport.length()) {
////                    var scopeJson = jsonReport.getJSONObject(i)
////                    var tempReport = Report(
////                            state=((scopeJson as JSONObject).get("state") as String),
////                            location= LatLng(((scopeJson as JSONObject).get("geo_long") as Double), ((scopeJson as JSONObject).get("geo_lat") as Double)),
////                            remoteUrl=((scopeJson as JSONObject).get("img_url") as String)
////                    )
////                    println(tempReport.state)
////                    println(tempReport.location)
////                    println(tempReport.remoteUrl)
////                    drawMarker(tempReport)
////                }
//                //println(test.toString())
//             }


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
                //drawMarker(Report)
//                Animal().reports.forEach {
//                    drawMarker(it)
//                }
                async(CommonPool) {
                    val r = get("http://40.71.253.77:8080/get_all_dogs/")
                    val jsonReport = r.jsonObject.get("animals") as JSONArray
                    async(UI) {
                        this@MapActivity.loadMarkers(jsonReport)
                    }

                }
                //map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLoc.latitude, currentLoc.longitude))
//                plotData()
            }

            floatingActionsMenu {
                elevation = dip(4).toFloat()
                addButton(FloatingActionButton(ctx).apply {
                    setIcon(R.drawable.ic_camera_alt)
                    post { title = "Report Missing" }
                    onClick {
                        dispatchTakePictureIntent()
                        //uploadImage()
                    }
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

    fun loadMarkers(jsonReport: JSONArray) {

        drawMarker(Animal().reports[0])

        println(jsonReport.toString())
        println(jsonReport.length())
        for (i in 0..(jsonReport.length() - 1)) {
            println(i)
            var scopeJson = jsonReport.get(i)
            var tempReport = Report(
                    state=((scopeJson as JSONObject).get("state") as String),
                    location= LatLng(((scopeJson as JSONObject).get("geo_lat") as Double), ((scopeJson as JSONObject).get("geo_long") as Double)),
                    remoteUrl=((scopeJson as JSONObject).get("img_url") as String),
                    id=((scopeJson as JSONObject).get("dog_id") as Int)
            )
            println(tempReport.state)
            println(tempReport.location)
            println(tempReport.remoteUrl)
            drawMarker(tempReport)
        }
    }
///get_dog_by_id/NUMBER
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
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLoc, 10f))



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

    fun animalTest(animal: Animal) {
        if(animal.reports.size == 0) {
            toast("Animal has no locations")
        } else {
            //
        }


    }

    fun drawMarker(rep: Report) {
        println(rep.location)

        map.addMarker(MarkerOptions()
                .position(rep.location)
                .visible(true)
                .title(rep.remoteUrl)
                )
        map.setInfoWindowAdapter(this)
    }

    fun getAllDogs() {

    }

//    Request:
//    {
//        left: 2.0,
//        top: 1.0,
//        right: 4.0,
//        bottom: 2.0
//    }
//  result
    // result["Request"]["left"]

//    JSON Response:
//    {
//        animals: [
//        {
//            reports: [
//            imageUrl: String,
//            location: [Double, Double],
//            timestamp: Long,
//            neutered: Bool,
//            injured: Bool,
//            state: String (eg. "CA", "NV", "MA"),
//            type: String ["SIGHTING", "MISSING", "FOUND"]
//            ]
//        },

//        ]
//    }

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

            println(marker.title)

            Glide.with(this@MapActivity).load(marker.title).into(imView)


        }
    }




    override fun getInfoWindow(marker: Marker): View? {
        return null
    }

    override fun onInfoWindowClick(marker: Marker) {
        var intent = Intent(this, ReportInfoActivity::class.java )
        intent.putExtra("id", marker.title)
        startActivity(intent)
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


    private fun dispatchTakePictureIntent(): Uri? {
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
                var uri = FileProvider.getUriForFile(this, "com.example.android.fileprovider", imageFile)
//                imageUri = Uri.parse(photoFile.absolutePath)
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO)
                //uploadImage(uri)
            }


        }
        return null
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
//        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "${System.currentTimeMillis()}" + ImageManager.randomString(5)//_${currentLoc!!.latitude as Int}_${currentLoc!!.longitude as Int}"
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
        println("Image name: " + imageName)
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

                        val r = post("https://maps.googleapis.com/maps/api/geocode/json?" + "latlng=" + currentLoc?.latitude + "," + currentLoc?.longitude + "&key=AIzaSyDSM2Qo6h_VCwFJGFm0BU527LZvlMqcXog")
                        //println("Image Name: " + imageName)
                        val address = (((r.jsonObject.get("results") as JSONArray).get(0) as JSONObject).get("address_components") as JSONArray).get(4)

                        val payload = mapOf("geo_long" to currentLoc?.longitude, "geo_lat" to currentLoc?.latitude, "img_url" to "https://hackharvarddiag310.blob.core.windows.net/images/" + imageName, "timestamp_img" to ((System.currentTimeMillis()/1000) as String), "nuetered" to neutered, "type" to "SIGHTING", "injured" to injured, "geo_lat" to currentLoc?.latitude)
                        val res = post("http://40.71.253.77:8080/insert_dog", data=payload)

                        println(res.text)
//                https://l.facebook.com/l.php?u=https%3A%2F%2Fhackharvarddiag310.blob.core.windows.net%2Fimages%2FJPEG_20171021_135907_&h=ATPmyBPxBMWjRRveIN9wRyAP_0c7H6fMcRveG5BAgtBlXuCPMGvqhb93IBkJdxJwQbSAMgmfZ5vgVgsyBJKQMngNscpfDgAYbz4f2r7015TAhUYPuQDTvKv7IXyj1fO921TUIyuLcQ1Q8f8bjSQ
//                geo_long : longitude, geo_lat: lat, img_url: image url, timestamp_img : timstamp (epcoh), nuetered: boolean, type: {SIGHTING, MISSING, FOUND}

//                "https://maps.googleapis.com/maps/api/geocode/json?latlng=40.714224,-73.961452&key=AIzaSyDSM2Qo6h_VCwFJGFm0BU527LZvlMqcXog"
//                    }
//                } catch (e: SecurityException) {
//                    e.printStackTrace()
//                }
            }
        } catch (e: Exception) {
            if (e.message != null) toast(e.message!!)
        }
        println("Image name: " + imageName)
        return imageName
    }

    var neutered: Switch? = null
    var injured: Switch? = null

    fun fillInfoDialog(report: Report) {
        alert("Optional Extra Info") {
            neutered = null
            injured = null
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
