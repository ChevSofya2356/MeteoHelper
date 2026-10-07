package com.example.meteohelper.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.meteohelper.utils.NotificationHelper

class MorningReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        NotificationHelper.showReminderNotification(context)
    }
}