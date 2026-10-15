package ro.smartshopping.list.data.local

import android.content.Context
import android.os.Environment
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LocalBackupStorageManager {

    private const val FOLDER_NAME = "SmartListCalculator"

    /**
     * Obține directorul dedicat din stocarea telefonului.
     * Folosește Documents/SmartListCalculator (sau directorul extern specific aplicației).
     */
    fun getDedicatedFolder(context: Context): File {
        val documentsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val folder = File(documentsDir, FOLDER_NAME)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        // Fallback dacă documentsDir nu este accesibil
        if (!folder.canWrite()) {
            val appSpecific = context.getExternalFilesDir(FOLDER_NAME) ?: context.filesDir
            if (!appSpecific.exists()) appSpecific.mkdirs()
            return appSpecific
        }
        return folder
    }

    /**
     * Salvează și exportă o listă de cumpărături într-un fișier .txt detaliat în folderul dedicat.
     */
    fun exportListToTxt(
        context: Context,
        list: ShoppingList,
        items: List<ShoppingListItem>
    ): File {
        val folder = getDedicatedFolder(context)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm", Locale.getDefault())
        val displayDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ro", "RO"))
        val now = Date()

        val sanitizedName = list.name.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(30)
        val fileName = "Lista_${sanitizedName}_${dateFormat.format(now)}.txt"
        val file = File(folder, fileName)

        val sb = StringBuilder()
        sb.append("=================================================================\n")
        sb.append("         SMART-LIST-CALCULATOR • RAPORT DETALIAT LISTĂ           \n")
        sb.append("=================================================================\n\n")

        sb.append("📋 NUME LISTĂ: ${list.name}\n")
        sb.append("📅 DATA SALVĂRII: ${displayDateFormat.format(now)}\n")
        if (!list.storeName.isNullOrBlank()) {
            sb.append("🏪 MAGAZIN ASOCIAT: ${list.storeName}\n")
        }
        if (!list.storeAddress.isNullOrBlank()) {
            sb.append("📍 ADRESĂ MAGAZIN: ${list.storeAddress}\n")
        }
        if (list.budget != null) {
            sb.append("💰 BUGET LIMITĂ: ${String.format(Locale.US, "%.2f", list.budget)} ${list.currency}\n")
        }
        sb.append("📊 TOTAL ESTIMAT: ${String.format(Locale.US, "%.2f", list.estimatedTotal)} ${list.currency}\n")
        if (list.actualTotal > 0.0) {
            sb.append("💳 TOTAL CHELTUIT EFECTIV: ${String.format(Locale.US, "%.2f", list.actualTotal)} ${list.currency}\n")
        }
        if (list.receiptTotal != null) {
            sb.append("🧾 TOTAL BON FISCAL: ${String.format(Locale.US, "%.2f", list.receiptTotal)} ${list.currency}\n")
        }
        sb.append("🎯 PROGRES: ${list.purchasedItemsCount}/${list.totalItemsCount} produse (${list.completionPercentage}%)\n\n")

        sb.append("-----------------------------------------------------------------\n")
        sb.append("                       PRODUSE ȘI DETALII                        \n")
        sb.append("-----------------------------------------------------------------\n")

        if (items.isEmpty()) {
            sb.append("(Lista nu conține produse)\n")
        } else {
            items.forEachIndexed { index, item ->
                val statusMarker = when {
                    item.isPurchased -> "[CUMPĂRAT]"
                    item.isInCart -> "[ÎN COȘ]   "
                    else -> "[DE CĂUTAT]"
                }

                val unitPrice = item.actualUnitPrice ?: item.estimatedUnitPrice
                val lineTotal = (unitPrice ?: 0.0) * item.quantity

                sb.append(String.format(Locale.US, "%02d. %s %s\n", index + 1, statusMarker, item.productNameSnapshot))
                sb.append("    Cantitate: ${item.quantity} ${item.unit.symbolRo}")
                if (item.packageCount != null) {
                    sb.append(" (${item.packageCount} ambalaje/buc)")
                }
                if (unitPrice != null) {
                    sb.append(" • Preț unitar: ${String.format(Locale.US, "%.2f", unitPrice)} ${list.currency}")
                    sb.append(" • Total: ${String.format(Locale.US, "%.2f", lineTotal)} ${list.currency}")
                }
                sb.append("\n")

                if (!item.category.isNullOrBlank()) {
                    sb.append("    Categorie: ${item.category}\n")
                }
                if (!item.storeName.isNullOrBlank()) {
                    sb.append("    Magazin/Raion: ${item.storeName}\n")
                }
                if (!item.notes.isNullOrBlank()) {
                    sb.append("    Note: ${item.notes}\n")
                }
                sb.append("\n")
            }
        }

        sb.append("=================================================================\n")
        sb.append("Folder dedicat telefon: ${folder.absolutePath}\n")
        sb.append("Generat automat de aplicația Smart-list-Calculator (Android 2026)\n")
        sb.append("=================================================================\n")

        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }

    /**
     * Salvează indexul general cu toate listele existente pe telefon.
     */
    fun exportAllListsIndexToTxt(
        context: Context,
        lists: List<ShoppingList>
    ): File {
        val folder = getDedicatedFolder(context)
        val file = File(folder, "Index_Toate_Listele_SmartListCalculator.txt")
        val displayDateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ro", "RO"))

        val sb = StringBuilder()
        sb.append("=================================================================\n")
        sb.append("          INDEX GENERAL LISTE • SMART-LIST-CALCULATOR            \n")
        sb.append("=================================================================\n")
        sb.append("Actualizat la: ${displayDateFormat.format(Date())}\n")
        sb.append("Număr total liste salvate: ${lists.size}\n\n")

        lists.forEachIndexed { index, list ->
            sb.append("${index + 1}. ${list.name}\n")
            if (!list.storeName.isNullOrBlank()) {
                sb.append("   Magazin: ${list.storeName}\n")
            }
            sb.append("   Produse: ${list.purchasedItemsCount}/${list.totalItemsCount} (${list.completionPercentage}%)\n")
            sb.append("   Total: ${String.format(Locale.US, "%.2f", list.estimatedTotal)} ${list.currency}\n")
            sb.append("   Status: ${list.status.name}\n\n")
        }

        sb.append("Folder stocare: ${folder.absolutePath}\n")
        file.writeText(sb.toString(), Charsets.UTF_8)
        return file
    }
}
