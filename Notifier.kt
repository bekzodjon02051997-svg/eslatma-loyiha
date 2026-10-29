package uz.eslatma

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDate
import java.time.ZoneId

object Notifier {
    const val CH = "eslatma_ch"
    private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

    private fun actPi(c: Context, day: String, act: String): PendingIntent {
        val i = Intent(c, NotifReceiver::class.java).setAction(act).putExtra("day", day)
        return PendingIntent.getBroadcast(c, (day + act).hashCode(), i, FLAGS)
    }

    private fun appPi(c: Context): PendingIntent =
        PendingIntent.getActivity(c, 0, Intent(c, MainActivity::class.java), FLAGS)

    fun post(c: Context, day: String) {
        val nm = c.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(CH, "Eslatmalar", NotificationManager.IMPORTANCE_HIGH))
        val open = Store.forDay(c, day).filter { !it.done }
        val nid = day.hashCode()
        if (open.isEmpty()) { nm.cancel(nid); return }
        val stage = Store.stage(c, day)
        val body = open.mapIndexed { i, it ->
            val t = if (it.min >= 0) String.format("%02d:%02d – ", it.min / 60, it.min % 60) else ""
            "${i + 1}. $t${it.text}"
        }.joinToString("\n")
        val title = if (stage == 1) "Haqiqatan bajarildimi?" else "Eslatma"
        val b = Notification.Builder(c, CH)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText("${open.size} ta vazifa")
            .setStyle(Notification.BigTextStyle().bigText(body))
            .setCategory(Notification.CATEGORY_REMINDER)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(appPi(c))
            .setDeleteIntent(actPi(c, day, "DISMISS"))
        if (stage == 0) {
            b.addAction(0, "1) ✅ Done", actPi(c, day, "DONE"))
        } else {
            b.addAction(0, "2) ✔ Confirm done", actPi(c, day, "CONFIRM"))
            b.addAction(0, "↩ Yo'q", actPi(c, day, "BACK"))
        }
        nm.notify(nid, b.build())
    }

    private fun alarmPi(c: Context, it: Item): PendingIntent {
        val i = Intent(c, NotifReceiver::class.java).setAction("SHOW").putExtra("day", it.day)
        return PendingIntent.getBroadcast(c, (it.id % 1000000007L).toInt(), i, FLAGS)
    }

    fun cancelAlarm(c: Context, it: Item) {
        c.getSystemService(AlarmManager::class.java).cancel(alarmPi(c, it))
    }

    fun refreshAll(c: Context) {
        val today = LocalDate.now()
        val am = c.getSystemService(AlarmManager::class.java)
        val items = Store.all(c).filter { !it.done }
        items.forEach {
            val h = if (it.min < 0) 8 * 60 else it.min
            val t = LocalDate.parse(it.day).atTime(h / 60, h % 60).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (t > System.currentTimeMillis()) am.setAlarmClock(AlarmManager.AlarmClockInfo(t, appPi(c)), alarmPi(c, it))
        }
        items.map { it.day }.distinct().filter { !LocalDate.parse(it).isAfter(today) }.forEach { post(c, it) }
    }
}

class NotifReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val day = i.getStringExtra("day") ?: return
        when (i.action) {
            "SHOW", "DISMISS" -> Notifier.post(c, day)
            "DONE" -> { Store.setStage(c, day, 1); Notifier.post(c, day) }
            "BACK" -> { Store.setStage(c, day, 0); Notifier.post(c, day) }
            "CONFIRM" -> {
                val l = Store.all(c)
                l.filter { it.day == day }.forEach { it.done = true }
                Store.save(c, l)
                Store.setStage(c, day, 0)
                Notifier.post(c, day)
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) { Notifier.refreshAll(c) }
}
