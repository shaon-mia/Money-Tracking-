package com.example.alarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.UUID

data class TextAlarmItem(
    val id: String = UUID.randomUUID().toString().replace("-", "").take(12),
    val text: String,
    val hour: Int,
    val minute: Int,
    val repeatDaily: Boolean = true,
    val enabled: Boolean = true
) {
    fun requestCode(): Int = (id.hashCode() and 0x0FFFFFFF) + 1000
}

data class AlarmConfigState(
    val dailyEnabled: Boolean = false,
    val dailyHour: Int = 21,
    val dailyMinute: Int = 0,
    val dailyText: String = "আজকের আয়-ব্যয় হিসাব লিখে রাখুন!",
    val textAlarms: List<TextAlarmItem> = emptyList()
)

object AlarmScheduler {

    private const val PREF_NAME = "moneytracker_alarm_prefs"
    private const val KEY_DAILY_ENABLED = "daily_enabled"
    private const val KEY_DAILY_HOUR = "daily_hour"
    private const val KEY_DAILY_MINUTE = "daily_minute"
    private const val KEY_DAILY_TEXT = "daily_text"
    private const val KEY_TEXT_ALARMS = "text_alarms_json"

    private const val DAILY_REQUEST_CODE = 701
    private const val LEGACY_CHANNEL_ID = "moneytracker_alarms"
    const val CHANNEL_ID = "moneytracker_text_alarm_notif"
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_TITLE = "extra_alarm_title"
    const val EXTRA_ALARM_TEXT = "extra_alarm_text"
    const val EXTRA_IS_DAILY = "extra_is_daily"
    const val EXTRA_REPEAT_DAILY = "extra_repeat_daily"
    const val EXTRA_HOUR = "extra_hour"
    const val EXTRA_MINUTE = "extra_minute"

    fun loadState(context: Context): AlarmConfigState {
        val sp = context.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val rawJson = sp.getString(KEY_TEXT_ALARMS, "[]") ?: "[]"
        val list = mutableListOf<TextAlarmItem>()
        runCatching {
            val arr = JSONArray(rawJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val text = obj.optString("text", "").trim()
                if (text.isNotEmpty()) {
                    list.add(
                        TextAlarmItem(
                            id = obj.optString("id").ifBlank { UUID.randomUUID().toString().take(12) },
                            text = text,
                            hour = obj.optInt("hour", 9).coerceIn(0, 23),
                            minute = obj.optInt("minute", 0).coerceIn(0, 59),
                            repeatDaily = obj.optBoolean("repeatDaily", true),
                            enabled = obj.optBoolean("enabled", true)
                        )
                    )
                }
            }
        }
        return AlarmConfigState(
            dailyEnabled = sp.getBoolean(KEY_DAILY_ENABLED, false),
            dailyHour = sp.getInt(KEY_DAILY_HOUR, 21).coerceIn(0, 23),
            dailyMinute = sp.getInt(KEY_DAILY_MINUTE, 0).coerceIn(0, 59),
            dailyText = sp.getString(KEY_DAILY_TEXT, "আজকের আয়-ব্যয় হিসাব লিখে রাখুন!")
                ?: "আজকের আয়-ব্যয় হিসাব লিখে রাখুন!",
            textAlarms = list
        )
    }

    fun saveAndScheduleAll(context: Context, state: AlarmConfigState): AlarmConfigState {
        val appCtx = context.applicationContext
        val previous = loadState(appCtx)
        previous.textAlarms.forEach { oldItem ->
            cancelPendingIntent(appCtx, oldItem.requestCode())
        }
        cancelPendingIntent(appCtx, DAILY_REQUEST_CODE)

        val arr = JSONArray()
        state.textAlarms.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("hour", item.hour)
                put("minute", item.minute)
                put("repeatDaily", item.repeatDaily)
                put("enabled", item.enabled)
            })
        }

        appCtx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).edit()
            .putBoolean(KEY_DAILY_ENABLED, state.dailyEnabled)
            .putInt(KEY_DAILY_HOUR, state.dailyHour)
            .putInt(KEY_DAILY_MINUTE, state.dailyMinute)
            .putString(KEY_DAILY_TEXT, state.dailyText)
            .putString(KEY_TEXT_ALARMS, arr.toString())
            .apply()

        rescheduleActiveAlarms(appCtx)
        return state
    }

    fun rescheduleActiveAlarms(context: Context) {
        val appCtx = context.applicationContext
        val state = loadState(appCtx)
        // Only Text Alarms are active; cancel any legacy general daily alarm
        cancelPendingIntent(appCtx, DAILY_REQUEST_CODE)
        state.textAlarms.filter { it.enabled }.forEach { item ->
            scheduleOne(
                context = appCtx,
                requestCode = item.requestCode(),
                alarmId = item.id,
                title = "MoneyTracker • টেক্সট এলাম",
                text = item.text,
                hour = item.hour,
                minute = item.minute,
                isDailyMain = false,
                repeatDaily = item.repeatDaily
            )
        }
    }

    fun disableOneShotTextAlarm(context: Context, alarmId: String) {
        val appCtx = context.applicationContext
        val current = loadState(appCtx)
        val updatedList = current.textAlarms.map {
            if (it.id == alarmId && !it.repeatDaily) it.copy(enabled = false) else it
        }
        saveAndScheduleAll(appCtx, current.copy(textAlarms = updatedList))
    }

    fun scheduleOne(
        context: Context,
        requestCode: Int,
        alarmId: String,
        title: String,
        text: String,
        hour: Int,
        minute: Int,
        isDailyMain: Boolean,
        repeatDaily: Boolean
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
            set(Calendar.MINUTE, minute.coerceIn(0, 59))
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 2_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val intent = Intent(context, DailyAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarmId)
            putExtra(EXTRA_ALARM_TITLE, title)
            putExtra(EXTRA_ALARM_TEXT, text)
            putExtra(EXTRA_IS_DAILY, isDailyMain)
            putExtra(EXTRA_REPEAT_DAILY, repeatDaily)
            putExtra(EXTRA_HOUR, hour)
            putExtra(EXTRA_MINUTE, minute)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerAt = cal.timeInMillis
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }

    private fun cancelPendingIntent(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DailyAlarmReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            alarmManager.cancel(pi)
            pi.cancel()
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            runCatching { nm.deleteNotificationChannel(LEGACY_CHANNEL_ID) }
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Text Alarm Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Plays standard notification sound only for text alarms"
                enableVibration(false)
                enableLights(false)
                setSound(soundUri, audioAttrs)
            }
            nm.createNotificationChannel(channel)
        }
    }

    fun showAlarmNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String
    ) {
        ensureNotificationChannel(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPi = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSound(soundUri)
            .setSilent(false)
            .setVibrate(longArrayOf(0L))
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        runCatching { nm.notify(notificationId, notif) }
    }

    fun formatAlarmTime(hour: Int, minute: Int, isBn: Boolean): String {
        val h12 = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        val suffix = if (hour < 12) {
            if (isBn) "সকাল" else "AM"
        } else {
            if (isBn) "বিকাল/রাত" else "PM"
        }
        return String.format("%02d:%02d %s", h12, minute, suffix)
    }
}
