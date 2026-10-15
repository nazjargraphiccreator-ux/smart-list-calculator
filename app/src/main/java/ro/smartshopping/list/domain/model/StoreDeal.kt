package ro.smartshopping.list.domain.model

data class StoreDeal(
    val id: String,
    val storeName: String, // "Kaufland Militari Residence", "Auchan Militari", "Lidl Militari Residence", "Penny Chiajna", "Metro Militari"
    val storeBranch: String, // Adresă fizică exactă în Chiajna / Militari
    val productName: String,
    val category: String,
    val originalPrice: Double,
    val discountPrice: Double,
    val discountPercent: Int,
    val unit: UnitOfMeasure = UnitOfMeasure.PCS,
    val validUntil: String = "Valabil săptămâna aceasta",
    val aisleLocation: String, // Locație exactă la raft / culoar
    val badge: String = "Promoție",
    val inStock: Boolean = true,
    val officialCatalogUrl: String? = null,
    val officialAppName: String? = null,
    val proximityText: String = "📍 Lângă Militari Residence / Chiajna",
    val googleMapsUrl: String? = null
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
