package com.example.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class DailyAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_ID) ?: "daily"
        val title = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TITLE) ?: "MoneyTracker এলাম"
        val text = intent.getStringExtra(AlarmScheduler.EXTRA_ALARM_TEXT)
            ?: "আজকের আয়-ব্যয় হিসাব লিখে রাখুন!"
        val isDailyMain = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_DAILY, true)
        val repeatDaily = intent.getBooleanExtra(AlarmScheduler.EXTRA_REPEAT_DAILY, true)
        val hour = intent.getIntExtra(AlarmScheduler.EXTRA_HOUR, 21)
        val minute = intent.getIntExtra(AlarmScheduler.EXTRA_MINUTE, 0)

        val notifId = if (isDailyMain) 9001 else 9100 + (alarmId.hashCode() and 0x0FFF)
        AlarmScheduler.showAlarmNotification(
            context = context,
            notificationId = notifId,
            title = title,
            message = text
        )

        if (repeatDaily) {
            AlarmScheduler.scheduleOne(
                context = context,
                requestCode = if (isDailyMain) 701 else TextAlarmItem(id = alarmId, text = text, hour = hour, minute = minute).requestCode(),
                alarmId = alarmId,
                title = title,
                text = text,
                hour = hour,
                minute = minute,
                isDailyMain = isDailyMain,
                repeatDaily = true
            )
        } else {
            AlarmScheduler.disableOneShotTextAlarm(context, alarmId)
        }
    }
}

class BootAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            AlarmScheduler.rescheduleActiveAlarms(context)
        }
    }
}
