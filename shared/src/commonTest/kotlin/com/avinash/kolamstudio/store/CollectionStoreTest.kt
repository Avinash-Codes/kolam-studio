package com.avinash.kolamstudio.store

import com.avinash.kolamstudio.model.Grid
import com.avinash.kolamstudio.model.Pattern
import com.avinash.kolamstudio.model.patternFromCode
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class CollectionStoreTest {
    private val pattern = Pattern(Grid(rows = 3, cols = 4))

    @Test
    fun savedPatternsSurviveARestart() {
        val settings = MapSettings()
        CollectionStore(settings).save("First", pattern)

        val reopened = CollectionStore(settings).patterns.value
        assertEquals(listOf("First"), reopened.map { it.name })
        assertEquals(pattern, patternFromCode(reopened.single().code))
    }

    @Test
    fun renameAndDeleteChangeOnlyTheChosenPattern() {
        val settings = MapSettings()
        val store = CollectionStore(settings)
        listOf("A", "B", "C").forEach { store.save(it, pattern) }

        store.rename(1, "Renamed")
        store.delete(0)

        assertEquals(listOf("Renamed", "C"), store.patterns.value.map { it.name })
        assertEquals(listOf("Renamed", "C"), CollectionStore(settings).patterns.value.map { it.name })
    }

    @Test
    fun unreadableStoredDataGivesAnEmptyCollection() {
        val settings = MapSettings().apply { putString("collection", "not json") }
        assertEquals(emptyList(), CollectionStore(settings).patterns.value)
    }
}
