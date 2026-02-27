package com.customphase.hdrezkacustom
import android.content.Context
import android.widget.Toast
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "data")

class SaveDataManager(val context: Context, val rezkaApi : HDRezkaApi, val scope: CoroutineScope) {

    private val SETTINGS_KEY = stringPreferencesKey("settings")
    private val WATCH_HISTORY_KEY = stringPreferencesKey("watch_history")

    private var saveSettingsJob : Job? = null

    val gson = Gson()
    var settings = Settings()
    var watchHistory = WatchHistory()

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
        watchHistory.onUpdate = { saveWatchHistory() }
    }

    private fun saveWatchHistory() {
        scope.launch {
            context.dataStore.edit { data ->
                data[WATCH_HISTORY_KEY] = gson.toJson(watchHistory).toString()
            }
        }
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

    /*suspend fun initializeWatchHistory() {
        val data = context.dataStore.data.first()
        val jsonString = data[WATCH_HISTORY_KEY] ?: ""

        if (jsonString.isNotEmpty()) {
            watchHistory = gson.fromJson(jsonString, WatchHistory::class.java)
        }
    }

    suspend fun saveWatchHistory() {
        context.dataStore.edit { data ->
            data[WATCH_HISTORY_KEY] = JSONObject(watchHistory).toString()
        }
    }

    suspend fun saveLoginName(v: String) {
        context.dataStore.edit { data ->
            data[LOGIN_NAME_KEY] = v
        }
    }

    suspend fun saveLoginPass(v: String) {
        context.dataStore.edit { data ->
            data[LOGIN_PASS_KEY] = v
        }
    }

    suspend fun delete() {
        context.dataStore.edit { data ->
            data.clear()
        }
        watchHistory = WatchHistory()
    }*/
}