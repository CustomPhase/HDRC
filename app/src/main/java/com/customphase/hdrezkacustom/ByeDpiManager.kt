package com.customphase.hdrezkacustom

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URL
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.*

class ByeDpiManager {
    private lateinit var process : Process

    suspend fun start(context: Context, useSettings : Boolean) = withContext(Dispatchers.IO) {

        if (isDomainReachable(context.getString(R.string.site_url))) return@withContext

        val nativeDir = context.applicationInfo.nativeLibraryDir
        val binFile = File(nativeDir, "libbyedpi.so")

        var args = ""
        if (useSettings) {
            val strategy = settings.byeDpiStrategyProp.getValue()
            args = "-H:rezka.ag $strategy"
        }
        val command = arrayOf(binFile.absolutePath, "-i", BYEDPI_PROXY_ADDRESS, "-p", BYEDPI_PROXY_PORT.toString()) +
                args.split("\\s+".toRegex()).toTypedArray()

        process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        /*Thread {
            process.inputStream.bufferedReader().forEachLine { Log.e("ByeDpi_Log", it) }
        }.start()*/

        if (!waitForProxy(BYEDPI_PROXY_PORT, 5000)) {
            throw Exception("Порт $BYEDPI_PROXY_PORT не открылся за 5 секунд")
        }

        Log.e("ByeDpi_Log", "Proxy started successfully")
        byeDpiStarted = true
    }

    private suspend fun waitForProxy(port: Int, timeoutMs: Long): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Socket(BYEDPI_PROXY_ADDRESS, port).use { return@withContext true }
            } catch (e: Exception) {
                delay(100)
            }
        }
        false
    }

    fun stop() {
        if (!byeDpiStarted) return
        process.destroy()
    }

    fun isDomainReachable(domainUrl: String): Boolean {
        return try {
            val sc = SSLContext.getInstance("TLS").apply {
                init(null, arrayOf(object : X509TrustManager {
                    override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                    override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) {}
                    override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                }), SecureRandom())
            }

            val url = URL(domainUrl)
            val connection = url.openConnection() as HttpsURLConnection

            connection.sslSocketFactory = sc.socketFactory
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }

            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.requestMethod = "HEAD"

            val code = connection.responseCode
            Log.e("ByeDpi_Log", "Check finished: $code")
            code in 200..299
        } catch (e: Exception) {
            Log.e("ByeDpi_Log", "Domain not reachable: ${e.message}. Starting byeDpi.")
            false
        }
    }
}