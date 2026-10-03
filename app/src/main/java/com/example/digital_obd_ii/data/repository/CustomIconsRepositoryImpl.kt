package com.example.digital_obd_ii.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.digital_obd_ii.domain.model.CustomIconItem
import com.example.digital_obd_ii.domain.model.IconFunction
import com.example.digital_obd_ii.domain.model.IconResolutionCategory
import com.example.digital_obd_ii.domain.repository.CustomIconsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

private val Context.iconsDataStore: DataStore<Preferences> by preferencesDataStore(name = "custom_dashboard_icons")

@Singleton
class CustomIconsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : CustomIconsRepository {

    private val ICONS_JSON_KEY = stringPreferencesKey("custom_icons_json_v1")

    companion object {
        fun createDefaultSlots(): List<CustomIconItem> {
            val list = mutableListOf<CustomIconItem>()
            IconResolutionCategory.values().forEach { cat ->
                for (i in 0 until 10) {
                    list.add(
                        CustomIconItem(
                            id = "${cat.name.lowercase()}_$i",
                            category = cat,
                            slotIndex = i,
                            posX = 50f + (i * 20f),
                            posY = 100f + (cat.ordinal * 120f)
                        )
                    )
                }
            }
            return list
        }
    }

    override fun getIconsFlow(): Flow<List<CustomIconItem>> {
        return context.iconsDataStore.data.map { preferences ->
            val jsonString = preferences[ICONS_JSON_KEY]
            if (jsonString.isNullOrEmpty()) {
                createDefaultSlots()
            } else {
                deserializeIcons(jsonString)
            }
        }
    }

    override suspend fun getIconsSync(): List<CustomIconItem> {
        val prefs = context.iconsDataStore.data.first()
        val jsonString = prefs[ICONS_JSON_KEY]
        return if (jsonString.isNullOrEmpty()) {
            createDefaultSlots()
        } else {
            deserializeIcons(jsonString)
        }
    }

    override suspend fun saveIcon(item: CustomIconItem) {
        val current = getIconsSync().toMutableList()
        val idx = current.indexOfFirst { it.id == item.id }
        if (idx >= 0) {
            current[idx] = item
        } else {
            current.add(item)
        }
        saveAll(current)
    }

    override suspend fun saveAll(items: List<CustomIconItem>) {
        val jsonString = serializeIcons(items)
        context.iconsDataStore.edit { preferences ->
            preferences[ICONS_JSON_KEY] = jsonString
        }
    }

    override suspend fun resetCategory(category: IconResolutionCategory) {
        val current = getIconsSync().toMutableList()
        val defaults = createDefaultSlots().filter { it.category == category }
        current.removeAll { it.category == category }
        current.addAll(defaults)
        saveAll(current)
    }

    private fun serializeIcons(items: List<CustomIconItem>): String {
        val jsonArray = JSONArray()
        items.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("category", item.category.name)
                put("slotIndex", item.slotIndex)
                put("imageUri", item.imageUri ?: "")
                put("posX", item.posX.toDouble())
                put("posY", item.posY.toDouble())
                put("isClickable", item.isClickable)
                put("function", item.function.name)
                put("label", item.label)
            }
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }

    private fun deserializeIcons(jsonString: String): List<CustomIconItem> {
        return try {
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<CustomIconItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val catName = obj.optString("category", IconResolutionCategory.SMALL.name)
                val category = runCatching { IconResolutionCategory.valueOf(catName) }.getOrDefault(IconResolutionCategory.SMALL)
                val funcName = obj.optString("function", IconFunction.NONE.name)
                val function = runCatching { IconFunction.valueOf(funcName) }.getOrDefault(IconFunction.NONE)
                val uri = obj.optString("imageUri", "").ifEmpty { null }

                list.add(
                    CustomIconItem(
                        id = obj.getString("id"),
                        category = category,
                        slotIndex = obj.getInt("slotIndex"),
                        imageUri = uri,
                        posX = obj.optDouble("posX", 100.0).toFloat(),
                        posY = obj.optDouble("posY", 100.0).toFloat(),
                        isClickable = obj.optBoolean("isClickable", false),
                        function = function,
                        label = obj.optString("label", "")
                    )
                )
            }

            // Garante que todos os 30 slots existam caso falte algum
            val defaults = createDefaultSlots()
            val finalMap = defaults.associateBy { it.id }.toMutableMap()
            list.forEach { finalMap[it.id] = it }
            finalMap.values.toList()
        } catch (_: Exception) {
            createDefaultSlots()
        }
    }
}
