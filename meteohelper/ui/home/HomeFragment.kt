package com.example.meteohelper.ui.home

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.EditText
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.meteohelper.R
import com.example.meteohelper.data.api.RetrofitInstance
import com.example.meteohelper.data.db.DatabaseHelper
import com.example.meteohelper.databinding.FragmentHomeBinding
import com.example.meteohelper.utils.AdviceHelper
import com.example.meteohelper.utils.PersonalAdviceModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper

    private var lastPressure: Float = 0f
    private var lastTemp: Float = 0f
    private var lastHumidity: Float = 0f

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        val prefs = requireContext().getSharedPreferences("meteo_prefs", Context.MODE_PRIVATE)
        lastPressure = prefs.getFloat("last_pressure", 0f)
        lastTemp = prefs.getFloat("last_temp", 0f)
        lastHumidity = prefs.getFloat("last_humidity", 0f)

        loadWeatherData()

        binding.addDiaryButton.setOnClickListener {
            showAddDiaryDialog()
        }

        binding.btnShowFullAdvice.setOnClickListener {
            showFullAdviceDialog()
        }
    }

    private fun loadWeatherData() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitInstance.weatherApi.getWeather(55.75, 37.62)

                if (!isAdded || _binding == null) return@launch

                val currentTimeStr = response.current_weather.time
                val currentIndex = response.hourly.time.indexOf(currentTimeStr)
                val index = if (currentIndex >= 0) currentIndex else 0

                val currentTemp = response.current_weather.temperature.toFloat()

                if (!isAdded || _binding == null) return@launch
                binding.tempValue.text = "${currentTemp.toInt()}°"

                val pressureHpa = response.hourly.pressure_msl.getOrNull(index)?.toDouble() ?: 1013.0
                val pressureInMmHg = (pressureHpa * 0.75006).toFloat()

                if (!isAdded || _binding == null) return@launch
                binding.pressureValue.text = "${pressureInMmHg.toInt()} мм"

                val humidity = response.hourly.relativehumidity_2m.getOrNull(index)?.toFloat() ?: 65f

                if (!isAdded || _binding == null) return@launch
                binding.humidityValue.text = "${humidity.toInt()}%"

                var currentKp = 2.0f
                var kpAdvice = "Спокойно"

                try {
                    val kpResponse = RetrofitInstance.noaaApi.getKpIndex()
                    val rawKp = kpResponse.firstOrNull()?.kpValue ?: "2"
                    currentKp = rawKp.toFloatOrNull() ?: 2.0f

                    kpAdvice = when {
                        currentKp >= 5.0f -> "Магнитная буря!"
                        currentKp >= 4.0f -> "Возбуждённая магнитосфера"
                        currentKp >= 3.0f -> "Небольшое возмущение"
                        else -> "Спокойно"
                    }
                } catch (e: Exception) {
                    currentKp = 2.0f
                    kpAdvice = "Спокойно"
                }

                if (!isAdded || _binding == null) return@launch
                binding.kpValue.text = "Kp=${currentKp.toInt()}"
                binding.magneticAdvice.text = kpAdvice

                val deltaPressure = if (lastPressure > 0) Math.abs(pressureInMmHg - lastPressure) else 0f
                val deltaTemp = if (lastTemp > 0) Math.abs(currentTemp - lastTemp) else 0f
                val deltaHumidity = if (lastHumidity > 0) Math.abs(humidity - lastHumidity) else 0f

                if (!isAdded || _binding == null) return@launch
                binding.pressureChange.text = buildString {
                    if (lastPressure > 0) {
                        append(if (pressureInMmHg > lastPressure) "+" else "")
                        append("${deltaPressure.toInt()} мм за сутки")
                    } else {
                        append("—")
                    }
                }

                if (!isAdded || _binding == null) return@launch
                binding.tempChange.text = buildString {
                    if (lastTemp > 0) {
                        append(if (currentTemp > lastTemp) "+" else "")
                        append("${deltaTemp.toInt()}° за сутки")
                    } else {
                        append("—")
                    }
                }

                val riskIndex = calculateRiskIndex(deltaPressure, deltaTemp, currentKp, deltaHumidity)

                if (!isAdded || _binding == null) return@launch
                updateRiskUI(riskIndex)
                updateAllAdvice(pressureInMmHg, currentKp, deltaPressure, deltaTemp, deltaHumidity)

                val prefs = requireContext().getSharedPreferences("meteo_prefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putFloat("last_pressure", pressureInMmHg)
                    .putFloat("last_temp", currentTemp)
                    .putFloat("last_humidity", humidity)
                    .putFloat("last_kp", currentKp)
                    .apply()

                dbHelper.saveWeatherData(
                    timestamp = System.currentTimeMillis(),
                    pressure = pressureInMmHg,
                    temp = currentTemp,
                    humidity = humidity,
                    kp = currentKp
                )

            } catch (e: Exception) {
                if (!isAdded) return@launch

                val errorMsg = when (e) {
                    is java.net.SocketTimeoutException -> "Таймаут соединения"
                    is java.net.UnknownHostException -> "Нет доступа к серверу"
                    is retrofit2.HttpException -> "Ошибка API: ${e.code()}"
                    else -> "Ошибка: ${e.message}"
                }
                Toast.makeText(requireContext(), "Демо-данные ($errorMsg)", Toast.LENGTH_LONG).show()

                if (_binding == null) return@launch
                binding.tempValue.text = "+22°"
                binding.pressureValue.text = "745 мм"
                binding.humidityValue.text = "65%"
                binding.kpValue.text = "Kp=2"
                binding.magneticAdvice.text = "Спокойно"
                binding.pressureChange.text = "—"
                binding.tempChange.text = "—"
                updateRiskUI(0.4f)
                updateAllAdvice(745f, 2f, 0f, 0f, 0f)
            }
        }
    }

    /**
     * Обновление советов из профиля (хронические заболевания)
     */
    private fun updateProfileAdvice(pressure: Float, kp: Float) {
        val profile = dbHelper.getUserProfile()
        val diseases = profile?.get("diseases") as? String ?: ""
        val profileAdvice = AdviceHelper.getProfileBasedAdvice(diseases, pressure, kp)

        val adviceText = if (profileAdvice.isEmpty()) {
            "✅ Нет специфических рекомендаций по профилю"
        } else {
            profileAdvice.joinToString("\n\n")
        }

        binding.tvProfileAdvice.text = adviceText
    }

    /**
     * Обновление советов из дневника (последние 5 записей)
     */
    private fun updateDiaryAdvice() {
        val diaryAdvice = PersonalAdviceModel.getDiaryBasedAdvice(requireContext(), dbHelper)

        val adviceText = if (diaryAdvice.isEmpty()) {
            "✅ В последних записях нет повторяющихся симптомов"
        } else {
            diaryAdvice.joinToString("\n\n")
        }

        binding.tvDiaryAdvice.text = adviceText
    }

    /**
     * Обновление всех советов
     */
    private fun updateAllAdvice(pressure: Float, kp: Float, deltaPressure: Float = 0f, deltaTemp: Float = 0f, deltaHumidity: Float = 0f) {
        updateProfileAdvice(pressure, kp)
        updateDiaryAdvice()
    }

    /**
     * Показ полного совета в диалоге
     */
    private fun showFullAdviceDialog() {
        val prefs = requireContext().getSharedPreferences("meteo_prefs", Context.MODE_PRIVATE)
        val pressure = prefs.getFloat("last_pressure", 745f)
        val kp = prefs.getFloat("last_kp", 2f)

        val profile = dbHelper.getUserProfile()
        val diseases = profile?.get("diseases") as? String ?: ""

        val profileAdvice = AdviceHelper.getProfileBasedAdvice(diseases, pressure, kp)
        val diaryAdvice = PersonalAdviceModel.getDiaryBasedAdvice(requireContext(), dbHelper)

        val fullAdviceText = buildString {
            append("📋 РЕКОМЕНДАЦИИ ПО ПРОФИЛЮ:\n\n")
            append(profileAdvice.joinToString("\n").ifEmpty { "Нет специфических рекомендаций" })
            append("\n\n")
            append("📊 АНАЛИЗ ДНЕВНИКА (последние 5 записей):\n\n")
            append(diaryAdvice.joinToString("\n").ifEmpty { "Нет повторяющихся симптомов" })
        }

        AlertDialog.Builder(requireContext())
            .setTitle("💡 Все рекомендации")
            .setMessage(fullAdviceText)
            .setPositiveButton("Закрыть", null)
            .setNeutralButton("📋 Копировать") { _, _ ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("MeteoHelper Advice", fullAdviceText)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(requireContext(), "Совет скопирован!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun calculateRiskIndex(
        deltaPressure: Float,
        deltaTemp: Float,
        kpIndex: Float,
        deltaHumidity: Float
    ): Float {
        val W1 = 0.3f
        val W2 = 0.2f
        val W3 = 0.3f
        val W4 = 0.2f

        val riskPressure = (deltaPressure / 8.0f) * W1
        val riskTemp = (deltaTemp / 10.0f) * W2
        val riskKp = (kpIndex / 5.0f) * W3
        val riskHumidity = (deltaHumidity / 20.0f) * W4

        return riskPressure + riskTemp + riskKp + riskHumidity
    }

    private fun updateRiskUI(riskIndex: Float) {
        val riskCard = binding.riskCard
        val riskIndexText = binding.riskIndexText
        val riskLevelText = binding.riskLevelText
        val riskAdviceText = binding.riskAdviceText

        val riskScore = (riskIndex * 10).toInt().coerceIn(0, 10)

        when {
            riskIndex <= 0.3f -> {
                riskCard.setCardBackgroundColor(Color.parseColor("#4CAF50"))
                riskIndexText.setTextColor(Color.WHITE)
                riskLevelText.setTextColor(Color.WHITE)
                riskAdviceText.setTextColor(Color.WHITE)
                riskLevelText.text = "Низкий риск"
                riskAdviceText.text = "Сегодня хорошая погода. Можно планировать обычные дела."
                binding.pressureChange.setTextColor(Color.parseColor("#4CAF50"))
                binding.tempChange.setTextColor(Color.parseColor("#4CAF50"))
            }
            riskIndex <= 0.7f -> {
                riskCard.setCardBackgroundColor(Color.parseColor("#FFC107"))
                riskIndexText.setTextColor(Color.BLACK)
                riskLevelText.setTextColor(Color.BLACK)
                riskAdviceText.setTextColor(Color.BLACK)
                riskLevelText.text = "Умеренный риск"
                riskAdviceText.text = "Обратите внимание на самочувствие. Снизьте нагрузки."
                binding.pressureChange.setTextColor(Color.parseColor("#FFC107"))
                binding.tempChange.setTextColor(Color.parseColor("#FFC107"))
            }
            else -> {
                riskCard.setCardBackgroundColor(Color.parseColor("#F44336"))
                riskIndexText.setTextColor(Color.WHITE)
                riskLevelText.setTextColor(Color.WHITE)
                riskAdviceText.setTextColor(Color.WHITE)
                riskLevelText.text = "Высокий риск"
                riskAdviceText.text = "Внимательно следуйте рекомендациям, избегайте перегрузок."
                binding.pressureChange.setTextColor(Color.parseColor("#F44336"))
                binding.tempChange.setTextColor(Color.parseColor("#F44336"))
            }
        }

        riskIndexText.text = "$riskScore / 10"
    }

    private fun showAddDiaryDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_diary_entry, null)
        val scoreSeekBar = dialogView.findViewById<SeekBar>(R.id.scoreSeekBar)
        val scoreText = dialogView.findViewById<TextView>(R.id.scoreText)
        val notesInput = dialogView.findViewById<EditText>(R.id.notesInput)

        val chkHeadache = dialogView.findViewById<CheckBox>(R.id.chk_headache)
        val chkDizziness = dialogView.findViewById<CheckBox>(R.id.chk_dizziness)
        val chkWeakness = dialogView.findViewById<CheckBox>(R.id.chk_weakness)
        val chkJoints = dialogView.findViewById<CheckBox>(R.id.chk_joints)
        val chkPressure = dialogView.findViewById<CheckBox>(R.id.chk_pressure)
        val chkMood = dialogView.findViewById<CheckBox>(R.id.chk_mood)
        val chkHeart = dialogView.findViewById<CheckBox>(R.id.chk_heart)

        scoreSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                scoreText.text = "$progress / 10"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        scoreSeekBar.progress = 5
        scoreText.text = "5 / 10"

        val accentColor = Color.parseColor("#FF387C")

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Как вы себя чувствуете?")
            .setView(dialogView)
            .setPositiveButton("Сохранить") { _, _ ->
                val selectedSymptoms = mutableListOf<String>()
                if (chkHeadache.isChecked) selectedSymptoms.add("Головная боль")
                if (chkDizziness.isChecked) selectedSymptoms.add("Головокружение")
                if (chkWeakness.isChecked) selectedSymptoms.add("Слабость / Сонливость")
                if (chkJoints.isChecked) selectedSymptoms.add("Боль в суставах")
                if (chkPressure.isChecked) selectedSymptoms.add("Скачки давления")
                if (chkMood.isChecked) selectedSymptoms.add("Перепады настроения")
                if (chkHeart.isChecked) selectedSymptoms.add("Дискоморт в сердце")
                if (selectedSymptoms.isEmpty()) selectedSymptoms.add("Нормально")

                val symptomsText = selectedSymptoms.joinToString(", ")
                val notes = notesInput.text.toString().ifEmpty { "—" }

                saveToDatabase(scoreSeekBar.progress, symptomsText, notes)
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(accentColor)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(accentColor)
    }

    private fun saveToDatabase(score: Int, symptoms: String, notes: String) {
        val timestamp = System.currentTimeMillis()
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

        val resultId = dbHelper.addWellbeingEntry(timestamp, date, score, symptoms, notes)
        if (resultId != -1L) {
            Toast.makeText(requireContext(), "Запись сохранена! ($date)", Toast.LENGTH_SHORT).show()

            // Обучаем модель
            val prefs = requireContext().getSharedPreferences("meteo_prefs", Context.MODE_PRIVATE)
            val currentPressure = prefs.getFloat("last_pressure", 750f)
            val currentKp = prefs.getFloat("last_kp", 2f)
            val weatherTags = PersonalAdviceModel.getWeatherTags(currentPressure, currentKp)
            val symptomList = symptoms.split(", ").map { it.trim() }
            PersonalAdviceModel.train(requireContext(), weatherTags, symptomList)

            // Обновляем советы на экране
            updateAllAdvice(currentPressure, currentKp)
        } else {
            Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadWeatherData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}