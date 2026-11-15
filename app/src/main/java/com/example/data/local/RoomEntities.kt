package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ChapterItem
import com.example.domain.model.PublicationMetadata
import com.example.domain.model.SceneItem
import com.example.domain.model.SeriesEpisode
import com.example.domain.model.SeriesProject
import com.example.domain.model.SoundEffectItem
import com.example.domain.model.SourceItem
import com.example.domain.model.VideoProject
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val title: String,
    val pageTypeId: String,
    val targetDurationMinutes: Int,
    val fullScript: String,
    val logline: String,
    val chaptersJson: String,
    val aiNotesJson: String,
    val scenesJson: String,
    val sourcesJson: String,
    val soundEffectsJson: String,
    val publicationJson: String,
    val illustrationBadge: Boolean,
    val audioPath: String?,
    val audioSourceLabel: String?,
    val renderedVideoPath: String?,
    val renderedAtMillis: Long?,
    val seriesId: String?,
    val episodeNumber: Int?,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

@Entity(tableName = "series")
data class SeriesEntity(
    @PrimaryKey val id: String,
    val name: String,
    val pageTypeId: String,
    val episodeDurationMinutes: Int,
    val bible: String,
    val episodesJson: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

object StudioJsonMapper {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val chaptersAdapter = moshi.adapter<List<ChapterItem>>(
        Types.newParameterizedType(List::class.java, ChapterItem::class.java)
    )
    private val stringsAdapter = moshi.adapter<List<String>>(
        Types.newParameterizedType(List::class.java, String::class.java)
    )
    private val scenesAdapter = moshi.adapter<List<SceneItem>>(
        Types.newParameterizedType(List::class.java, SceneItem::class.java)
    )
    private val sourcesAdapter = moshi.adapter<List<SourceItem>>(
        Types.newParameterizedType(List::class.java, SourceItem::class.java)
    )
    private val sfxAdapter = moshi.adapter<List<SoundEffectItem>>(
        Types.newParameterizedType(List::class.java, SoundEffectItem::class.java)
    )
    private val publicationAdapter = moshi.adapter(PublicationMetadata::class.java)
    private val episodesAdapter = moshi.adapter<List<SeriesEpisode>>(
        Types.newParameterizedType(List::class.java, SeriesEpisode::class.java)
    )

    fun toEntity(project: VideoProject): ProjectEntity = ProjectEntity(
        id = project.id,
        title = project.title,
        pageTypeId = project.pageTypeId,
        targetDurationMinutes = project.targetDurationMinutes,
        fullScript = project.fullScript,
        logline = project.logline,
        chaptersJson = chaptersAdapter.toJson(project.chapters),
        aiNotesJson = stringsAdapter.toJson(project.aiNotes),
        scenesJson = scenesAdapter.toJson(project.scenes),
        sourcesJson = sourcesAdapter.toJson(project.sources),
        soundEffectsJson = sfxAdapter.toJson(project.soundEffects),
        publicationJson = publicationAdapter.toJson(project.publication),
        illustrationBadge = project.illustrationBadge,
        audioPath = project.audioPath,
        audioSourceLabel = project.audioSourceLabel,
        renderedVideoPath = project.renderedVideoPath,
        renderedAtMillis = project.renderedAtMillis,
        seriesId = project.seriesId,
        episodeNumber = project.episodeNumber,
        createdAtMillis = project.createdAtMillis,
        updatedAtMillis = project.updatedAtMillis
    )

    fun toDomain(entity: ProjectEntity): VideoProject = VideoProject(
        id = entity.id,
        title = entity.title,
        pageTypeId = entity.pageTypeId,
        targetDurationMinutes = entity.targetDurationMinutes,
        fullScript = entity.fullScript,
        logline = entity.logline,
        chapters = runCatching { chaptersAdapter.fromJson(entity.chaptersJson) }.getOrNull().orEmpty(),
        aiNotes = runCatching { stringsAdapter.fromJson(entity.aiNotesJson) }.getOrNull().orEmpty(),
        scenes = runCatching { scenesAdapter.fromJson(entity.scenesJson) }.getOrNull().orEmpty(),
        sources = runCatching { sourcesAdapter.fromJson(entity.sourcesJson) }.getOrNull().orEmpty(),
        soundEffects = runCatching { sfxAdapter.fromJson(entity.soundEffectsJson) }.getOrNull().orEmpty(),
        publication = runCatching { publicationAdapter.fromJson(entity.publicationJson) }.getOrNull() ?: PublicationMetadata(),
        illustrationBadge = entity.illustrationBadge,
        audioPath = entity.audioPath,
        audioSourceLabel = entity.audioSourceLabel,
        renderedVideoPath = entity.renderedVideoPath,
        renderedAtMillis = entity.renderedAtMillis,
        seriesId = entity.seriesId,
        episodeNumber = entity.episodeNumber,
        createdAtMillis = entity.createdAtMillis,
        updatedAtMillis = entity.updatedAtMillis
    )

    fun toEntity(series: SeriesProject): SeriesEntity = SeriesEntity(
        id = series.id,
        name = series.name,
        pageTypeId = series.pageTypeId,
        episodeDurationMinutes = series.episodeDurationMinutes,
        bible = series.bible,
        episodesJson = episodesAdapter.toJson(series.episodes),
        createdAtMillis = series.createdAtMillis,
        updatedAtMillis = series.updatedAtMillis
    )

    fun toDomain(entity: SeriesEntity): SeriesProject = SeriesProject(
        id = entity.id,
        name = entity.name,
        pageTypeId = entity.pageTypeId,
        episodeDurationMinutes = entity.episodeDurationMinutes,
        bible = entity.bible,
        episodes = runCatching { episodesAdapter.fromJson(entity.episodesJson) }.getOrNull().orEmpty(),
        createdAtMillis = entity.createdAtMillis,
        updatedAtMillis = entity.updatedAtMillis
    )
}
