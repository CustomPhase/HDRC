package com.customphase.hdrezkacustom

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Socket

class ByeDpiManager {

    private lateinit var process : Process

    suspend fun start(context: Context) = withContext(Dispatchers.IO) {
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
        process.destroy()
    }
}