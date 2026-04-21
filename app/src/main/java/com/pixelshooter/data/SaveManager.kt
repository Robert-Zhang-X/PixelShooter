package com.pixelshooter.data

import android.content.Context
import com.google.gson.Gson
import com.pixelshooter.game.entities.PlaneType

data class SaveData(
    val unlockedLevels: MutableSet<Int> = mutableSetOf(1),
    val bestScores: MutableMap<Int, Int> = mutableMapOf(),
    val savedBombs: Int = 0,
    val lastPlaneType: String = PlaneType.FALCON.name
)

object SaveManager {
    private const val PREF_NAME = "pixel_shooter_save"
    private const val KEY_SAVE = "save_data"
    private val gson = Gson()

    fun load(context: Context): SaveData {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_SAVE, null) ?: return SaveData()
        return try { gson.fromJson(json, SaveData::class.java) } catch (e: Exception) { SaveData() }
    }

    fun save(context: Context, data: SaveData) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SAVE, gson.toJson(data)).apply()
    }

    fun unlockLevel(context: Context, levelId: Int) {
        val data = load(context)
        data.unlockedLevels.add(levelId)
        save(context, data)
    }

    fun updateBestScore(context: Context, levelId: Int, score: Int) {
        val data = load(context)
        val cur = data.bestScores[levelId] ?: 0
        if (score > cur) {
            data.bestScores[levelId] = score
            save(context, data)
        }
    }

    fun isLevelUnlocked(context: Context, levelId: Int): Boolean {
        return load(context).unlockedLevels.contains(levelId)
    }
}
