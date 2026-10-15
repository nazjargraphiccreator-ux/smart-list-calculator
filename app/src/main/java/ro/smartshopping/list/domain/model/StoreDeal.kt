package ro.smartshopping.list.domain.model

data class StoreDeal(
    val id: String,
    val storeName: String, // "Kaufland Militari" sau "Auchan Militari"
    val storeBranch: String, // "Str. Valea Cascadelor 3, Militari" sau "Centrul Comercial Militari, Bd. Iuliu Maniu 560A"
    val productName: String,
    val category: String,
    val originalPrice: Double,
    val discountPrice: Double,
    val discountPercent: Int,
    val unit: UnitOfMeasure = UnitOfMeasure.PCS,
    val validUntil: String = "Valabil săptămâna aceasta",
    val aisleLocation: String, // ex: "Raion Lactate - Raft 3", "Raion Băcănie - Raft 5"
    val badge: String = "Promoție",
    val inStock: Boolean = true
)

data class StoreDealNotification(
    val id: String,
    val storeName: String,
    val title: String,
    val message: String,
    val discountHighlight: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false
)

data class AgentMatchResult(
    val item: ShoppingListItem,
    val matchedDeal: StoreDeal?,
    val kauflandDeal: StoreDeal?,
    val auchanDeal: StoreDeal?,
    val bestStore: String?,
    val unitSavings: Double,
    val totalSavings: Double,
    val aisleLocation: String?
)

data class BasketComparisonSummary(
    val totalItemsCount: Int,
    val matchedItemsCount: Int,
    val kauflandTotal: Double,
    val kauflandSavings: Double,
    val auchanTotal: Double,
    val auchanSavings: Double,
    val bestOverallStore: String,
    val recommendedStrategy: String,
    val matches: List<AgentMatchResult>
)
