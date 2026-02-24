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
import javax.net.ssl.*

class HDRezkaParser(val context: Context) {

    private val cookieJar = object : CookieJar {
        private val cookieStore = mutableMapOf<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            cookieStore[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookieStore[url.host] ?: emptyList()
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

    private fun Request.Builder.addHeaders() : Request.Builder {
            return this
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            .addHeader("Accept-Language", "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7")
            .addHeader("Host", "rezka.ag")
            .addHeader("Referer", "https://rezka.ag/")
            .addHeader("Cache-Control", "no-cache")
            .addHeader("Connection", "keep-alive")
            .addHeader("Pragma", "no-cache")
            .addHeader("Sec-Ch-Ua", "\"Not(A:Brand\";v=\"8\", \"Chromium\";v=\"144\", \"Google Chrome\";v=\"144\"")
            .addHeader("Sec-Ch-Ua-Mobile", "?0")
            .addHeader("Sec-Ch-Ua-Platform", "\"Windows\"")
            .addHeader("Sec-Fetch-Dest", "document")
            .addHeader("Sec-Fetch-Mode", "navigate")
            .addHeader("Sec-Fetch-Site", "same-origin")
            .addHeader("Sec-Fetch-User", "?1")
    }

    // Выполнение HTTP-запроса
    private suspend fun makeRequest(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .addHeaders()
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

    fun fetchCdnSeries(
        isMovie : Boolean,
        itemId : Int,
        translatorId : Int,
        seasonId : Int,
        episodeId : Int,
        isDirector : Boolean
    ) : String? {

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
            .url("${context.getString(R.string.site_url)}/ajax/get_cdn_series/")   // adjust the endpoint path if needed
            .post(formBody)
            .addHeaders()
            .addHeader("X-Requested-With", "XMLHttpRequest")   // important for AJAX detection
            .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
            .build()

        return try {
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
                        qualities.put(quality, link)
                    }
                    qualities["1080p"]
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}