// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.config

import android.content.Context
import com.hive.utils.GlobalApp
import com.hive.utils.security.SecureCredentialStore

/**
 * Resolves Azure / 讯飞 speech credentials.
 * Priority: Android Keystore-backed store → BuildConfig map (local.properties inject, never commit real keys).
 */
object SpeechCredentialHelper {
    const val ID_MS_SPEECH_KEY = "ms_speech_key"
    const val ID_MS_SPEECH_REGION = "ms_speech_region"
    const val ID_XF_APP_ID = "xf_app_id"
    const val ID_ASR_PROVIDER = "audio_asr_provider_id"

    private fun store(context: Context = GlobalApp.getContext()): SecureCredentialStore {
        return SecureCredentialStore(context)
    }

    fun getMsSpeechKey(context: Context = GlobalApp.getContext()): String? {
        return normalize(store(context).get(ID_MS_SPEECH_KEY))
            ?: normalize(BuildConfigHelper.getMapString("msSpeechKey", ""))
    }

    fun getMsSpeechRegion(context: Context = GlobalApp.getContext()): String? {
        return normalize(store(context).get(ID_MS_SPEECH_REGION))
            ?: normalize(BuildConfigHelper.getMapString("msSpeechRegion", ""))
    }

    fun getXfAppId(context: Context = GlobalApp.getContext()): String? {
        return normalize(store(context).get(ID_XF_APP_ID))
            ?: normalize(BuildConfigHelper.getMapString("xfAppId", ""))
    }

    fun getAsrProviderId(context: Context = GlobalApp.getContext(), defaultId: String = "ms"): String {
        return normalize(store(context).get(ID_ASR_PROVIDER))
            ?: normalize(BuildConfigHelper.getMapString("audioAsrProviderId", defaultId))
            ?: defaultId
    }

    fun saveMsSpeech(key: String, region: String, context: Context = GlobalApp.getContext()) {
        val s = store(context)
        s.put(ID_MS_SPEECH_KEY, key.trim())
        s.put(ID_MS_SPEECH_REGION, region.trim())
    }

    fun saveXfAppId(appId: String, context: Context = GlobalApp.getContext()) {
        store(context).put(ID_XF_APP_ID, appId.trim())
    }

    fun saveAsrProviderId(providerId: String, context: Context = GlobalApp.getContext()) {
        store(context).put(ID_ASR_PROVIDER, providerId.trim())
    }

    fun clear(context: Context = GlobalApp.getContext()) {
        val s = store(context)
        s.remove(ID_MS_SPEECH_KEY)
        s.remove(ID_MS_SPEECH_REGION)
        s.remove(ID_XF_APP_ID)
        s.remove(ID_ASR_PROVIDER)
    }

    fun normalize(value: String?): String? {
        val s = value?.trim() ?: return null
        if (s.isEmpty() || s == "-" || s.equals("null", ignoreCase = true)) return null
        return s
    }
}
