package com.example.meteohelper.ui.diary

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
import androidx.appcompat.app.AlertDialog as AppCompatAlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.meteohelper.R
import com.example.meteohelper.data.db.DatabaseHelper
import com.example.meteohelper.databinding.FragmentDiaryBinding
import java.text.SimpleDateFormat
import java.util.*

class DiaryFragment : Fragment() {
    private var _binding: FragmentDiaryBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var diaryAdapter: DiaryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDiaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        setupRecyclerView()

        binding.btnAddEntry.setOnClickListener {
            showAddDiaryDialog()
        }

        loadDiaryEntries()
    }

    private fun setupRecyclerView() {
        diaryAdapter = DiaryAdapter(mutableListOf()) { entryId ->
            dbHelper.deleteWellbeingEntry(entryId)
            loadDiaryEntries()
            Toast.makeText(requireContext(), "Запись удалена", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewDiary.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = diaryAdapter
        }
    }

    private fun loadDiaryEntries() {
        val entries = dbHelper.getAllWellbeingEntries()
        diaryAdapter.updateEntries(entries)
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

        val dialog = AppCompatAlertDialog.Builder(requireContext())
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
        dialog.getButton(AppCompatAlertDialog.BUTTON_POSITIVE).setTextColor(accentColor)
        dialog.getButton(AppCompatAlertDialog.BUTTON_NEGATIVE).setTextColor(accentColor)
    }

    private fun saveToDatabase(score: Int, symptoms: String, notes: String) {
        val timestamp = System.currentTimeMillis()
        val date = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

        val resultId = dbHelper.addWellbeingEntry(timestamp, date, score, symptoms, notes)
        if (resultId != -1L) {
            Toast.makeText(requireContext(), "Запись сохранена! ($date)", Toast.LENGTH_SHORT).show()
            loadDiaryEntries()
        } else {
            Toast.makeText(requireContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDiaryEntries()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}