package com.example

import com.example.data.media.SubtitleExporter
import com.example.domain.model.PageModelsCatalog
import com.example.domain.model.SceneItem
import com.example.domain.model.VideoProject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun pageModelsCatalog_containsAll27Models() {
        assertEquals(27, PageModelsCatalog.all.size)
        assertFalse(PageModelsCatalog.findById("vidas-ficcao").isFactual)
        assertTrue(PageModelsCatalog.findById("vidas-doc").isFactual)
        assertTrue(PageModelsCatalog.findById("true-crime").isFactual)
    }

    @Test
    fun subtitleExporter_generatesSequentialSrtTimestamps() {
        val project = VideoProject(
            id = "p1",
            title = "Teste",
            pageTypeId = "vidas-doc",
            targetDurationMinutes = 1,
            fullScript = "Linha 1\n\nLinha 2",
            scenes = listOf(
                SceneItem("scene-001", 1, "Cena 1", "Primeira fala.", "Prompt 1", durationSeconds = 6),
                SceneItem("scene-002", 2, "Cena 2", "Segunda fala.", "Prompt 2", durationSeconds = 9)
            )
        )
        val srt = SubtitleExporter.generateSrt(project)
        assertTrue(srt.contains("00:00:00,000 --> 00:00:06,000"))
        assertTrue(srt.contains("00:00:06,000 --> 00:00:15,000"))
        assertTrue(srt.contains("Primeira fala."))
        assertTrue(srt.contains("Segunda fala."))
    }
}
