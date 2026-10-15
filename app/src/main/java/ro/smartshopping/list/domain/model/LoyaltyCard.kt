package ro.smartshopping.list.domain.model

enum class BarcodeType(val displayName: String) {
    CODE_128("Cod de Bare (Code 128)"),
    EAN_13("Cod EAN-13"),
    QR_CODE("Cod QR"),
    EAN_8("Cod EAN-8"),
    CODE_39("Cod 39")
}

data class LoyaltyCard(
    val id: String,
    val storeName: String,
    val programName: String,
    val cardNumber: String,
    val barcodeType: BarcodeType = BarcodeType.CODE_128,
    val primaryColorHex: String = "#E11A22",
    val secondaryColorHex: String = "#8B0000",
    val cardHolderName: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastScannedAt: Long? = null
) {
    val maskedNumber: String
        get() {
            val clean = cardNumber.replace(" ", "")
            return if (clean.length > 4) {
                val visibleEnd = clean.takeLast(4)
                val maskedPart = "•".repeat(clean.length - 4)
                "$maskedPart $visibleEnd"
            } else {
                clean
            }
        }
}
