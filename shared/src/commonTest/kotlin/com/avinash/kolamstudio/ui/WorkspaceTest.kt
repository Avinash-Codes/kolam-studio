package com.avinash.kolamstudio.ui

import com.avinash.kolamstudio.model.patternFromCode
import com.avinash.kolamstudio.model.toCode
import com.avinash.kolamstudio.store.CollectionStore
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkspaceTest {
    @Test
    fun firstLessonJoinsTracesAndReopensTheExactSavedPattern() {
        val workspace = WorkspaceState(initialPattern = null)
        assertEquals("lesson/join", workspace.urlFragment)
        assertEquals(2, workspace.loops.size)
        workspace.tapMidpoint(TopologyLesson.JOIN.midpoint)
        assertTrue(workspace.lessonComplete)
        assertEquals(1, workspace.loops.size)
        workspace.selectMode(Mode.TRACE)
        for (sample in assertNotNull(workspace.tracePath).samples) workspace.traceTo(x = sample.x, y = sample.y)
        assertEquals(1f, workspace.tracedFraction)

        val settings = MapSettings()
        CollectionStore(settings).save("First loop", workspace.pattern)
        val restored = CollectionStore(settings).patterns.value.single()
        assertEquals(workspace.urlFragment, restored.code)
        assertEquals(workspace.pattern, patternFromCode(restored.code))
    }

    @Test
    fun everyLessonHasADistinctStartingCodeAndCanBeCompletedAndUndone() {
        val lessons = TopologyLesson.entries
        assertEquals(lessons.size, lessons.map { it.code }.distinct().size)
        for (lesson in lessons) {
            val workspace = WorkspaceState(initialPattern = null, initialLesson = lesson)
            assertFalse(workspace.lessonComplete, lesson.title)
            assertEquals(lesson.midpoint, workspace.highlightedMidpoint)
            workspace.tapMidpoint(lesson.midpoint)
            assertTrue(workspace.lessonComplete, lesson.title)
            assertEquals(lesson.goalLoops, workspace.loops.size)
            assertEquals(workspace.pattern.toCode(), workspace.urlFragment)
            workspace.undo()
            assertFalse(workspace.lessonComplete, lesson.title)
            assertEquals(lesson.code, workspace.pattern.toCode())
        }
    }

    @Test
    fun importingMultipleLoopsWhileTracingReturnsToEdit() {
        val workspace = WorkspaceState(initialPattern = TopologyLesson.SPLIT.pattern())
        workspace.selectMode(Mode.TRACE)
        assertEquals(Mode.TRACE, workspace.mode)
        workspace.openPattern(TopologyLesson.JOIN.pattern())
        assertEquals(Mode.EDIT, workspace.mode)
        assertEquals(2, workspace.loops.size)
        assertEquals(0f, workspace.tracedFraction)
        workspace.selectMode(Mode.TRACE)
        assertEquals(Mode.EDIT, workspace.mode)
    }

    @Test
    fun generatedPatternDoesNotReplaceAnImportOrANewerRequest() {
        val workspace = WorkspaceState(initialPattern = null)
        workspace.newPattern()
        val request = assertNotNull(workspace.request)
        workspace.openPattern(TopologyLesson.SPLIT.pattern())
        workspace.newPattern()
        workspace.finishGeneration(request, TopologyLesson.REROUTE.pattern(), request.symmetry)
        assertEquals(TopologyLesson.SPLIT.pattern(), workspace.pattern)
    }
}
