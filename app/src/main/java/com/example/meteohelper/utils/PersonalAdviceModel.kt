package com.example.meteohelper.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.meteohelper.data.db.DatabaseHelper

object PersonalAdviceModel {
    private const val PREFS_NAME = "personal_model_weights"

    // Теги погоды для связи с симптомами
    fun getWeatherTags(pressure: Float, kp: Float): List<String> {
        val tags = mutableListOf<String>()
        if (pressure < 745) tags.add("LowPressure")
        if (pressure > 760) tags.add("HighPressure")
        if (kp >= 4) tags.add("MagneticStorm")
        return tags
    }

    /**
     * Обучение модели:
     * 1. Связываем погоду и симптомы
     * 2. Считаем общую частоту симптомов
     */
    fun train(context: Context, weatherTags: List<String>, symptoms: List<String>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()

        // 1. Связь Погода -> Симптом
        for (symptom in symptoms) {
            for (tag in weatherTags) {
                val key = "${tag}_${symptom}"
                val currentWeight = prefs.getInt(key, 0)
                editor.putInt(key, (currentWeight + 20).coerceAtMost(100))
            }
        }

        // 2. Общая частота симптома (независимо от погоды)
        for (symptom in symptoms) {
            val key = "SYMPTOM_COUNT_${symptom}"
            val count = prefs.getInt(key, 0)
            editor.putInt(key, count + 1)
        }

        editor.apply()
    }

    /**
     * Получение советов от Модели (на основе последних 5 записей)
     */
    fun getDiaryBasedAdvice(context: Context, dbHelper: DatabaseHelper): List<String> {
        val advice = mutableListOf<String>()

        // Берём только последние 5 записей
        val allEntries = dbHelper.getAllWellbeingEntries()
        val recentEntries = allEntries.take(5)

        if (recentEntries.isEmpty()) {
            return advice
        }

        // Считаем частоту симптомов в последних 5 записях
        val symptomCounts = mutableMapOf<String, Int>()
        for (entry in recentEntries) {
            val symptomsStr = entry["symptoms"] as? String ?: ""
            val symptoms = symptomsStr.split(",").map { it.trim() }

            for (symptom in symptoms) {
                if (symptom.isNotEmpty() && symptom != "Нормально") {
                    symptomCounts[symptom] = (symptomCounts[symptom] ?: 0) + 1
                }
            }
        }

        // Генерируем советы на основе частоты
        for ((symptom, count) in symptomCounts) {
            // Если симптом встречается 2+ раза в последних 5 записях
            if (count >= 2) {
                when (symptom) {
                    "Головная боль" -> {
                        advice.add("🤕 В последних записях вы часто отмечаете головную боль. Проветривайте помещение и пейте больше воды.")
                    }
                    "Головокружение" -> {
                        advice.add("💫 Вы часто отмечаете головокружение. Вставайте с кровати медленно, избегайте резких движений.")
                    }
                    "Слабость / Сонливость" -> {
                        advice.add("😴 Вы часто чувствуете слабость. Попробуйте высыпаться и не перегружать организм.")
                    }
                    "Боль в суставах" -> {
                        advice.add("🦴 Боли в суставах беспокоят часто. Держите их в тепле и делайте лёгкую разминку.")
                    }
                    "Скачки давления" -> {
                        advice.add("📊 Вы часто отмечаете скачки давления. Контролируйте АД утром и вечером.")
                    }
                    "Перепады настроения" -> {
                        advice.add("🎭 Частые перепады настроения. Попробуйте техники релаксации и прогулки.")
                    }
                    "Дискомфорт в сердце" -> {
                        advice.add("❤️ Вы отмечаете дискомфорт в сердце. Избегайте стресса и проконсультируйтесь с врачом.")
                    }
                }
            }
        }

        return advice
    }
}