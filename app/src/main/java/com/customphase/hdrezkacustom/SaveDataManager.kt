package com.customphase.hdrezkacustom
import android.content.Context
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "data")

class SaveDataManager(val context: Context, val hdrezkaApi : HDRezkaApi, val scope: CoroutineScope) {

    private val SETTINGS_KEY = stringPreferencesKey("settings")
    private val WATCH_HISTORY_KEY = stringPreferencesKey("watch_history")

    private var saveSettingsJob : Job? = null
    private var saveWatchHistoryJob : Job? = null

    val gson = Gson()
    var settings = Settings()
    private var watchHistory = WatchHistory()

    suspend fun loadSettings() {
        val data = context.dataStore.data.first()
        val json = data[SETTINGS_KEY] ?: ""
        if (json.isNotEmpty()) {
            settings = gson.fromJson(json, Settings::class.java)
        }
        settings.onUpdate = { saveSettings() }
    }

    private fun saveSettings() {
        saveSettingsJob?.cancel()
        saveSettingsJob = scope.launch {
            delay(1000)
            val activity = (context as MainActivity)
            context.dataStore.edit { data ->
                data[SETTINGS_KEY] = gson.toJson(settings).toString()
            }
            Toast.makeText(activity, activity.getString(R.string.data_saved), Toast.LENGTH_SHORT).show()
        }
    }

    suspend fun loadWatchHistory() {
        val data = context.dataStore.data.first()
        val json = data[WATCH_HISTORY_KEY] ?: ""
        if (json.isNotEmpty()) {
            watchHistory = gson.fromJson(json, WatchHistory::class.java)
        }
    }

    private fun saveWatchHistory() {
        saveWatchHistoryJob?.cancel()
        saveWatchHistoryJob = scope.launch {
            delay(900)
            context.dataStore.edit { data ->
                data[WATCH_HISTORY_KEY] = gson.toJson(watchHistory).toString()
            }
        }
    }

    fun addOrUpdateWatchHistoryItem(
        itemTitle : String,
        itemId : Int,
        translatorId : Int,
        seasonId : Int,
        episodeId : Int,
        currentTime : Long,
        duration : Long)
    {
        if (!watchHistory.containsKey(itemId)) watchHistory[itemId] = WatchHistoryItem()
        watchHistory[itemId]?.title = itemTitle
        watchHistory[itemId]?.selection?.translatorId = translatorId
        watchHistory[itemId]?.selection?.seasonId = seasonId
        watchHistory[itemId]?.selection?.episodeId = episodeId
        watchHistory[itemId]?.currentTime = currentTime
        val progress = currentTime.toDouble() / duration
        scope.launch (Dispatchers.IO) {
            hdrezkaApi.saveProgress(itemId, translatorId, seasonId, episodeId, progress, duration.toDouble() / 1000)
        }
        saveWatchHistory()
    }

    fun deleteWatchHistoryItem(id : Int) {
        if (!watchHistory.containsKey(id)) return
        watchHistory.remove(id)
        saveWatchHistory()
    }

    fun getWatchHistory() : Map<Int, WatchHistoryItem> {
        return watchHistory.toMap()
    }

    fun delete() {
        val activity = (context as MainActivity)
        scope.launch {
            context.dataStore.edit { data ->
                data.clear()
            }
            settings = Settings()
            watchHistory = WatchHistory()
            Toast.makeText(activity, activity.getString(R.string.data_deleted), Toast.LENGTH_SHORT).show()
        }
    }
}