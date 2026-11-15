package com.example.data.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.local.FileStorageManager
import com.example.domain.model.AiAnalysisResult
import com.example.domain.model.AiResult
import com.example.domain.model.ChapterItem
import com.example.domain.model.PageModelsCatalog
import com.example.domain.model.PublicationMetadata
import com.example.domain.model.SceneItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

interface AiStudioRepository {
    val isFirebaseConnected: Boolean

    suspend fun analyzeScriptToScenes(
        projectTitle: String,
        pageTypeId: String,
        targetDurationMinutes: Int,
        fullScript: String
    ): AiResult<AiAnalysisResult>

    suspend fun generateSceneImage(
        projectId: String,
        scene: SceneItem,
        pageTypeId: String,
        continuityImagePath: String?
    ): AiResult<String>

    suspend fun generatePublicationCopy(
        projectTitle: String,
        pageTypeId: String,
        logline: String,
        fullScript: String
    ): AiResult<PublicationMetadata>
}

/**
 * Placeholder implementation used when Firebase AI Logic is not enabled.
 * Clearly labels every generated output as a demonstration sample and explains
 * that real Gemini generation requires enabling Firebase in AI Studio.
 */
class PlaceholderAiStudioRepository(
    private val fileStorageManager: FileStorageManager
) : AiStudioRepository {

    override val isFirebaseConnected: Boolean = false

    private val demoNotice =
        "Modo Demonstração: conecte o Firebase AI Logic para análises e imagens Gemini reais."

    override suspend fun analyzeScriptToScenes(
        projectTitle: String,
        pageTypeId: String,
        targetDurationMinutes: Int,
        fullScript: String
    ): AiResult<AiAnalysisResult> = withContext(Dispatchers.Default) {
        val cleaned = fullScript.trim()
        if (cleaned.isEmpty()) {
            return@withContext AiResult.Error("Insira e salve o roteiro completo antes de analisar.")
        }

        val model = PageModelsCatalog.findById(pageTypeId)
        val targetSeconds = (targetDurationMinutes.coerceIn(1, 30)) * 60

        // Split script into paragraphs or sentences so 100% of user text is preserved
        val rawBlocks = cleaned
            .split(Regex("\\n\\s*\\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .ifEmpty {
                cleaned.split(Regex("(?<=[.!?])\\s+"))
                    .chunked(2)
                    .map { it.joinToString(" ").trim() }
                    .filter { it.isNotEmpty() }
            }

        val blocks = if (rawBlocks.size == 1 && rawBlocks.first().length > 260) {
            rawBlocks.first()
                .split(Regex("(?<=[.!?])\\s+"))
                .chunked(2)
                .map { it.joinToString(" ").trim() }
                .filter { it.isNotEmpty() }
        } else {
            rawBlocks
        }

        val totalWords = blocks.sumOf { it.split(Regex("\\s+")).size }.coerceAtLeast(1)
        val scenes = blocks.mapIndexed { index, paragraph ->
            val words = paragraph.split(Regex("\\s+")).size.coerceAtLeast(3)
            val proportionalDuration = ((words.toDouble() / totalWords) * targetSeconds)
                .roundToInt()
                .coerceIn(5, 22)
            val sceneNum = index + 1
            val id = "scene-${sceneNum.toString().padStart(3, '0')}"
            val snippet = paragraph.take(44).replace("\n", " ").trim()
            val title = if (index == 0) "Abertura · $snippet" else "Cena $sceneNum · $snippet"
            SceneItem(
                id = id,
                number = sceneNum,
                title = title.take(80),
                narration = paragraph,
                visualPrompt = "[Amostra Demo] ${model.visualGuidance} Cena $sceneNum em 16:9 retratando: ${paragraph.take(140)}",
                durationSeconds = proportionalDuration
            )
        }

        val chapters = mutableListOf<ChapterItem>()
        chapters.add(ChapterItem(title = "Abertura", startScene = 1))
        if (scenes.size >= 4) {
            chapters.add(ChapterItem(title = "Desenvolvimento", startScene = (scenes.size / 3).coerceAtLeast(2)))
        }
        if (scenes.size >= 7) {
            chapters.add(ChapterItem(title = "Desfecho", startScene = ((scenes.size * 2) / 3).coerceAtLeast(4)))
        }

        val notes = buildList {
            add("Divisão automática em modo demonstração (${scenes.size} cenas preservando 100% do texto).")
            add("Conecte o Firebase AI Logic para segmentação semântica e prompts visuais avançados via Gemini.")
            if (model.isFactual) {
                add("Formato factual (${model.name}): confira datas, nomes e registre as fontes na seção Apuração.")
            }
        }

        AiResult.Success(
            data = AiAnalysisResult(
                logline = "[Amostra Demo] $projectTitle — roteiro estruturado em ${scenes.size} cenas (${model.name}).",
                chapters = chapters,
                notes = notes,
                scenes = scenes,
                isPlaceholder = true
            ),
            isDemoPlaceholder = true,
            notice = demoNotice
        )
    }

    override suspend fun generateSceneImage(
        projectId: String,
        scene: SceneItem,
        pageTypeId: String,
        continuityImagePath: String?
    ): AiResult<String> = withContext(Dispatchers.Default) {
        val model = PageModelsCatalog.findById(pageTypeId)
        val width = 1280
        val height = 720
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = (0xFF000000L or (model.bgHex and 0x00FFFFFFL)).toInt()
        val accentColor = (0xFF000000L or (model.accentHex and 0x00FFFFFFL)).toInt()
        val darkOverlay = Color.argb(215, 16, 19, 27)
        val midOverlay = Color.argb(175, 25, 30, 41)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(darkOverlay, bgColor, midOverlay),
                floatArrayOf(0f, 0.55f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Subtle cinematic frame border
        val framePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = accentColor
            alpha = 150
        }
        canvas.drawRoundRect(RectF(28f, 28f, width - 28f, height - 28f), 22f, 22f, framePaint)

        // Decorative glow circle
        val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            alpha = 45
        }
        canvas.drawCircle(width * 0.82f, height * 0.28f, 180f, glowPaint)
        canvas.drawCircle(width * 0.16f, height * 0.78f, 120f, glowPaint)

        // Demo notice badge at top left
        val badgeRect = RectF(56f, 52f, 670f, 102f)
        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(210, 16, 19, 27)
        }
        canvas.drawRoundRect(badgeRect, 14f, 14f, badgeBgPaint)
        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = accentColor
        }
        canvas.drawRoundRect(badgeRect, 14f, 14f, badgeBorderPaint)

        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = accentColor
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(
            "AMOSTRA DEMO · REQUER FIREBASE AI LOGIC",
            76f,
            85f,
            badgeTextPaint
        )

        // Scene number + model header
        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = 220
            textSize = 26f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        }
        canvas.drawText(
            "${model.iconEmoji} ${model.name.uppercase()} · CENA ${scene.number.toString().padStart(2, '0')} (${scene.durationSeconds}s)",
            56f,
            165f,
            metaPaint
        )

        // Scene title
        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 44f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val titleLayout = StaticLayout.Builder
            .obtain(scene.title, 0, scene.title.length, titlePaint, width - 120)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(2)
            .build()
        canvas.save()
        canvas.translate(56f, 200f)
        titleLayout.draw(canvas)
        canvas.restore()

        // Visual prompt preview
        val promptCardRect = RectF(56f, 340f, width - 56f, height - 96f)
        val promptCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(185, 16, 19, 27)
        }
        canvas.drawRoundRect(promptCardRect, 18f, 18f, promptCardPaint)

        val promptPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(224, 229, 238)
            textSize = 28f
        }
        val promptPreview = scene.visualPrompt.ifBlank { scene.narration }.take(260)
        val promptLayout = StaticLayout.Builder
            .obtain(promptPreview, 0, promptPreview.length, promptPaint, width - 168)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setMaxLines(5)
            .build()
        canvas.save()
        canvas.translate(84f, 368f)
        promptLayout.draw(canvas)
        canvas.restore()

        val savedFile = fileStorageManager.saveSceneBitmap(projectId, scene.id, bitmap)
        bitmap.recycle()

        AiResult.Success(
            data = savedFile.absolutePath,
            isDemoPlaceholder = true,
            notice = demoNotice
        )
    }

    override suspend fun generatePublicationCopy(
        projectTitle: String,
        pageTypeId: String,
        logline: String,
        fullScript: String
    ): AiResult<PublicationMetadata> = withContext(Dispatchers.Default) {
        val model = PageModelsCatalog.findById(pageTypeId)
        val cleanTitle = projectTitle.ifBlank { "Novo Vídeo" }
        val keyword = cleanTitle.split(" ").firstOrNull()?.lowercase()?.replace(Regex("[^a-z0-9]"), "")
            ?.ifBlank { "historia" } ?: "historia"

        val metadata = PublicationMetadata(
            titles = listOf(
                "[Amostra] $cleanTitle: O Que Ninguém Contou",
                "[Amostra] A História Completa de $cleanTitle",
                "[Amostra] $cleanTitle Explicado em Detalhes"
            ),
            description = buildString {
                appendLine("[Modo Demonstração — Conecte o Firebase AI Logic para gerar descrições com Gemini]")
                appendLine()
                appendLine(logline.ifBlank { fullScript.take(180) })
                appendLine()
                appendLine("Formato: ${model.name}")
                appendLine("Inscreva-se no canal e ative as notificações para acompanhar os próximos episódios.")
                appendLine()
                append("#$keyword #AutoTubeStudio #Documentario")
            },
            tags = listOf(
                cleanTitle,
                model.name,
                "$keyword em portugues",
                "documentario",
                "narrativa",
                "curiosidades"
            ),
            hashtags = listOf("#$keyword", "#AutoTubeStudio", "#VideoNarrado"),
            socialCaption = "[Amostra Demo] Novo vídeo no ar: $cleanTitle. O que você achou dessa história?"
        )

        AiResult.Success(
            data = metadata,
            isDemoPlaceholder = true,
            notice = demoNotice
        )
    }
}
