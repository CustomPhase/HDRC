package com.customphase.hdrezkacustom

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.IOException
import java.security.SecureRandom
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import java.security.cert.X509Certificate
import javax.net.ssl.*

class HDRezkaParser {

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
            // Trust manager, который доверяет всем сертификатам
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

    private val client = unsafeOkHttpClient.newBuilder().cookieJar(cookieJar).build()

    suspend fun warmup() {
        val url = "https://rezka.ag/"
        makeRequest(url)
    }

    suspend fun search(query: String): List<SearchResult> {
        val results = mutableListOf<SearchResult>()

        // Кодируем запрос для URL
        val encodedQuery = java.net.URLEncoder.encode(query, "utf-8")
        // Используйте актуальное зеркало rezka
        val url = "https://rezka.ag/search/?do=search&subaction=search&q=$encodedQuery"
        println(url)
        val html = makeRequest(url)
        if (html == null) {
            println("REQUEST FAILED")
            return results
        }

        val doc: Document = Jsoup.parse(html)
        // Селекторы могут меняться, проверьте на сайте!
        val items = doc.select("div.b-content__inline_item")

        for (item in items) {
            val linkDiv = item.selectFirst("div.b-content__inline_item-link")
            val coverDiv = item.selectFirst("div.b-content__inline_item-cover")

            var title = "!!! PARSER BROKE !!!"
            var itemUrl = ""
            var poster = ""
            var info = ""

            if (linkDiv != null) {
                val linkElement = linkDiv.selectFirst("a")
                if (linkElement != null) title = linkElement.text()
                val infoElement = linkDiv.select("div")[1]
                if (infoElement != null) info = infoElement.text()
            }

            if (coverDiv != null) {
                val coverElement = coverDiv.selectFirst("img")
                if (coverElement != null) poster = coverElement.attr("src")
            }
            results.add(SearchResult(title, itemUrl, poster, info))
        }

        return results
    }

    // Выполнение HTTP-запроса
    private suspend fun makeRequest(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
            //.addHeader("Accept-Encoding", "deflate, br, zstd")
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
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) response.body?.string() else null
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}