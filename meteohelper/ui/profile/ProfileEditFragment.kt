package com.example.meteohelper.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.meteohelper.R
import com.example.meteohelper.data.db.DatabaseHelper
import com.example.meteohelper.databinding.FragmentProfileEditBinding
import com.google.android.material.textfield.TextInputEditText

class ProfileEditFragment : Fragment() {
    private var _binding: FragmentProfileEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper

    // Ответы на тест
    private var testScore = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        setupGenderSpinner()
        setupTestQuestions()
        setupSymptomCheckboxes()

        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun setupGenderSpinner() {
        val genders = listOf("Мужской", "Женский")
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            genders
        )
        binding.spinnerGender.setAdapter(adapter)
    }

    private fun setupTestQuestions() {
        // Вопрос 1
        binding.rbQ1a.setOnClickListener { testScore = 0 }
        binding.rbQ1b.setOnClickListener { testScore = 1 }
        binding.rbQ1c.setOnClickListener { testScore = 2 }

        // Вопрос 2
        binding.rbQ2a.setOnClickListener { testScore += 0 }
        binding.rbQ2b.setOnClickListener { testScore += 1 }
        binding.rbQ2c.setOnClickListener { testScore += 2 }

        // Вопрос 3
        binding.rbQ3a.setOnClickListener { testScore += 0 }
        binding.rbQ3b.setOnClickListener { testScore += 1 }
        binding.rbQ3c.setOnClickListener { testScore += 2 }

        // Вопрос 4
        binding.rbQ4a.setOnClickListener { testScore += 0 }
        binding.rbQ4b.setOnClickListener { testScore += 1 }
        binding.rbQ4c.setOnClickListener { testScore += 2 }

        // Вопрос 5
        binding.rbQ5a.setOnClickListener { testScore += 0 }
        binding.rbQ5b.setOnClickListener { testScore += 1 }
        binding.rbQ5c.setOnClickListener { testScore += 2 }
    }

    private fun setupSymptomCheckboxes() {
        val symptomsList = listOf(
            "Головная боль",
            "Слабость / усталость",
            "Скачки давления",
            "Боли в суставах",
            "Бессонница",
            "Раздражительность",
            "Тошнота",
            "Одышка",
            "Головокружение"
        )

        val container = binding.symptomsContainer
        container.removeAllViews()

        for (symptom in symptomsList) {
            val checkBox = layoutInflater.inflate(
                R.layout.item_symptom_checkbox,
                container,
                false
            ) as CheckBox
            checkBox.id = View.generateViewId()
            checkBox.text = symptom
            checkBox.tag = symptom
            container.addView(checkBox)
        }
    }

    private fun saveProfile() {
        val name = binding.etName.text.toString().ifEmpty { "Пользователь" }
        val age = binding.etAge.text.toString().toIntOrNull() ?: 0

        // Для AutoCompleteTextView используем .text.toString()
        val genderText = binding.spinnerGender.text.toString()
        val gender = when (genderText) {
            "Мужской" -> "male"
            "Женский" -> "female"
            else -> "unknown"
        }

        val diseases = binding.etDiseases.text.toString()

        // Собираем выбранные симптомы из чекбоксов
        val selectedSymptoms = mutableListOf<String>()
        for (i in 0 until binding.symptomsContainer.childCount) {
            val cb = binding.symptomsContainer.getChildAt(i) as CheckBox
            if (cb.isChecked) {
                selectedSymptoms.add(cb.text.toString())
            }
        }

        // Добавляем "другие" симптомы
        val otherSymptoms = binding.etOtherSymptoms.text.toString().trim()
        if (otherSymptoms.isNotEmpty()) {
            selectedSymptoms.add(otherSymptoms)
        }
        val symptoms = selectedSymptoms.joinToString(", ")

        // Определяем тип метеозависимости
        val weatherType = when {
            testScore <= 3 -> "none"
            testScore <= 6 -> "mild"
            testScore <= 9 -> "moderate"
            else -> "severe"
        }

        dbHelper.saveUserProfile(name, age, gender, diseases, weatherType, symptoms)

        Toast.makeText(requireContext(), "Профиль сохранён!", Toast.LENGTH_SHORT).show()

        // Возврат на главную страницу профиля
        requireActivity().supportFragmentManager.popBackStack()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}