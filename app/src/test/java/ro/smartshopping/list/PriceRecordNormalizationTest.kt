package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Test
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.usecase.NormalizePriceUseCase

class PriceRecordNormalizationTest {

    private val normalizer = NormalizePriceUseCase()

    @Test
    fun `grams correctly normalize to price per kilogram`() {
        // e.g. 0.03 RON per gram -> 30.00 RON per KG
        val (normPrice, baseUnit) = normalizer(0.03, UnitOfMeasure.G)
        assertEquals(30.00, normPrice, 0.001)
        assertEquals(UnitOfMeasure.KG, baseUnit)
    }

    @Test
    fun `milliliters correctly normalize to price per liter`() {
        // e.g. 0.008 RON per ml -> 8.00 RON per Liter
        val (normPrice, baseUnit) = normalizer(0.008, UnitOfMeasure.ML)
        assertEquals(8.00, normPrice, 0.001)
        assertEquals(UnitOfMeasure.L, baseUnit)
    }

    @Test
    fun `pieces stay per piece without modification`() {
        val (normPrice, baseUnit) = normalizer(12.50, UnitOfMeasure.PCS)
        assertEquals(12.50, normPrice, 0.001)
        assertEquals(UnitOfMeasure.PCS, baseUnit)
    }
}
