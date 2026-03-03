package com.customphase.hdrezkacustom

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.annotation.Keep
import androidx.core.graphics.createBitmap
import java.io.ByteArrayOutputStream

@Keep
data class WatchHistoryItem(
    var selection: MediaItemSelection = MediaItemSelection(),
    var title : String = "",
    var currentTime: Long = 0,
    var screenshotBase64 : String = ""
) {
    fun getScreenshotBitmap() : Bitmap {
        val byteArray = Base64.decode(screenshotBase64, Base64.NO_WRAP)
        if (byteArray == null || byteArray.isEmpty()) {
            return createBitmap(1, 1)
        }
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    fun setScreenshot(screenshot : Bitmap) {
        val stream = ByteArrayOutputStream()
        screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)
        screenshotBase64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}

@Keep
class WatchHistory(
    private val map: MutableMap<Int, WatchHistoryItem> = mutableMapOf()
) : MutableMap<Int, WatchHistoryItem> by map {
}
