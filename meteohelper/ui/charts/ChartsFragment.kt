package com.example.meteohelper.ui.charts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.meteohelper.data.api.RetrofitInstance
import com.example.meteohelper.databinding.FragmentChartsBinding
import kotlinx.coroutines.launch

class ChartsFragment : Fragment() {
    private var _binding: FragmentChartsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadChartsFromApi()
    }

    private fun loadChartsFromApi() {
        lifecycleScope.launch {
            try {
                // 1. Загружаем погоду
                val response = RetrofitInstance.weatherApi.getWeather(55.75, 37.62)

                // Берем только первые 24 часа
                val limit = 24
                val times = response.hourly.time.take(limit)
                val temps = response.hourly.temperature_2m.take(limit)
                val pressuresHpa = response.hourly.pressure_msl.take(limit)

                // 2. Форматируем данные

                // Время: из "2026-05-14T00:00" делаем "00:00"
                var labels = times.map { time ->
                    if (time.length > 12) time.substring(11, 16) else time
                }

                // Температура
                val tempData = temps.map { it.toFloat() }

                // Давление: конвертируем в мм рт. ст.
                val pressureData = pressuresHpa.map { (it * 0.75006).toFloat() }

                // 3. Отображаем

                // 🔥 ГРАФИК ДАВЛЕНИЯ (Убрали minY и maxY для авто-масштаба)
                binding.chartPressure.apply {
                    data = pressureData
                    labels = labels
                    lineColor = Color.parseColor("#FF387C")
                    // minY = 700f  <-- УДАЛЕНО
                    // maxY = 800f  <-- УДАЛЕНО
                }

                // 🔥 ГРАФИК ТЕМПЕРАТУРЫ (Авто-масштаб)
                binding.chartTemperature.apply {
                    data = tempData
                    labels = labels
                    lineColor = Color.parseColor("#FF387C")
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}