package com.readinghabitcoach.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MetadataLookup {
    suspend fun search(query: String): List<Book> = withContext(Dispatchers.IO) {
        require(query.length in 2..200)
        val url = URL("https://openlibrary.org/search.json?q=${URLEncoder.encode(query, "UTF-8")}&limit=10&fields=title,author_name,isbn,number_of_pages_median")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 10_000; connection.readTimeout = 10_000
            connection.setRequestProperty("User-Agent", "ReadingHabitCoach/0.1 (personal book metadata lookup)")
            require(connection.responseCode == 200) { "Book search is unavailable. You can still add books manually." }
            val data = connection.inputStream.use { it.readBytesLimited(1024 * 1024) }.toString(Charsets.UTF_8)
            val docs = JSONObject(data).getJSONArray("docs")
            (0 until docs.length()).map { i -> val d = docs.getJSONObject(i); Book(title = d.getString("title"), author = d.optJSONArray("author_name")?.optString(0) ?: "", isbn = d.optJSONArray("isbn")?.optString(0) ?: "", totalPages = d.optInt("number_of_pages_median").coerceAtLeast(0), colour = d.getString("title").hashCode()) }
        } finally { connection.disconnect() }
    }
}
