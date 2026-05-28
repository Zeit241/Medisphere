package com.example.kursovaya.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.kursovaya.R
import com.example.kursovaya.activity.MainActivity

class QueueNotificationManager(private val context: Context) {
    companion object {
        private const val CHANNEL_ID = "queue_updates_channel"
        private const val CHANNEL_NAME = "Обновления очереди"
        private const val CHANNEL_DESCRIPTION = "Уведомления об изменениях в очереди к врачу"
        private const val NOTIFICATION_ID_BASE = 1000
    }

    private val notificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                    enableLights(true)
                }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Показывает уведомление об изменении позиции в очереди
     */
    fun showQueuePositionUpdate(
        doctorName: String,
        newPosition: Int,
        previousPosition: Int? = null,
    ) {
        val message =
            when {
                newPosition == 0 -> "Ваша очередь! Подойдите к врачу."
                newPosition == 1 -> "Вы следующий в очереди. Приготовьтесь."
                previousPosition != null && newPosition < previousPosition -> {
                    val moved = previousPosition - newPosition
                    "Вы продвинулись на $moved ${if (moved == 1) "позицию" else "позиции"}. Ваша позиция: ${newPosition + 1}"
                }
                else -> "Ваша позиция в очереди: ${newPosition + 1}"
            }

        showNotification(
            title = "Очередь к $doctorName",
            message = message,
            doctorName = doctorName,
            position = newPosition,
        )
    }

    /**
     * Показывает уведомление о том, что очередь скоро подойдет
     */
    fun showQueueApproaching(
        doctorName: String,
        position: Int,
    ) {
        val message =
            when (position) {
                0 -> "Ваша очередь! Подойдите к врачу."
                1 -> "Вы следующий в очереди. Приготовьтесь."
                2 -> "Скоро ваша очередь. Осталось 2 человека."
                else -> "До вашей очереди осталось ${position + 1} человек."
            }

        showNotification(
            title = "Очередь к $doctorName",
            message = message,
            doctorName = doctorName,
            position = position,
        )
    }

    /**
     * Показывает общее уведомление об обновлении очереди
     */
    fun showQueueUpdate(
        doctorName: String,
        message: String,
    ) {
        showNotification(
            title = "Обновление очереди",
            message = "$doctorName: $message",
            doctorName = doctorName,
        )
    }

    private fun showNotification(
        title: String,
        message: String,
        doctorName: String,
        position: Int? = null,
    ) {
        val intent =
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("fragment", "queue")
            }

        val pendingIntent =
            PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val notificationId =
            position?.let { NOTIFICATION_ID_BASE + it }
                ?: (doctorName.hashCode() and 0x7FFFFFFF)

        val notification =
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_queue_empty)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .build()

        // Проверяем разрешение для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                android.util.Log.w("QueueNotificationManager", "POST_NOTIFICATIONS permission not granted")
                return
            }
        }

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (notificationManagerCompat.areNotificationsEnabled()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    // Для Android 13+ проверяем разрешение еще раз
                    if (ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationManagerCompat.notify(notificationId, notification)
                        android.util.Log.d("QueueNotificationManager", "Notification sent: $title - $message (ID: $notificationId)")
                    } else {
                        android.util.Log.w("QueueNotificationManager", "Permission denied, cannot send notification")
                    }
                } else {
                    // Для старых версий Android просто отправляем
                    notificationManagerCompat.notify(notificationId, notification)
                    android.util.Log.d("QueueNotificationManager", "Notification sent: $title - $message (ID: $notificationId)")
                }
            } catch (e: SecurityException) {
                android.util.Log.e("QueueNotificationManager", "Failed to send notification: ${e.message}", e)
            } catch (e: Exception) {
                android.util.Log.e("QueueNotificationManager", "Unexpected error sending notification", e)
            }
        } else {
            android.util.Log.w("QueueNotificationManager", "Notifications are disabled. Please enable in app settings.")
        }
    }

    /**
     * Отменяет все уведомления
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Отменяет уведомление для конкретного врача
     */
    fun cancelNotification(doctorName: String) {
        val notificationId = doctorName.hashCode() and 0x7FFFFFFF
        notificationManager.cancel(notificationId)
    }
}
