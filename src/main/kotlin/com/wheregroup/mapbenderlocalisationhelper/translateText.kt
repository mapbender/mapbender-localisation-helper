package com.wheregroup.mapbenderlocalisationhelper

import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import org.json.JSONObject
import org.json.JSONTokener
import java.io.OutputStreamWriter

fun translateText(source: String, target: String, text: String): String {
    // The URL of the server endpoint
    val url = URL("http://127.0.0.1:5000/translate")

    // Create the JSON body using JSONObject
    val jsonBody = JSONObject().apply {
        put("q", text)
        put("source", source)
        put("target", target)
        put("format", "text")
        put("api_key", "")
    }
    val payload = jsonBody.toString();

    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "POST"
    connection.doOutput = true // Indicates that we want to write data to the connection
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Content-Length", payload.length.toString())

    OutputStreamWriter(connection.outputStream).use { outputStream ->
        outputStream.write(payload)
    }

    var response: Any = ""

    try {
        val responseCode = connection.responseCode
        println("Response Code: $responseCode")

        // Read the response from the server
        val responseToken = connection.inputStream.bufferedReader().use { it.readText() }.let { JSONTokener(it) }
        val responseObject =  JSONObject(responseToken)
        response = responseObject.get("translatedText")
        connection.disconnect()
        return response.toString()
    } catch(e: Exception) {
        connection.disconnect()
        throw e
    }
}
