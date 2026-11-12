
package com.pipkeypet.android

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class PipNotificationService : NotificationListenerService() {

    companion object {
        private const val TAG = "PipNotifications"
    }

    override fun onNotificationPosted(
        sbn: StatusBarNotification
    ) {
        val notification = sbn.notification

        val title = notification.extras
            .getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            .orEmpty()

        val message = notification.extras
            .getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            .orEmpty()

        // Не обрабатываем уведомления самого Pip
        if (sbn.packageName == packageName) return

        // Не записываем содержимое уведомлений в журнал
        Log.d(
            TAG,
            "Notification received from ${sbn.packageName}"
        )

        // Позже добавим передачу через BLE.
        // Пока текст уведомлений никуда не отправляется.
    }

    override fun onNotificationRemoved(
        sbn: StatusBarNotification
    ) {
        Log.d(TAG, "Notification removed")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification listener disconnected")
    }
}
