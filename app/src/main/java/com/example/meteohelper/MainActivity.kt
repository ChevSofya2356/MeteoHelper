package com.example.meteohelper

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.meteohelper.databinding.ActivityMainBinding
import com.example.meteohelper.ui.charts.ChartsFragment
import com.example.meteohelper.ui.diary.DiaryFragment
import com.example.meteohelper.ui.home.HomeFragment
import com.example.meteohelper.ui.profile.ProfileFragment
import com.example.meteohelper.utils.ReminderScheduler

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    // Запрос разрешения на уведомления (Android 13+)
    private val requestNotificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) ReminderScheduler.scheduleDailyReminder(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }

        binding.navView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> { replaceFragment(HomeFragment()); true }
                R.id.navigation_diary -> { replaceFragment(DiaryFragment()); true }
                R.id.navigation_charts -> { replaceFragment(ChartsFragment()); true }
                R.id.navigation_profile -> { replaceFragment(ProfileFragment()); true }
                else -> false
            }
        }

        // Запуск планировщика уведомлений
        setupMorningReminder()
    }

    private fun setupMorningReminder() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+: запрашиваем разрешение
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                ReminderScheduler.scheduleDailyReminder(this)
            }
        } else {
            // Android 12 и ниже: сразу планируем
            ReminderScheduler.scheduleDailyReminder(this)
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}