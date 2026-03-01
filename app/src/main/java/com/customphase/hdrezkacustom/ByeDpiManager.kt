package com.customphase.hdrezkacustom

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Socket

class ByeDpiManager {
    suspend fun start(context: Context) = withContext(Dispatchers.IO) {
        val nativeDir = context.applicationInfo.nativeLibraryDir
        val binFile = File(nativeDir, "libbyedpi.so")

        val args = "-H:rezka.ag -s1 -q1 -r1+s -a1 -Ar -o1 -a1 -At -f-1 -r1+s -a1"
        // Запускаем через ProcessBuilder, а НЕ через JNI
        //val command = arrayOf(binFile.absolutePath, "-i", "127.0.0.1", "-f", "-1")
        val command = arrayOf(binFile.absolutePath, "-i", "127.0.0.1", "-p", "1080") +
                args.split("\\s+".toRegex()).toTypedArray()

        val process = ProcessBuilder(*command)
            .redirectErrorStream(true)
            .start()

        // Читаем логи, чтобы видеть, что происходит внутри ByeDPI
        Thread {
            process.inputStream.bufferedReader().forEachLine { Log.e("ByeDPI_LOG", it) }
        }.start()

        if (!waitForProxy(1080, 5000)) {
            throw Exception("Порт 1080 не открылся за 5 секунд")
        }

        Log.e("ByeDPI_LOG", "Proxy started successfully")
    }

    private suspend fun waitForProxy(port: Int, timeoutMs: Long): Boolean = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            try {
                Socket("127.0.0.1", port).use { return@withContext true }
            } catch (e: Exception) {
                delay(100) // Ждем чуть-чуть перед следующей попыткой
            }
        }
        false
    }
}