package ro.smartshopping.list.util

import android.content.Context
import android.content.Intent

object ShareIntentHelper {

    /**
     * Triggers the native Android Sharesheet with ACTION_SEND.
     * Allows the user to select WhatsApp, Telegram, SMS, Email, etc.
     */
    fun shareText(context: Context, text: String, title: String = "Trimite lista de cumpărături") {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, title)
        context.startActivity(shareIntent)
    }

    /**
     * Direct WhatsApp shortcut intent (if WhatsApp is installed).
     * Falls back to general Sharesheet if WhatsApp is not installed.
     */
    fun shareViaWhatsApp(context: Context, text: String) {
        val whatsappIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            `package` = "com.whatsapp"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            // Fallback to standard chooser
            shareText(context, text, "Trimite pe WhatsApp sau alte aplicații")
        }
    }
}
