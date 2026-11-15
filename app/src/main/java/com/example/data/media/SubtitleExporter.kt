package com.example.data.media

import com.example.domain.model.VideoProject
import java.util.Locale

object SubtitleExporter {

    fun generateSrt(project: VideoProject): String {
        var currentSeconds = 0
        val sb = StringBuilder()
        project.scenes.forEachIndexed { index, scene ->
            val duration = scene.durationSeconds.coerceAtLeast(1)
            val start = formatSrtTime(currentSeconds)
            val end = formatSrtTime(currentSeconds + duration)
            val text = scene.narration.replace(Regex("\\s+"), " ").trim()
                .ifBlank { scene.title }
            sb.append(index + 1).append('\n')
            sb.append(start).append(" --> ").append(end).append('\n')
            sb.append(text).append("\n\n")
            currentSeconds += duration
        }
        return sb.toString()
    }

    private fun formatSrtTime(totalSeconds: Int): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d,000", h, m, s)
    }
}
