package ro.smartshopping.list.domain.model

data class ShoppingListItem(
    val id: String,
    val shoppingListId: String,
    val productId: String,
    val productNameSnapshot: String,
    val quantity: Double = 1.0,
    val unit: UnitOfMeasure = UnitOfMeasure.PCS,
    val estimatedUnitPrice: Double? = null,
    val actualUnitPrice: Double? = null,
    val isPurchased: Boolean = false,
    val category: String = "Alimente",
    val notes: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val purchasedAt: Long? = null,
    val isInCart: Boolean = false,
    val packageCount: Int? = null,
    val storeName: String? = null
) {
    /**
     * 3-Stage shopping progress state:
     * 1. TO_BUY: planned, not yet picked up
     * 2. IN_CART: placed in shopping cart in-store
     * 3. PURCHASED: paid at cashier
     */
    val purchaseStage: ItemPurchaseStage
        get() = when {
            isPurchased -> ItemPurchaseStage.PURCHASED
            isInCart -> ItemPurchaseStage.IN_CART
            else -> ItemPurchaseStage.TO_BUY
        }

    /**
     * Line total calculated dynamically:
     * - Uses actualUnitPrice if purchased & known
     * - Otherwise uses estimatedUnitPrice
     * - Returns 0.0 if neither is known
     */
    val effectiveUnitPrice: Double?
        get() = if (isPurchased && actualUnitPrice != null) actualUnitPrice else estimatedUnitPrice

    val calculatedLineTotal: Double
        get() = (effectiveUnitPrice ?: 0.0) * quantity
}
