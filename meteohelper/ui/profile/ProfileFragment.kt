package com.example.meteohelper.ui.profile

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.example.meteohelper.R
import com.example.meteohelper.data.db.DatabaseHelper
import com.example.meteohelper.databinding.FragmentProfileBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        loadProfile()

        binding.btnEditProfile.setOnClickListener {
            // Переход к редактированию
            val fragmentManager = requireActivity().supportFragmentManager
            fragmentManager.beginTransaction()
                .replace(R.id.fragment_container, ProfileEditFragment())
                .addToBackStack(null)
                .commit()
        }

        // ПАСХАЛКА - секретная кнопка
        binding.btnEasterEgg.setOnClickListener {
            showEasterEgg()
        }
    }

    private fun loadProfile() {
        val profile = dbHelper.getUserProfile()
        if (profile != null) {
            val name = profile["name"] as? String ?: ""
            val age = profile["age"] as? Int ?: 0
            val gender = profile["gender"] as? String ?: ""
            val diseases = profile["diseases"] as? String ?: ""
            val weatherType = profile["weatherType"] as? String ?: ""
            val symptoms = profile["symptoms"] as? String ?: ""

            binding.tvUserName.text = if (name.isNotEmpty()) name else "Пользователь"
            binding.tvUserInfo.text = """
                Возраст: $age лет
                Пол: ${getGenderText(gender)}
                Заболевания: ${if (diseases.isNotEmpty()) diseases else "Не указаны"}
                
                Тип метеозависимости: ${getWeatherTypeText(weatherType)}
                
                Типичные симптомы:
                ${if (symptoms.isNotEmpty()) symptoms else "Не указаны"}
            """.trimIndent()
        }

        else {
            binding.tvUserName.text = "Профиль не заполнен"
            binding.tvUserInfo.text = "Нажмите «Редактировать профиль», чтобы добавить информацию"
        }
        binding.tvUserInfo.setTextColor(Color.BLACK)

    }

    private fun getGenderText(gender: String): String = when (gender) {
        "male" -> "Мужской"
        "female" -> "Женский"
        else -> "Не указан"
    }

    private fun getWeatherTypeText(type: String): String = when (type) {
        "none" -> "Не метеозависим"
        "mild" -> "Лёгкая степень"
        "moderate" -> "Средняя степень"
        "severe" -> "Тяжёлая степень"
        else -> "Не определён"
    }

    private fun showEasterEgg() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_easter_egg, null)
        val imageView = dialogView.findViewById<ImageView>(R.id.ivEasterEgg)

        // Загрузка GIF (можно заменить на свою ссылку)
        imageView.setImageResource(R.drawable.blade) // Или используй Glide для загрузки из интернета

        AlertDialog.Builder(requireContext())
            .setTitle("🎉 Секретная пасхалка!")
            .setView(dialogView)
            .setPositiveButton("Закрыть", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        loadProfile() // Обновляем при возврате
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}