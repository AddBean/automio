package com.hive.audio.utils

import java.util.Locale

object LanguageHelper {

    private val languageTagList = mutableListOf<String>().apply {
        add("ar-SA");
        add("bn-BD");
        add("bn-IN");
        add("cs-CZ");
        add("da-DK");
        add("de-AT");
        add("de-CH");
        add("de-DE");
        add("el-GR");
        add("en-AU");
        add("en-CA");
        add("en-GB");
        add("en-IE");
        add("en-IN");
        add("en-NZ");
        add("en-US");
        add("en-ZA");
        add("es-AR");
        add("es-CL");
        add("es-CO");
        add("es-ES");
        add("es-MX");
        add("es-US");
        add("fi-FI");
        add("fr-BE");
        add("fr-CA");
        add("fr-CH");
        add("fr-FR");
        add("he-IL");
        add("hi-IN");
        add("hu-HU");
        add("id-ID");
        add("it-CH");
        add("it-IT");
        add("ja-JP");
        add("ko-KR");
        add("nl-BE");
        add("nl-NL");
        add("no-NO");
        add("pl-PL");
        add("pt-BR");
        add("pt-PT");
        add("ro-RO");
        add("ru-RU");
        add("sk-SK");
        add("sv-SE");
        add("ta-IN");
        add("ta-LK");
        add("th-TH");
        add("tr-TR");
        add("zh-CN");
        add("zh-HK");
        add("zh-TW");
    }

    fun getLanguageTagByCodeAndRegion(langCode: String, regionCode: String): String {
        val code = langCode.lowercase(Locale.getDefault())
        val region = regionCode.uppercase(Locale.getDefault())
        languageTagList.forEach {
            if (it.startsWith(code) && it.endsWith(region)) {
                return it
            }
        }
        languageTagList.forEach {
            if (it.startsWith(code)) {
                return it
            }
        }
        return "en-US"
    }
}