package com.squeegee.ruffinit

import android.content.Context
import android.location.Geocoder
import com.github.salomonbrys.kotson.*
import com.google.android.gms.maps.model.LatLng
import com.google.gson.JsonParser
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import org.jetbrains.anko.toast
import java.io.File


data class Animal(
    var reports: List<Report> = listOf()
) {
    companion object {
        /*
        Request:
        {
            left: 2.0,
            top: 1.0,
            right: 4.0,
            bottom: 2.0
        }

        JSON Response:
        {
            animals: [
                {
                    reports: [
                        imageUrl: String,
                        location: [Double, Double],
                        timestamp: Long,
                        neutered: Bool,
                        injured: Bool,
                        state: String (eg. "CA", "NV", "MA"),
                        type: String ["SIGHTING", "MISSING", "FOUND"]
                    ]
                }
            ]
        }
         */

        /**
         * @param loc location to search for nearby stray animals
         */
        fun retrieveNearby(loc: LatLng): List<Animal> {
            val width = 5.0
            val height = 5.0
            val left = loc.latitude - width / 2.0
            val top = loc.longitude - height / 2.0
            val right = loc.latitude + width / 2.0
            val bottom = loc.longitude + height / 2.0
            val res = JsonParser().parse(khttp.get("http://40.71.253.77:8080", params = mapOf(
                "left" to left.toString(),
                "top" to top.toString(),
                "right" to right.toString(),
                "bottom" to bottom.toString()
            )).text).obj

            return res["animals"].array.map { it.obj }.map {
                Animal(reports = it["reports"].array.map { it.obj }.map {
                    Report(
                        localUrl = "",
                        remoteUrl = it["imageUrl"].string,
                        location = LatLng(it["location"][0].double, it["location"][1].double),
                        timestamp = it["timestamp"].long,
                        extraInfo = ReportInfo(
                            it["neutered"].bool,
                            it["injured"].bool
                        ),
                        state = it["state"].string,
                        type = Report.Type.valueOf(it["type"].string)
                    )
                })
            }
        }

        fun byState(state: String): List<Animal> {
            val res = JsonParser().parse(khttp.get("http://40.71.253.77:8080", params = mapOf(
                "state" to state
            )).text).obj
            return res["animals"].array.map { it.obj }.map {
                Animal(reports = it["reports"].array.map { it.obj }.map {
                    Report(
                        localUrl = "",
                        remoteUrl = it["imageUrl"].string,
                        location = LatLng(it["location"][0].double, it["location"][1].double),
                        timestamp = it["timestamp"].long,
                        extraInfo = ReportInfo(
                            it["neutered"].bool,
                            it["injured"].bool
                        ),
                        state = it["state"].string,
                        type = Report.Type.valueOf(it["type"].string)
                    )
                })
            }
        }
    }
}

/**
 * Individual reporting of an animal with type, location, & other info
 */
data class Report(
    var localUrl: String = "",
    var remoteUrl: String = "https://googlechrome.github.io/samples/picture-element/images/butterfly.jpg",
    var location: LatLng = LatLng(0.0, 0.0),
    var timestamp: Long = System.currentTimeMillis(),
    var extraInfo: ReportInfo? = null,
    var state: String = "",
    var type: Report.Type = Type.SIGHTING
) {

    enum class Type {
        SIGHTING,
        MISSING,
        FOUND
    }

    /**
     * Publish this report to the server.
     * 1. Uploads the image (?)
     * 2. Sends the image url to the database (?)
     */
    fun publish(ctx: Context) {
        val f = File(localUrl)
        try {
            ImageManager.uploadImage(f.nameWithoutExtension, f.inputStream(), f.length())
            async(UI) {
                ctx.toast("Image Uploaded Successfully. Name = '${f.nameWithoutExtension}'")
            }
        } catch (e: Exception) {
            async(UI) { if (e.message != null) ctx.toast(e.message!!) }
        }


        // TODO: Reverse geocode

        val addrs = Geocoder(ctx).getFromLocation(location.latitude, location.longitude, 3)
        val state = if (addrs.isNotEmpty()) {
           addrs.first().adminArea
        } else null

        khttp.post("http://40.71.253.77:8080", data = jsonObject(
            "imageUrl" to remoteUrl,
            "location" to jsonArray(location.latitude, location.longitude),
            "timestamp" to timestamp,
            "neutered" to extraInfo?.neutered,
            "injured" to extraInfo?.injured,
            "type" to type.toString(),
            "state" to state
        ).toString())
    }
}