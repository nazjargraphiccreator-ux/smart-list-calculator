package ro.smartshopping.list.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ro.smartshopping.list.MainActivity
import ro.smartshopping.list.R

object StoreNotificationHelper {

    const val CHANNEL_ID = "militari_store_deals_channel"
    private const val CHANNEL_NAME = "Alerte Reduceri Magazine Militari"
    private const val CHANNEL_DESCRIPTION = "Notificări automate privind reducerile din Kaufland și Auchan Militari"

    const val BUDGET_CHANNEL_ID = "budget_alerts_channel"
    private const val BUDGET_CHANNEL_NAME = "Avertizări Buget Depășit"
    private const val BUDGET_CHANNEL_DESCRIPTION = "Alerte când cheltuielile din magazin depășesc bugetul alocat listei înainte de plată"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
            }
            val budgetChannel = NotificationChannel(BUDGET_CHANNEL_ID, BUDGET_CHANNEL_NAME, importance).apply {
                description = BUDGET_CHANNEL_DESCRIPTION
                enableLights(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 350, 150, 350)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
            notificationManager?.createNotificationChannel(budgetChannel)
        }
    }

    fun postStoreDealNotification(
        context: Context,
        notificationId: Int,
        storeName: String,
        title: String,
        message: String,
        discountHighlight: String? = null
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.app_brand_logo)
        } catch (e: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("[$storeName] $title")
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$message\n\n📌 $storeName • Militari București${discountHighlight?.let { "\n🔥 $it" } ?: ""}")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(notificationId, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission not granted yet - gracefully catch
        }
    }

    fun postBudgetExceededNotification(
        context: Context,
        listName: String,
        currentSpent: Double,
        budget: Double,
        currency: String = "RON"
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.app_brand_logo)
        } catch (e: Exception) {
            null
        }

        val overBudget = (currentSpent - budget).coerceAtLeast(0.0)
        val title = "⚠️ Avertizare Buget Depășit: $listName"
        val message = "Ai atins ${String.format(java.util.Locale.US, "%.2f", currentSpent)} $currency din bugetul alocat de ${String.format(java.util.Locale.US, "%.2f", budget)} $currency (+${String.format(java.util.Locale.US, "%.2f", overBudget)} $currency depășire). Verifică coșul înainte de a ajunge la casa de marcat!"

        val builder = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("🚨 **ALERTA PRE-CHECKOUT**\n\n$message\n\n💡 Sugestie: Elimină sau ajustează produsele opționale din coș înainte de plată.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(longArrayOf(0, 350, 150, 350))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (largeIcon != null) {
            builder.setLargeIcon(largeIcon)
        }

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(99001, builder.build())
        } catch (e: SecurityException) {
            // Android 13+ permission not granted yet - gracefully catch
        }
    }
}
