package ro.smartshopping.list.domain.model

enum class UnitOfMeasure(val symbolRo: String, val symbolEn: String) {
    PCS("buc", "pcs"),
    KG("kg", "kg"),
    G("g", "g"),
    L("L", "L"),
    ML("ml", "ml"),
    PACK("pachet", "pack"),
    BOTTLE("sticlă", "btl"),
    CARTON("cofraj", "carton"),
    CAN("doză", "can"),
    JAR("borcan", "jar"),
    BAG("pungă", "bag");

    fun getSymbol(isRo: Boolean = true): String = if (isRo) symbolRo else symbolEn

    companion object {
        fun fromString(value: String): UnitOfMeasure {
            return when (value.trim().lowercase()) {
                "kg", "kile", "kilogram", "kilograme" -> KG
                "g", "gr", "gram", "grame" -> G
                "l", "litru", "litri", "liter", "litres" -> L
                "ml", "mililitru", "mililitri", "cl" -> ML
                "pack", "pachet", "pachete" -> PACK
                "sticlă", "sticla", "sticle", "btl", "bottle", "bottles" -> BOTTLE
                "cofraj", "cofraje", "cutie", "cutii", "carton", "box" -> CARTON
                "doză", "doza", "doze", "conservă", "conserva", "conserve", "can" -> CAN
                "borcan", "borcane", "jar" -> JAR
                "pungă", "punga", "pungi", "plic", "plicuri", "bag" -> BAG
                "buc", "bucată", "bucata", "bucăți", "bucati", "bca", "pcs", "piece", "pieces" -> PCS
                else -> PCS
            }
        }
    }
}

enum class ItemPurchaseStage(val labelRo: String, val labelEn: String) {
    TO_BUY("De adăugat în coș", "To Buy"),
    IN_CART("Adăugat în coș", "In Cart"),
    PURCHASED("Plătit și cumpărat", "Purchased")
}

enum class ShoppingListStatus {
    ACTIVE,
    COMPLETED,
    ARCHIVED
}

enum class PriceSourceType {
    ESTIMATED,
    CONFIRMED_PURCHASE,
    MANUAL_OVERRIDE
}

