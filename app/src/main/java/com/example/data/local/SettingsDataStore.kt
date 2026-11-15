package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.NarrationProvider
import com.example.domain.model.StudioSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.studioDataStore: DataStore<Preferences> by preferencesDataStore(name = "studio_settings")

class SettingsDataStore(private val context: Context) {
    private object Keys {
        val CHANNEL_NAME = stringPreferencesKey("channel_name")
        val WATERMARK_TEXT = stringPreferencesKey("watermark_text")
        val WATERMARK_LOGO_PATH = stringPreferencesKey("watermark_logo_path")
        val IMAGE_MODEL_ID = stringPreferencesKey("image_model_id")
        val NARRATION_PROVIDER = stringPreferencesKey("narration_provider")
        val ANDROID_TTS_VOICE = stringPreferencesKey("android_tts_voice")
        val ELEVEN_API_KEY = stringPreferencesKey("eleven_api_key")
        val ELEVEN_VOICE_ID = stringPreferencesKey("eleven_voice_id")
        val ELEVEN_VOICE_NAME = stringPreferencesKey("eleven_voice_name")
        val ELEVEN_MODEL = stringPreferencesKey("eleven_model")
        val LICENSE_NOTE = stringPreferencesKey("license_note")
    }

    val settingsFlow: Flow<StudioSettings> = context.studioDataStore.data.map { prefs ->
        val providerName = prefs[Keys.NARRATION_PROVIDER] ?: NarrationProvider.ANDROID_TTS.name
        val provider = runCatching { NarrationProvider.valueOf(providerName) }
            .getOrDefault(NarrationProvider.ANDROID_TTS)
        StudioSettings(
            channelName = prefs[Keys.CHANNEL_NAME].orEmpty(),
            watermarkText = prefs[Keys.WATERMARK_TEXT].orEmpty(),
            watermarkLogoPath = prefs[Keys.WATERMARK_LOGO_PATH],
            imageModelId = prefs[Keys.IMAGE_MODEL_ID] ?: "gemini-nano-banana-2.1",
            narrationProvider = provider,
            androidTtsVoiceName = prefs[Keys.ANDROID_TTS_VOICE].orEmpty(),
            elevenLabsApiKey = prefs[Keys.ELEVEN_API_KEY].orEmpty(),
            elevenLabsVoiceId = prefs[Keys.ELEVEN_VOICE_ID].orEmpty(),
            elevenLabsVoiceName = prefs[Keys.ELEVEN_VOICE_NAME].orEmpty(),
            elevenLabsModel = prefs[Keys.ELEVEN_MODEL] ?: "eleven_multilingual_v2",
            licenseNote = prefs[Keys.LICENSE_NOTE].orEmpty()
        )
    }

    suspend fun updateSettings(transform: (StudioSettings) -> StudioSettings) {
        context.studioDataStore.edit { prefs ->
            val currentProviderName = prefs[Keys.NARRATION_PROVIDER] ?: NarrationProvider.ANDROID_TTS.name
            val current = StudioSettings(
                channelName = prefs[Keys.CHANNEL_NAME].orEmpty(),
                watermarkText = prefs[Keys.WATERMARK_TEXT].orEmpty(),
                watermarkLogoPath = prefs[Keys.WATERMARK_LOGO_PATH],
                imageModelId = prefs[Keys.IMAGE_MODEL_ID] ?: "gemini-nano-banana-2.1",
                narrationProvider = runCatching { NarrationProvider.valueOf(currentProviderName) }
                    .getOrDefault(NarrationProvider.ANDROID_TTS),
                androidTtsVoiceName = prefs[Keys.ANDROID_TTS_VOICE].orEmpty(),
                elevenLabsApiKey = prefs[Keys.ELEVEN_API_KEY].orEmpty(),
                elevenLabsVoiceId = prefs[Keys.ELEVEN_VOICE_ID].orEmpty(),
                elevenLabsVoiceName = prefs[Keys.ELEVEN_VOICE_NAME].orEmpty(),
                elevenLabsModel = prefs[Keys.ELEVEN_MODEL] ?: "eleven_multilingual_v2",
                licenseNote = prefs[Keys.LICENSE_NOTE].orEmpty()
            )
            val updated = transform(current)
            prefs[Keys.CHANNEL_NAME] = updated.channelName
            prefs[Keys.WATERMARK_TEXT] = updated.watermarkText
            if (updated.watermarkLogoPath != null) {
                prefs[Keys.WATERMARK_LOGO_PATH] = updated.watermarkLogoPath
            } else {
                prefs.remove(Keys.WATERMARK_LOGO_PATH)
            }
            prefs[Keys.IMAGE_MODEL_ID] = updated.imageModelId
            prefs[Keys.NARRATION_PROVIDER] = updated.narrationProvider.name
            prefs[Keys.ANDROID_TTS_VOICE] = updated.androidTtsVoiceName
            prefs[Keys.ELEVEN_API_KEY] = updated.elevenLabsApiKey
            prefs[Keys.ELEVEN_VOICE_ID] = updated.elevenLabsVoiceId
            prefs[Keys.ELEVEN_VOICE_NAME] = updated.elevenLabsVoiceName
            prefs[Keys.ELEVEN_MODEL] = updated.elevenLabsModel
            prefs[Keys.LICENSE_NOTE] = updated.licenseNote
        }
    }
}
