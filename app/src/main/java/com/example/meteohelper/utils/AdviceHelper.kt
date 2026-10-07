package com.example.meteohelper.utils

object AdviceHelper {

    /**
     * Получает советы на основе хронических заболеваний из профиля
     */
    fun getProfileBasedAdvice(diseases: String, pressure: Float, kpIndex: Float): List<String> {
        val advice = mutableListOf<String>()
        val diseasesLower = diseases.lowercase()

        // Гипертония / проблемы с давлением
        if (diseasesLower.contains("гипертон") || diseasesLower.contains("давлен")) {
            advice.add("🫀 При гипертонии измеряйте давление 2 раза в день.")
            if (pressure > 755) {
                advice.add("⚠️ Повышенное атмосферное давление! Контролируйте АД.")
            }
        }

        // Гипотония
        if (diseasesLower.contains("гипотон") || diseasesLower.contains("пониженн")) {
            advice.add("🫀 При гипотонии пейте больше воды, можно крепкий чай.")
            if (pressure < 745) {
                advice.add("⚠️ Пониженное атмосферное давление! Возможна слабость.")
            }
        }

        // Сердечно-сосудистые
        if (diseasesLower.contains("сердц") || diseasesLower.contains("сосуд")) {
            advice.add("❤️ При сердечно-сосудистых заболеваниях избегайте резких нагрузок.")
            if (kpIndex >= 4) {
                advice.add("⚠️ Магнитная активность может влиять на сердце. Будьте осторожны.")
            }
        }

        // Суставы
        if (diseasesLower.contains("сустав") || diseasesLower.contains("артрит") || diseasesLower.contains("остеохондроз")) {
            advice.add("🦴 При проблемах с суставами держите их в тепле.")
            if (pressure < 745 || pressure > 755) {
                advice.add("⚠️ Скачки давления могут усиливать боли.")
            }
        }

        // Мигрени
        if (diseasesLower.contains("мигрен") || diseasesLower.contains("головн")) {
            advice.add("🤕 При склонности к мигреням избегайте яркого света и шума.")
        }

        // Магнитные бури (общий совет)
        if (kpIndex >= 5) {
            advice.add("🧲 Сильная магнитная буря! Исключите алкоголь, больше отдыхайте.")
        } else if (kpIndex >= 4) {
            advice.add("🧲 Возбуждённая магнитосфера. Снизьте физические нагрузки.")
        }

        return advice
    }
}