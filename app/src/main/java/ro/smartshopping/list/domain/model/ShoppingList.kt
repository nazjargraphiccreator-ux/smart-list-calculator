package ro.smartshopping.list.domain.model

data class ShoppingList(
    val id: String,
    val ownerId: String? = null,
    val name: String,
    val status: ShoppingListStatus = ShoppingListStatus.ACTIVE,
    val currency: String = "RON",
    val shoppingDate: Long? = null,
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    // Store information
    val storeName: String? = null,
    val storeAddress: String? = null,
    val storeLatitude: Double? = null,
    val storeLongitude: Double? = null,
    // Pre-checkout budget & receipt reconciliation
    val budget: Double? = null,
    val receiptTotal: Double? = null,
    val discountAmount: Double? = null,
    val extraFeesAmount: Double? = null,
    // Aggregated / computed metrics
    val totalItemsCount: Int = 0,
    val toBuyItemsCount: Int = 0,
    val inCartItemsCount: Int = 0,
    val purchasedItemsCount: Int = 0,
    val completionPercentage: Int = 0,
    val estimatedTotal: Double = 0.0,
    val actualTotal: Double = 0.0
)

data class FinancialSummary(
    val estimatedTotal: Double,
    val actualSpent: Double,
    val remainingEstimated: Double,
    val variance: Double,
    val purchasedItemsCount: Int,
    val remainingItemsCount: Int,
    val totalItemsCount: Int,
    val completionRate: Int,
    val currency: String = "RON",
    // 3-Stage in-store metrics & pre-payment calculation
    val toBuyItemsCount: Int = 0,
    val inCartItemsCount: Int = 0,
    val paidItemsCount: Int = purchasedItemsCount,
    val inCartTotal: Double = 0.0,
    val remainingToBuyEstimated: Double = remainingEstimated,
    val budget: Double? = null,
    val remainingBudget: Double? = null,
    val isOverBudget: Boolean = false,
    val overBudgetAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val extraFeesAmount: Double = 0.0,
    val netEstimatedTotal: Double = estimatedTotal,
    val receiptTotal: Double? = null,
    val receiptVariance: Double? = null
)

