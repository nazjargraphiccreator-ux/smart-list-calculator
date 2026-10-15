package ro.smartshopping.list.domain.usecase

import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import java.util.Locale

class FormatListForSharingUseCase(
    private val financialCalculator: CalculateFinancialSummaryUseCase = CalculateFinancialSummaryUseCase()
) {

    /**
     * Formats a shopping list into a clean, human-readable text message
     * suitable for sharing directly to WhatsApp, Telegram, SMS or Email via Android Sharesheet.
     */
    operator fun invoke(
        list: ShoppingList,
        items: List<ShoppingListItem>,
        isRomanian: Boolean = true
    ): String {
        val summary = financialCalculator(items, list.currency)
        val sb = StringBuilder()

        // Header
        val headerTitle = list.name.uppercase()
        sb.append("🛒 ").append(headerTitle).append("\n\n")

        // Items
        if (items.isEmpty()) {
            sb.append(if (isRomanian) "Lista nu conține produse momentan.\n" else "List is currently empty.\n")
        } else {
            items.forEach { item ->
                val checkmark = if (item.isPurchased) "☑ " else "☐ "
                val qtyStr = formatQuantity(item.quantity)
                val unitStr = item.unit.getSymbol(isRomanian)

                sb.append(checkmark).append(item.productNameSnapshot)
                sb.append(" — ").append(qtyStr).append(" ").append(unitStr)

                val price = item.effectiveUnitPrice
                if (price != null && price > 0.0) {
                    val lineTotal = item.calculatedLineTotal
                    sb.append(" — ")
                        .append(String.format(Locale.US, "%.2f", price))
                        .append(" ").append(list.currency).append("/").append(unitStr)
                    if (item.quantity > 1.0) {
                        sb.append(" (")
                            .append(String.format(Locale.US, "%.2f", lineTotal))
                            .append(" ").append(list.currency).append(")")
                    }
                }
                sb.append("\n")
            }
        }

        // Summary Divider & Stats
        sb.append("\n━━━━━━━━━━━━━━━━━━━━━━━━━━\n")
        if (isRomanian) {
            sb.append("📊 Progres: ${summary.purchasedItemsCount}/${summary.totalItemsCount} produse (${summary.completionRate}%)\n")
            sb.append("💰 Total estimat: ${String.format(Locale.US, "%.2f", summary.estimatedTotal)} ${list.currency}\n")
            sb.append("✅ Cheltuit: ${String.format(Locale.US, "%.2f", summary.actualSpent)} ${list.currency}\n")
            if (summary.remainingItemsCount > 0) {
                sb.append("⏳ Rămas: ${String.format(Locale.US, "%.2f", summary.remainingEstimated)} ${list.currency}\n")
            }
        } else {
            sb.append("📊 Progress: ${summary.purchasedItemsCount}/${summary.totalItemsCount} items (${summary.completionRate}%)\n")
            sb.append("💰 Estimated total: ${String.format(Locale.US, "%.2f", summary.estimatedTotal)} ${list.currency}\n")
            sb.append("✅ Purchased: ${String.format(Locale.US, "%.2f", summary.actualSpent)} ${list.currency}\n")
            if (summary.remainingItemsCount > 0) {
                sb.append("⏳ Remaining: ${String.format(Locale.US, "%.2f", summary.remainingEstimated)} ${list.currency}\n")
            }
        }

        sb.append("\nCreat cu Smart Shopping List 2026")
        return sb.toString()
    }

    private fun formatQuantity(qty: Double): String {
        return if (qty % 1.0 == 0.0) {
            qty.toInt().toString()
        } else {
            String.format(Locale.US, "%.2f", qty).trimEnd('0').trimEnd('.')
        }
    }
}
