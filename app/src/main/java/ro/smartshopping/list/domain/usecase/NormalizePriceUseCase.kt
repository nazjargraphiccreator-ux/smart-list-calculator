package ro.smartshopping.list.domain.usecase

import ro.smartshopping.list.domain.model.UnitOfMeasure

class NormalizePriceUseCase {

    /**
     * Normalizes a price to a standard base unit (per KG, per Liter, or per piece).
     * Prevents comparing 500g directly with 1kg without normalizing.
     *
     * Returns a pair of:
     * - Normalized price per base unit
     * - Standard base unit (KG, L, or PCS)
     */
    operator fun invoke(unitPrice: Double, unit: UnitOfMeasure, quantity: Double = 1.0): Pair<Double, UnitOfMeasure> {
        if (unitPrice <= 0.0) return Pair(0.0, unit)

        return when (unit) {
            UnitOfMeasure.G -> {
                // Convert grams to KG: 1000g = 1kg
                val pricePerKg = unitPrice * 1000.0
                Pair(pricePerKg, UnitOfMeasure.KG)
            }
            UnitOfMeasure.ML -> {
                // Convert ml to Liter: 1000ml = 1L
                val pricePerL = unitPrice * 1000.0
                Pair(pricePerL, UnitOfMeasure.L)
            }
            UnitOfMeasure.KG -> Pair(unitPrice, UnitOfMeasure.KG)
            UnitOfMeasure.L -> Pair(unitPrice, UnitOfMeasure.L)
            UnitOfMeasure.PACK,
            UnitOfMeasure.PCS,
            UnitOfMeasure.BOTTLE,
            UnitOfMeasure.CARTON,
            UnitOfMeasure.CAN,
            UnitOfMeasure.JAR,
            UnitOfMeasure.BAG -> Pair(unitPrice, UnitOfMeasure.PCS)
        }
    }
}
