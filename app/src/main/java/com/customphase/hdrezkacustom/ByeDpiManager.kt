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

    suspend fun start(context: Context) = withContext(Dispatchers.IO) {

        if (isDomainReachable(context.getString(R.string.site_url))) return@withContext

        val nativeDir = context.applicationInfo.nativeLibraryDir
        val binFile = File(nativeDir, "libbyedpi.so")

        val args = "-H:rezka.ag -s1 -q1 -r1+s -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1"
        // Запускаем через ProcessBuilder, а НЕ через JNI
        val command = arrayOf(binFile.absolutePath, "-i", BYEDPI_PROXY_ADDRESS, "-p", BYEDPI_PROXY_PORT.toString()) +
                args.split("\\s+".toRegex()).toTypedArray()

        process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        // Читаем логи, чтобы видеть, что происходит внутри ByeDPI
        Thread {
            process.inputStream.bufferedReader().forEachLine { Log.e("ByeDPI_LOG", it) }
        }.start()

        if (!waitForProxy(BYEDPI_PROXY_PORT, 5000)) {
            throw Exception("Порт $BYEDPI_PROXY_PORT не открылся за 5 секунд")
        }

        Log.e("ByeDPI_LOG", "Proxy started successfully")
        byeDpiStarted = true
    }

    private suspend fun waitForProxy(port: Int, timeoutMs: Long): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Socket(BYEDPI_PROXY_ADDRESS, port).use { return@withContext true }
            } catch (e: Exception) {
                delay(100) // Ждем чуть-чуть перед следующей попыткой
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
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sc = SSLContext.getInstance("SSL")
            sc.init(null, trustAllCerts, SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)

            HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }

            val url = URL(domainUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 3000
            connection.requestMethod = "HEAD"

            Log.e("DomainCheck", "Domain is reachable without proxy, continue")
            // A 200-level code generally means the resource is available
            connection.responseCode in 200..299
        } catch (e: IOException) {
            Log.e("DomainCheck", e.toString())
            false
        } catch (e: Exception) {
            Log.e("DomainCheck", e.toString())
            false
        }
    }
}