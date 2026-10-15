package ro.smartshopping.list.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import ro.smartshopping.list.domain.model.ItemPurchaseStage
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object WhatsAppShareHelper {

    /**
     * Curăță numărul de telefon introdus de utilizator și îl aduce în format internațional valid WhatsApp:
     * - "0722 123 456" -> "40722123456" (Prefix automat de România)
     * - "+40 722 123 456" -> "40722123456"
     * - "0040722123456" -> "40722123456"
     * - "40722123456" -> "40722123456"
     */
    fun cleanPhoneNumber(rawNumber: String): String {
        val digitsOnly = rawNumber.replace(Regex("[^0-9]"), "")
        return when {
            digitsOnly.startsWith("07") && digitsOnly.length == 10 -> "4" + digitsOnly // 07xx -> 407xx
            digitsOnly.startsWith("407") && digitsOnly.length == 11 -> digitsOnly
            digitsOnly.startsWith("0040") -> digitsOnly.removePrefix("00")
            else -> digitsOnly
        }
    }

    /**
     * Validează dacă numărul are o structură rezonabilă pentru trimitere (minim 9 cifre).
     */
    fun isValidPhoneNumber(rawNumber: String): Boolean {
        val clean = cleanPhoneNumber(rawNumber)
        return clean.length in 9..15
    }

    /**
     * Formatează produsele rămase de cumpărat într-un mesaj bogat cu iconițe pentru WhatsApp.
     */
    fun formatRemainingListWithIcons(
        list: ShoppingList,
        items: List<ShoppingListItem>,
        onlyRemaining: Boolean = true,
        includeStoreContext: Boolean = true,
        includeAisleNotes: Boolean = true
    ): String {
        val targetItems = if (onlyRemaining) {
            items.filter { !it.isPurchased && it.purchaseStage != ItemPurchaseStage.PURCHASED }
        } else {
            items
        }

        val sb = StringBuilder()
        val titleTag = if (onlyRemaining) "RĂMASE DE CUMPĂRAT" else "LISTĂ DE CUMPĂRĂTURI"

        sb.append("🛒 *${list.name.uppercase()}* • _${titleTag}_\n")
        if (includeStoreContext && !list.storeName.isNullOrBlank()) {
            sb.append("📍 *Magazin:* ${list.storeName}\n")
        }
        val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ro", "RO"))
        sb.append("📅 ${dateFormat.format(Date())}\n\n")

        if (targetItems.isEmpty()) {
            sb.append("🎉 *Totul a fost bifat/cumpărat!* Nu mai sunt produse rămase pe listă.\n")
        } else {
            // Grupare pe categorii pentru claritate maximă în magazin
            val grouped = targetItems.groupBy { it.category?.takeIf { c -> c.isNotBlank() } ?: "Alimente & Diverse" }

            for ((cat, catItems) in grouped) {
                val catEmoji = ProductEmojiHelper.getCategoryEmoji(cat)
                sb.append("$catEmoji *${cat.uppercase()}*\n")

                for (item in catItems) {
                    val pEmoji = ProductEmojiHelper.getEmojiForProduct(item.productNameSnapshot, cat)
                    val qtyStr = formatQuantity(item.quantity)
                    val unitStr = item.unit.getSymbol(true)

                    sb.append("▫️ $pEmoji *${item.productNameSnapshot}* — $qtyStr $unitStr")

                    val price = item.effectiveUnitPrice
                    if (price != null && price > 0.0) {
                        val totalLine = item.calculatedLineTotal
                        sb.append(" (~${String.format(Locale.US, "%.2f", totalLine)} ${list.currency})")
                    }

                    if (includeAisleNotes && !item.notes.isNullOrBlank()) {
                        sb.append(" [_${item.notes}_]")
                    }
                    sb.append("\n")
                }
                sb.append("\n")
            }
        }

        // Divider & Total statistic
        sb.append("───────────────────────\n")
        val remainingCount = targetItems.size
        val remainingEstimated = targetItems.sumOf { (it.effectiveUnitPrice ?: 0.0) * it.quantity }

        sb.append("⏳ *Rămase de luat:* $remainingCount ")
        sb.append(if (remainingCount == 1) "produs" else "produse")
        if (remainingEstimated > 0.0) {
            sb.append("\n💰 *Total estimat rămas:* ~${String.format(Locale.US, "%.2f", remainingEstimated)} ${list.currency}")
        }
        sb.append("\n✨ _Trimis din Smart Shopping List 2026_")

        return sb.toString()
    }

    private fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            qty.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Construiește URL-ul de deep-link WhatsApp:
     * https://api.whatsapp.com/send?phone=...&text=...
     */
    fun buildWhatsAppUrl(phoneNumber: String?, messageText: String): String {
        val encodedText = URLEncoder.encode(messageText, "UTF-8").replace("+", "%20")
        val cleanPhone = phoneNumber?.let { cleanPhoneNumber(it) }?.takeIf { it.isNotBlank() }
        return if (!cleanPhone.isNullOrBlank()) {
            "https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedText"
        } else {
            "https://api.whatsapp.com/send?text=$encodedText"
        }
    }

    /**
     * Trimite mesajul direct pe WhatsApp către numărul specificat.
     */
    fun sendWhatsAppDirect(
        context: Context,
        phoneNumber: String?,
        messageText: String
    ): Boolean {
        val url = buildWhatsAppUrl(phoneNumber, messageText)
        val uri = Uri.parse(url)

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Încearcă WhatsApp Business (com.whatsapp.w4b)
            val businessIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp.w4b")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            try {
                context.startActivity(businessIntent)
                true
            } catch (e2: Exception) {
                // Fallback la browser sau intent general
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                try {
                    context.startActivity(fallbackIntent)
                    true
                } catch (e3: Exception) {
                    ShareIntentHelper.shareText(context, messageText, "Trimite pe WhatsApp")
                    false
                }
            }
        }
    }
}
