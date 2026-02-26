package com.customphase.hdrezkacustom

import android.content.Context
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import java.security.cert.X509Certificate
import java.util.Locale
import javax.net.ssl.*

class HDRezkaParser(val context: Context) {

    private val cookieJar = object : CookieJar {
        // Храним куки по их имени, чтобы обновлять только нужные
        private val cookieStore = mutableMapOf<String, Cookie>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookies.forEach { cookie ->
                // Если сервер прислал куку, которая уже просрочена (max-age=0),
                // мы удаляем её из нашего хранилища вместо того, чтобы сохранять "deleted"
                if (cookie.expiresAt <= System.currentTimeMillis()) {
                    cookieStore.remove(cookie.name)
                } else {
                    cookieStore[cookie.name] = cookie
                }
            }
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val currentTime = System.currentTimeMillis()
            // Фильтруем: только те, что подходят домену и еще не протухли
            return cookieStore.values.filter {
                it.matches(url) && it.expiresAt > currentTime
            }
        }
    }

    private val unsafeOkHttpClient: OkHttpClient
    get() {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            })

            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustAllCerts, SecureRandom())

            OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true } // любой hostname
                .followRedirects(true)
                .build()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    val client = unsafeOkHttpClient.newBuilder().cookieJar(cookieJar).build()

    private fun Request.Builder.addHDRezkaHeaders() : Request.Builder {
            return this
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
            .addHeader("Accept", "*/*")
            .addHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .addHeader("Host", "rezka.ag")
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            .addHeader("Referer", "https://rezka.ag/")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .addHeader("Pragma", "no-cache")
            .addHeader("Sec-Ch-Ua", "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\", \"Google Chrome\";v=\"144\"")
            .addHeader("Sec-Ch-Ua-Mobile", "?0")
            .addHeader("Sec-Ch-Ua-Platform", "\"Windows\"")
            .addHeader("Sec-Fetch-Dest", "document")
            .addHeader("Sec-Fetch-Mode", "cors")
            .addHeader("Sec-Fetch-Site", "same-origin")
            .addHeader("Sec-Fetch-User", "?1")
    }

    // Выполнение HTTP-запроса
    private suspend fun makeRequest(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .addHDRezkaHeaders()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("Request failed: error ${response.code}")
                    null
                } else {
                    response.body?.string()
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    suspend fun warmup() {
        val url = context.getString(R.string.site_url)
        makeRequest(url)
    }

    suspend fun search(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        val encodedQuery = java.net.URLEncoder.encode(query, "utf-8")
        val url = "${context.getString(R.string.site_url)}/search/?do=search&subaction=search&q=$encodedQuery"
        val html = makeRequest(url) ?: return results

        val doc = Jsoup.parse(html)
        val items = doc.select("div.b-content__inline_item")

        for (item in items) {
            val linkDiv = item.selectFirst("div.b-content__inline_item-link")
            val coverDiv = item.selectFirst("div.b-content__inline_item-cover")

            var title = "!!! PARSER BROKE !!!"
            var url = ""
            var imageUrl = ""
            var info = ""

            if (linkDiv != null) {
                val linkElement = linkDiv.selectFirst("a")
                if (linkElement != null) {
                    title = linkElement.text()
                    url = linkElement.attr("href")
                }
                if (coverDiv != null) {
                    val typeText = coverDiv.select("span.cat").text()
                    info = "($typeText) "
                }
                val infoElement = linkDiv.select("div")[1]
                if (infoElement != null) info += infoElement.text()
            }

            if (coverDiv != null) {
                val coverElement = coverDiv.selectFirst("img")
                if (coverElement != null) imageUrl = coverElement.attr("src")
            }
            results.add(SearchResult(title, url, imageUrl, info))
        }

        return results
    }

    suspend fun getMediaItem(itemUrl: String) : MediaItem {
        val html = makeRequest(itemUrl) ?: return MediaItem()
        val doc = Jsoup.parse(html)

        var initCdnPattern = Regex("""initCDNMoviesEvents\(\s*(\d+)\s*,\s*(\d+)\s*,""")
        var initCdn = initCdnPattern.find(doc.html())

        if (initCdn == null) {
            initCdnPattern = Regex("""initCDNSeriesEvents\(\s*(\d+)\s*,\s*(\d+)\s*,""")
            initCdn = initCdnPattern.find(doc.html())
        }

        val title = doc.selectFirst("div.b-post__title h1")?.text() ?: "None"
        val description = doc.selectFirst("div.b-post__description_text")?.text() ?: "None"
        val id = initCdn?.groupValues[1]?.toInt() ?: 0
        val defaultTranslatorId = initCdn?.groupValues[2]?.toInt() ?: 0

        return MediaItem(
            id = id,
            title = title,
            description = description,
            defaultTranslatorId = defaultTranslatorId,
            translators = parseMediaSelections(doc, ".b-translator__item"),
            seasons = parseMediaSelections(doc, ".b-simple_season__item"),
            episodes = parseMediaSelections(doc, ".b-simple_episode__item"),
        )
    }

    suspend private fun parseMediaSelections(doc: Document, query : String) : MutableList<MediaItemSelection>{
        val ret = mutableListOf<MediaItemSelection>()
        val elements = doc.select(query)
        for(item in elements) {

            var translatorId = 0
            if (item.hasAttr("data-translator_id")) translatorId = item.attr("data-translator_id").toInt()

            var seasonId = 0
            if (item.hasAttr("data-tab_id")) seasonId = item.attr("data-tab_id").toInt()
            if (item.hasAttr("data-season_id")) seasonId = item.attr("data-season_id").toInt()

            var episodeId = 0
            if (item.hasAttr("data-episode_id")) episodeId = item.attr("data-episode_id").toInt()

            ret.add(MediaItemSelection(
                item.text(),
                item.hasClass("active"),
                translatorId,
                seasonId,
                episodeId,
                item.attr("data-director")?.toBoolean() ?: false
            ))
        }
        return ret
    }

    suspend fun getMediaStreamUrl(
        itemId : Int,
        translatorId : Int,
        seasonId : Int,
        episodeId : Int,
        isDirector : Boolean
    ) : Map<String, String> {
        val isMovie = seasonId <= 0
        val actionType = if (isMovie) "movie" else "stream"

        val formBody = FormBody.Builder()
            .add("id", itemId.toString())
            .add("translator_id", translatorId.toString())
            .add("season", seasonId.toString())
            .add("episode", episodeId.toString())
            .add("is_camrip", "0")
            .add("is_ads", "0")
            .add("is_director", if (isDirector) "1" else "0")
            .add("action", "get_$actionType")
            .build()

        val request = Request.Builder()
            .url("${context.getString(R.string.site_url)}/ajax/get_cdn_series/")
            .post(formBody)
            .addHDRezkaHeaders()
            .addHeader("X-Requested-With", "XMLHttpRequest")   // important for AJAX detection
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) {
                println("Request failed: error ${response.code}")
                null
            } else {
                val jsonObject = JSONObject(body.string())
                val urls = jsonObject.getString("url").toString().split(",")
                val qualities = mutableMapOf<String, String>()
                for (url in urls) {
                    val parts = url.split(" or ")
                    val quality = parts[0].substringAfter("[").substringBefore("]")
                    val link = parts[0].substringAfter("]").replace("\\/", "/")
                    qualities[quality] = link
                }
                return qualities
            }
        }

        return mapOf()
    }

    suspend fun getMediaEpisodes(itemId : Int, translatorId : Int) : MediaItem {
        val formBody = FormBody.Builder()
            .add("id", itemId.toString())
            .add("translator_id", translatorId.toString())
            .add("action", "get_episodes")
            .build()

        val request = Request.Builder()
            .url("${context.getString(R.string.site_url)}/ajax/get_cdn_series/")
            .post(formBody)
            .addHDRezkaHeaders()
            .addHeader("X-Requested-With", "XMLHttpRequest")   // important for AJAX detection
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body
            if (!response.isSuccessful || body == null) {
                println("Request failed: error ${response.code}")
                return MediaItem()
            } else {
                val jsonObject = JSONObject(body.string())
                val seasonsDoc = Jsoup.parse(jsonObject.getString("seasons"))
                val episodesDoc = Jsoup.parse(jsonObject.getString("episodes"))
                return MediaItem(
                    itemId,
                    "",
                    "",
                    0,
                    listOf(),
                    parseMediaSelections(seasonsDoc, ".b-simple_season__item"),
                    parseMediaSelections(episodesDoc, ".b-simple_episode__item")
                )
            }
        }
    }

    suspend fun login(login : String, password : String) {
        val formBody = FormBody.Builder()
            .add("login_name", login)
            .add("login_password", password)
            .add("login_not_save", "0")
            .build()

        val request = Request.Builder()
            .url("${context.getString(R.string.site_url)}/ajax/login/")
            .post(formBody)
            .addHDRezkaHeaders()
            .addHeader("X-Requested-With", "XMLHttpRequest")   // important for AJAX detection
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body
            println("LOGIN: ${body?.string()}")
            if (!response.isSuccessful || body == null) {
                println("Request failed: error ${response.code}")
            }
        }
    }

    suspend fun saveProgress(itemId : Int,
                             translatorId: Int,
                             seasonId: Int,
                             episodeId: Int,
                             currentTime : Double,
                             duration : Double
    ) {
        var formBody = FormBody.Builder()
            .add("action", "add")
            .add("id", itemId.toString())
            .add("translator_id", translatorId.toString())
            .build()

        var request = Request.Builder()
            .url("${context.getString(R.string.site_url)}/ajax/send_watching/")
            .post(formBody)
            .addHDRezkaHeaders()
            .addHeader("X-Requested-With", "XMLHttpRequest")   // important for AJAX detection
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body
            println("SENDWATCH: response code (${response.code}). ${body?.string()}")
            if (!response.isSuccessful || body == null) {
                println("Request failed: error ${response.code}")
            }
        }

        formBody = FormBody.Builder()
            .add("post_id", itemId.toString())
            .add("translator_id", translatorId.toString())
            .add("season", seasonId.toString())
            .add("episode", episodeId.toString())
            .add("current_time", String.format(Locale.ENGLISH, "%.4f", currentTime))
            .add("duration", String.format(Locale.ENGLISH, "%.4f", duration))
            .build()

        request = Request.Builder()
            .url("${context.getString(R.string.site_url)}/ajax/send_save/")
            .post(formBody)
            .addHDRezkaHeaders()
            .addHeader("X-Requested-With", "XMLHttpRequest")   // important for AJAX detection
            .build()

        println("Cookies for request: ${client.cookieJar.loadForRequest(request.url)}")

        client.newCall(request).execute().use { response ->
            val body = response.body
            println("SAVE: response code (${response.code}). ${body?.string()}")
            if (!response.isSuccessful || body == null) {
                println("Request failed: error ${response.code}")
            }
        }
    }
}