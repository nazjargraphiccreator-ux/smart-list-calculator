package ro.smartshopping.list.domain.usecase

import ro.smartshopping.list.domain.model.ShoppingListItem
import kotlin.math.roundToInt

class CalculateCompletionRateUseCase {

    /**
     * Calculates completion percentage from a list of shopping items.
     * Formula: (Purchased / Total) * 100.
     * Guaranteed safe against division by zero (returns 0 for empty list).
     */
    operator fun invoke(items: List<ShoppingListItem>): Int {
        if (items.isEmpty()) return 0
        val purchasedCount = items.count { it.isPurchased }
        val percentage = (purchasedCount.toDouble() / items.size.toDouble()) * 100.0
        return percentage.roundToInt().coerceIn(0, 100)
    }

    fun fromCounts(purchasedCount: Int, totalCount: Int): Int {
        if (totalCount <= 0) return 0
        val percentage = (purchasedCount.toDouble() / totalCount.toDouble()) * 100.0
        return percentage.roundToInt().coerceIn(0, 100)
    }
}
