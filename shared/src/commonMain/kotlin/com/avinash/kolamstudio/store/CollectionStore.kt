package com.avinash.kolamstudio.store

import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.toCode
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

private const val KEY = "collection"

@Serializable
data class SavedPattern(
    val name: String,
    val code: String,
)

class CollectionStore(
    private val settings: Settings,
) {
    private val saved = MutableStateFlow(load())
    val patterns: StateFlow<List<SavedPattern>> = saved.asStateFlow()

    fun save(
        name: String,
        pattern: Pattern,
    ) = update { it + SavedPattern(name, pattern.toCode()) }

    fun rename(
        index: Int,
        name: String,
    ) = update { it.mapIndexed { i, item -> if (i == index) item.copy(name = name) else item } }

    fun delete(index: Int) = update { it.filterIndexed { i, _ -> i != index } }

    private fun update(change: (List<SavedPattern>) -> List<SavedPattern>) {
        saved.value = change(saved.value)
        settings.putString(KEY, Json.encodeToString(saved.value))
    }

    private fun load(): List<SavedPattern> {
        val json = settings.getStringOrNull(KEY) ?: return emptyList()
        return try {
            Json.decodeFromString(json)
        } catch (_: SerializationException) {
            emptyList()
        }
    }
}
