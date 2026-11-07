package com.marcelo.huba50

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import org.json.JSONArray
import org.json.JSONObject

// Lê as notificações do Relógio para mostrar os timers na tela
class TimerService : NotificationListenerService() {

    override fun onListenerConnected() { atualizar() }
    override fun onNotificationPosted(sbn: StatusBarNotification) { atualizar() }
    override fun onNotificationRemoved(sbn: StatusBarNotification) { atualizar() }

    private fun atualizar() {
        val lista = JSONArray()
        try {
            for (sbn in activeNotifications ?: emptyArray()) {
                if (!ehRelogio(sbn.packageName)) continue
                val n = sbn.notification
                val ex = n.extras
                val titulo = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
                val texto = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
                val cronometro = ex.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER)
                val regressivo = ex.getBoolean("android.chronometerCountDown")
                val ehTimer = cronometro || (titulo + texto).contains("emporizador", true) || (titulo + texto).contains("timer", true)
                if (!ehTimer) continue
                val t = JSONObject()
                t.put("titulo", if (titulo.isEmpty()) "Timer" else titulo)
                t.put("texto", texto)
                t.put("fim", if (cronometro && regressivo) n.`when` else 0L)
                lista.put(t)
            }
        } catch (e: Exception) { }
        atual = lista.toString()
    }

    private fun ehRelogio(p: String) =
        p == "com.sec.android.app.clockpackage" || p == "com.google.android.deskclock" || p.contains("clock")

    companion object {
        @Volatile var atual = "[]"
    }
}
