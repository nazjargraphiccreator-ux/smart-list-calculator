package ro.smartshopping.list.domain.model

data class Product(
    val id: String,
    val ownerId: String? = null,
    val name: String,
    val category: String = "Alimente",
    val brand: String? = null,
    val defaultUnit: UnitOfMeasure = UnitOfMeasure.PCS,
    val barcode: String? = null,
    val lastKnownPrice: Double? = null,
    val currency: String = "RON",
    val createdAt: Long = System.currentTimeMillis()
)
