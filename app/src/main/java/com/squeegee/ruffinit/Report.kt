package com.squeegee.ruffinit

import com.google.android.gms.maps.model.LatLng

/**
 * Individual reporting of an animal with type, location, & other info
 */
data class Report(
    var imageUrl: String = "",
    var location: LatLng = LatLng(0.0, 0.0),
    var similarImages: List<String> = listOf(),
    var timestamp: Long = System.currentTimeMillis(),
    var extraInfo: ReportInfo? = null
) {
    companion object {
        /**
         * @param loc location to search for nearby stray animals
         * @param radius in miles to search away from the current location
         */
        fun retrieveNearby(loc: LatLng, radius: Double): List<Report> {
            return listOf()
        }
    }

    /**
     * Publish this report to the server.
     * 1. Uploads the image (?)
     * 2. Sends the image url to the database (?)
     */
    fun publish() {

    }
}