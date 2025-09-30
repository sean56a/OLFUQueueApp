package com.example.olfuantipoloregistrarqueueingmanagementsystem

import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.*

abstract class VolleyMultipartRequest(
    method: Int,
    url: String,
    private val listener: Response.Listener<NetworkResponse>,
    private val errorListener: Response.ErrorListener
) : Request<NetworkResponse>(method, url, errorListener) {

    private val boundary = "apiclient-${System.currentTimeMillis()}"
    private val mimeType = "multipart/form-data;boundary=$boundary"

    abstract fun getByteData(): MutableMap<String, DataPart>?

    override fun getBodyContentType(): String = mimeType

    @Throws(AuthFailureError::class)
    override fun getBody(): ByteArray {
        val bos = ByteArrayOutputStream()
        val dos = DataOutputStream(bos)

        try {
            // Add text parameters
            val params = params
            if (params != null && params.isNotEmpty()) {
                for ((key, value) in params) {
                    buildTextPart(dos, key, value)
                }
            }

            // Add file bytes
            val data = getByteData()
            if (data != null) {
                for ((key, dataPart) in data) {
                    buildFilePart(dos, key, dataPart)
                }
            }

            dos.writeBytes("--$boundary--\r\n")
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return bos.toByteArray()
    }

    @Throws(IOException::class)
    private fun buildTextPart(dos: DataOutputStream, parameterName: String, parameterValue: String) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"$parameterName\"\r\n\r\n")
        dos.writeBytes(parameterValue)
        dos.writeBytes("\r\n")
    }

    @Throws(IOException::class)
    private fun buildFilePart(dos: DataOutputStream, inputName: String, dataFile: DataPart) {
        dos.writeBytes("--$boundary\r\n")
        dos.writeBytes("Content-Disposition: form-data; name=\"$inputName\"; filename=\"${dataFile.fileName}\"\r\n")
        dos.writeBytes("Content-Type: ${dataFile.type}\r\n\r\n")
        dos.write(dataFile.content)
        dos.writeBytes("\r\n")
    }

    override fun parseNetworkResponse(response: NetworkResponse): Response<NetworkResponse> {
        return Response.success(response, HttpHeaderParser.parseCacheHeaders(response))
    }

    override fun deliverResponse(response: NetworkResponse) {
        listener.onResponse(response)
    }

    override fun deliverError(error: VolleyError) {
        errorListener.onErrorResponse(error)
    }

    class DataPart(
        val fileName: String,
        val content: ByteArray,
        val type: String = "application/octet-stream"
    )
}
