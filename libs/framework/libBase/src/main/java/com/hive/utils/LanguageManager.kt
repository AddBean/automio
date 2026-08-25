// Copyright (c) 2026 jiadou
// SPDX-License-Identifier: MIT

package com.hive.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.hive.i8n.R as i8nR
import java.util.Locale

object LanguageManager {
    private const val LANGUAGE_KEY = "language_key"
    const val LANGUAGE_ZH_CN = "zhCN"
    const val LANGUAGE_ZH_TW = "zhTW"
    const val LANGUAGE_EN = "en"

    fun setLanguage(context: Context) {
        val sharedPreferences = context.getSharedPreferences("LanguagePrefs", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString(LANGUAGE_KEY, null)
        setLanguage(context, savedLanguage ?: getDeviceLanguageCode(context))
    }

    fun setLanguage(context: Context, language: String) {
        val normalizedLanguage = normalizeLanguageCode(language, context)
        val locale = toLocale(normalizedLanguage)
        Locale.setDefault(locale)
        val resources = context.resources
        val configuration = resources.configuration

        configuration.setLocale(locale)

        // Android 7.0+ 需要特殊处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = android.os.LocaleList(locale)
            configuration.setLocales(localeList)
        }

        resources.updateConfiguration(configuration, resources.displayMetrics)
        val sharedPreferences = context.getSharedPreferences("LanguagePrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(LANGUAGE_KEY, normalizedLanguage).apply()
//        BaseStatisticsParamsUtils.getInstance().clear()
    }

    @JvmStatic
    fun loadLanguage(context: Context) {
        val sharedPreferences = context.getSharedPreferences("LanguagePrefs", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString(LANGUAGE_KEY, null)
        if (savedLanguage?.isNotBlank() == true) {
            savedLanguage.let { setLanguage(context, it) }
        } else {
            setLanguage(context, getDeviceLanguageCode(context))
        }
    }

    @JvmStatic
    fun getLanguage(context: Context): String {
        val sharedPreferences = context.getSharedPreferences("LanguagePrefs", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString(LANGUAGE_KEY, null)
        return normalizeLanguageCode(savedLanguage, context)
    }

    @JvmStatic
    fun getLanguageDisplayName(context: Context, language: String = getLanguage(context)): String {
        return when (normalizeLanguageCode(language, context)) {
            LANGUAGE_ZH_CN -> context.getString(i8nR.string.language_name_zh_cn)
            LANGUAGE_ZH_TW -> context.getString(i8nR.string.language_name_zh_tw)
            LANGUAGE_EN -> context.getString(i8nR.string.language_name_en)
            else -> {
                val locale = toLocale(language)
                locale.getDisplayLanguage(locale).replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(locale) else it.toString()
                }
            }
        }
    }

    /**
     * 在 attachBaseContext 中使用的语言设置方法
     * 适用于 Android 7.0+ 版本
     */
    @JvmStatic
    fun attachBaseContext(context: Context): Context {
        val sharedPreferences = context.getSharedPreferences("LanguagePrefs", Context.MODE_PRIVATE)
        val savedLanguage = sharedPreferences.getString(LANGUAGE_KEY, null)
        val locale = toLocale(normalizeLanguageCode(savedLanguage, context))

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        // Android 7.0+ 需要特殊处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val localeList = android.os.LocaleList(locale)
            configuration.setLocales(localeList)
        }

        return context.createConfigurationContext(configuration)
    }

    private fun getDeviceLanguageCode(context: Context): String {
        val configuration = context.resources.configuration
        val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
        return normalizeLanguageCode(locale.toLanguageTag(), context)
    }

    private fun normalizeLanguageCode(language: String?, context: Context): String {
        val raw = language?.trim().orEmpty()
        if (raw.isBlank()) {
            return getLocaleLanguageCode(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.resources.configuration.locales[0]
                } else {
                    @Suppress("DEPRECATION")
                    context.resources.configuration.locale
                }
            )
        }

        val compact = raw.replace("-", "").replace("_", "")
        return when {
            compact.equals(LANGUAGE_EN, ignoreCase = true) -> LANGUAGE_EN
            compact.equals("zh", ignoreCase = true) -> {
                val country = Locale.getDefault().country.uppercase(Locale.ROOT)
                if (country in setOf("TW", "HK", "MO")) LANGUAGE_ZH_TW else LANGUAGE_ZH_CN
            }
            compact.startsWith("zh", ignoreCase = true) -> {
                val region = compact.removePrefix("zh").uppercase(Locale.ROOT)
                if (region in setOf("TW", "HK", "MO")) LANGUAGE_ZH_TW else LANGUAGE_ZH_CN
            }
            else -> raw.lowercase(Locale.ROOT)
        }
    }

    private fun getLocaleLanguageCode(locale: Locale): String {
        return when (locale.language.lowercase(Locale.ROOT)) {
            "zh" -> {
                val country = locale.country.uppercase(Locale.ROOT)
                if (country in setOf("TW", "HK", "MO")) LANGUAGE_ZH_TW else LANGUAGE_ZH_CN
            }
            "en" -> LANGUAGE_EN
            else -> locale.language.lowercase(Locale.ROOT)
        }
    }

    private fun toLocale(language: String): Locale {
        return when (language) {
            LANGUAGE_ZH_CN -> Locale.SIMPLIFIED_CHINESE
            LANGUAGE_ZH_TW -> Locale.TRADITIONAL_CHINESE
            LANGUAGE_EN -> Locale.ENGLISH
            else -> {
                if (language.length == 4) {
                    Locale(language.substring(0, 2), language.substring(2, 4))
                } else {
                    Locale(language)
                }
            }
        }
    }
}


