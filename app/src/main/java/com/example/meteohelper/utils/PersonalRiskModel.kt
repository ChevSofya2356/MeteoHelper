package com.example.meteohelper.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.meteohelper.data.db.DatabaseHelper
import kotlin.math.abs

/**
 * Простая статистическая модель персонализации прогнозов.
 * Анализирует историю записей и находит, какие погодные факторы
 * чаще всего приводят к плохому самочувствию у конкретного пользователя.
 */
object PersonalRiskModel {

    private const val PREFS_NAME = "personal_risk_model"
    private const val MIN_RECORDS_FOR_PREDICTION = 5  // Минимум записей для работы модели
    private const val CORRELATION_THRESHOLD = 0.5f     // Порог корреляции для предупреждения

    /**
     * Данные одной записи для анализа
     */
    data class TrainingSample(
        val deltaPressure: Float,   // Изменение давления
        val deltaTemp: Float,       // Изменение температуры
        val kpIndex: Float,         // Kp-индекс
        val deltaHumidity: Float,   // Изменение влажности
        val wellbeingScore: Int     // Оценка самочувствия 1-10
    )

    /**
     * Результат анализа модели
     */
    data class RiskPrediction(
        val isPersonalRiskHigh: Boolean,  // Персональный риск выше среднего
        val dominantFactor: String?,      // Какой фактор чаще всего влияет
        val accuracy: Float,              // Точность модели (0-1)
        val message: String               // Текст для пользователя
    )

    /**
     * Подготовка датасета: берём последние записи из БД
     */
    fun prepareDataset(dbHelper: DatabaseHelper, maxSamples: Int = 20): List<TrainingSample> {
        val entries = dbHelper.getAllWellbeingEntries()
        val weatherHistory = dbHelper.getWeatherHistory(maxSamples)

        val samples = mutableListOf<TrainingSample>()

        // Сопоставляем записи дневника с погодой по времени
        for (entry in entries.take(maxSamples)) {
            val timestamp = entry["timestamp"] as? Long ?: continue
            val wellbeing = entry["score"] as? Int ?: continue

            // Ищем ближайшую запись погоды
            val weather = weatherHistory.minByOrNull {
                abs((it["timestamp"] as Long) - timestamp)
            } ?: continue

            samples.add(TrainingSample(
                deltaPressure = (weather["pressure_change"] as? Float) ?: 0f,
                deltaTemp = (weather["temp_change"] as? Float) ?: 0f,
                kpIndex = (weather["kp"] as? Float) ?: 0f,
                deltaHumidity = (weather["humidity_change"] as? Float) ?: 0f,
                wellbeingScore = wellbeing
            ))
        }

        return samples
    }

    /**
     * Обучение модели: считаем корреляции факторов с плохим самочувствием
     */
    fun trainModel(samples: List<TrainingSample>): Map<String, Float> {
        if (samples.size < MIN_RECORDS_FOR_PREDICTION) return emptyMap()

        val correlations = mutableMapOf<String, Float>()

        // Для каждого фактора считаем: насколько он связан с низким wellbeing
        val factors = listOf(
            "pressure" to samples.map { it.deltaPressure },
            "temperature" to samples.map { it.deltaTemp },
            "kp" to samples.map { it.kpIndex },
            "humidity" to samples.map { it.deltaHumidity }
        )

        for ((name, values) in factors) {
            val correlation = calculateCorrelation(values, samples.map { 10 - it.wellbeingScore })
            if (abs(correlation) > CORRELATION_THRESHOLD) {
                correlations[name] = correlation
            }
        }

        return correlations
    }

    /**
     * Простой расчёт корреляции Пирсона (упрощённый)
     */
    private fun calculateCorrelation(x: List<Float>, y: List<Int>): Float {
        if (x.size != y.size || x.isEmpty()) return 0f

        val n = x.size.toFloat()
        val meanX = x.sum() / n
        val meanY = y.average().toFloat()

        var numerator = 0f
        var denomX = 0f
        var denomY = 0f

        for (i in x.indices) {
            val dx = x[i] - meanX
            val dy = y[i] - meanY
            numerator += dx * dy
            denomX += dx * dx
            denomY += dy * dy
        }

        val denominator = kotlin.math.sqrt(denomX * denomY)
        return if (denominator > 0) numerator / denominator else 0f
    }

    /**
     * Прогноз: оцениваем персональный риск на основе текущих условий
     */
    fun predictRisk(
        context: Context,
        dbHelper: DatabaseHelper,
        currentDeltaPressure: Float,
        currentDeltaTemp: Float,
        currentKp: Float,
        currentDeltaHumidity: Float
    ): RiskPrediction {

        val samples = prepareDataset(dbHelper)
        if (samples.size < MIN_RECORDS_FOR_PREDICTION) {
            return RiskPrediction(
                isPersonalRiskHigh = false,
                dominantFactor = null,
                accuracy = 0f,
                message = "Недостаточно данных для персонального прогноза"
            )
        }

        val correlations = trainModel(samples)
        if (correlations.isEmpty()) {
            return RiskPrediction(
                isPersonalRiskHigh = false,
                dominantFactor = null,
                accuracy = 0f,
                message = "Персональные закономерности не обнаружены"
            )
        }

        // Проверяем, совпадают ли текущие условия с «опасными» паттернами
        var personalRiskScore = 0f
        var dominantFactor: String? = null

        for ((factor, correlation) in correlations) {
            val currentValue = when (factor) {
                "pressure" -> currentDeltaPressure
                "temperature" -> currentDeltaTemp
                "kp" -> currentKp
                "humidity" -> currentDeltaHumidity
                else -> 0f
            }

            // Если знак корреляции и значения совпадают — риск растёт
            if (correlation > 0 && currentValue > 0 || correlation < 0 && currentValue < 0) {
                personalRiskScore += abs(correlation)
                if (dominantFactor == null || abs(correlation) > abs(correlations[dominantFactor] ?: 0f)) {
                    dominantFactor = factor
                }
            }
        }

        // Расчёт точности (упрощённо: доля записей, где модель «угадала»)
        val accuracy = calculateAccuracy(samples, correlations)

        val isHighRisk = personalRiskScore > 0.7f
        val message = buildMessage(isHighRisk, dominantFactor, accuracy)

        return RiskPrediction(isHighRisk, dominantFactor, accuracy, message)
    }

    /**
     * Упрощённая оценка точности модели
     */
    private fun calculateAccuracy(samples: List<TrainingSample>, correlations: Map<String, Float>): Float {
        if (samples.isEmpty()) return 0f

        var correct = 0
        for (sample in samples) {
            val isBadWellbeing = sample.wellbeingScore <= 4
            var predictedBad = false

            for ((factor, correlation) in correlations) {
                val value = when (factor) {
                    "pressure" -> sample.deltaPressure
                    "temperature" -> sample.deltaTemp
                    "kp" -> sample.kpIndex
                    "humidity" -> sample.deltaHumidity
                    else -> 0f
                }
                if ((correlation > 0 && value > 0) || (correlation < 0 && value < 0)) {
                    predictedBad = true
                    break
                }
            }

            if (predictedBad == isBadWellbeing) correct++
        }

        return correct.toFloat() / samples.size
    }

    /**
     * Формирование сообщения для пользователя
     */
    private fun buildMessage(isHighRisk: Boolean, factor: String?, accuracy: Float): String {
        if (!isHighRisk) return "На основе вашей истории: погодные условия не вызывают тревоги"

        val factorName = when (factor) {
            "pressure" -> "изменение давления"
            "temperature" -> "перепад температуры"
            "kp" -> "магнитная активность"
            "humidity" -> "изменение влажности"
            else -> "погодные условия"
        }

        val accuracyText = when {
            accuracy > 0.8f -> "высокая"
            accuracy > 0.6f -> "средняя"
            else -> "низкая"
        }

        return "⚠️ Ваш личный риск выше среднего!\n" +
                "• Чаще всего на вас влияет: $factorName\n" +
                "• Точность прогноза: $accuracyText (${(accuracy * 100).toInt()}%)"
    }

    /**
     * Сохранение метрики точности в SharedPreferences
     */
    fun saveModelMetrics(context: Context, accuracy: Float) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putFloat("last_accuracy", accuracy)
            .putLong("last_updated", System.currentTimeMillis())
            .apply()
    }
}