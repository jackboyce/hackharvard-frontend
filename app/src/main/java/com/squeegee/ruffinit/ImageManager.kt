package com.squeegee.ruffinit

import com.google.android.gms.maps.model.LatLng
import com.microsoft.azure.storage.CloudStorageAccount
import com.microsoft.azure.storage.blob.CloudBlobContainer
import com.microsoft.azure.storage.blob.CloudBlockBlob
import java.io.InputStream
import java.io.OutputStream
import java.security.SecureRandom
import java.util.*



object ImageManager {
    private val storageConnectionString = ("DefaultEndpointsProtocol=https;"
        + "AccountName=hackharvarddiag310;"
        + "AccountKey=x7tCJRfbJGCLo9a1hencOpCGZXq9sggJZpL8rVCHXFRZfLj8mPkJlTEUMzD5PS03KPZOxa2cu5n1Q2f83dogwA==")

    // Retrieve storage account from connection-string.
    // Create the blob client.
    // Get a reference to a container.
    // The container name must be lower case
    private val container: CloudBlobContainer
        @Throws(Exception::class)
        get() {
            val storageAccount = CloudStorageAccount.parse(storageConnectionString)
            val blobClient = storageAccount.createCloudBlobClient()

            return blobClient.getContainerReference("images")
        }

    private val validChars = "abcdefghijklmnopqrstuvwxyz"
    private var rnd = SecureRandom()

    @Throws(Exception::class)
    fun uploadImage(name: String, image: InputStream, imageLength: Long): String {
        val container = container

        container.createIfNotExists()

        // Uploads to Azure
        val imageBlob = container.getBlockBlobReference(name)
        imageBlob.upload(image, imageLength)

        // TODO: Send url, once uploaded, to the database for geographic pooling and adding to Clarifai dataset

        return name

    }

    @Throws(Exception::class)
    fun listImages(): List<String> {
        val container = container

        val blobs = container.listBlobs()
        return blobs.map { (it as CloudBlockBlob).name }
    }

    @Throws(Exception::class)
    fun getImage(name: String, imageStream: OutputStream) {
//        var imageLength = imageLength
        val container = container

        val blob = container.getBlockBlobReference(name)

        if (blob.exists()) {
            blob.downloadAttributes()

//            imageLength = blob.properties.length

            blob.download(imageStream)
        }
    }

    fun randomString(len: Int): String {
        val sb = StringBuilder(len)
        for (i in 0 until len)
            sb.append(validChars[rnd.nextInt(validChars.length)])
        return sb.toString()
    }
}
