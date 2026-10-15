package ro.smartshopping.list.domain.model

data class PriceRecord(
    val id: String,
    val productId: String,
    val shoppingListId: String? = null,
    val unitPrice: Double,
    val quantity: Double = 1.0,
    val unit: UnitOfMeasure = UnitOfMeasure.PCS,
    val currency: String = "RON",
    val storeName: String? = null,
    val storeAddress: String? = null,
    val sourceType: PriceSourceType = PriceSourceType.CONFIRMED_PURCHASE,
    val recordedAt: Long = System.currentTimeMillis()
)

data class StorePriceComparison(
    val storeName: String,
    val storeAddress: String? = null,
    val latestPrice: Double,
    val lowestPrice: Double,
    val highestPrice: Double,
    val lastRecordedAt: Long,
    val currency: String = "RON"
)

data class ProductPriceHistory(
    val productId: String,
    val productName: String,
    val records: List<PriceRecord>,
    val latestPrice: Double?,
    val previousPrice: Double?,
    val lowestPrice: Double?,
    val highestPrice: Double?,
    val averagePrice: Double?,
    val priceChangeAbsolute: Double?,
    val priceChangePercentage: Double?,
    val currency: String = "RON",
    val storeComparisons: List<StorePriceComparison> = emptyList()
)
