package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.parser.SmartTextImportParser

class SmartTextImportParserTest {

    private lateinit var parser: SmartTextImportParser

    @Before
    fun setup() {
        parser = SmartTextImportParser()
    }

    @Test
    fun testParseStandardPromptShoppingList() {
        val input = """
            Lapte 2 bucăți
            Pâine 3 buc
            Ouă 1 cofraj
            Mere 2 kg
            Detergent 1 bucată
            Apă minerală 6 sticle
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals(6, result.items.size)

        val lapte = result.items[0]
        assertEquals("Lapte", lapte.name)
        assertEquals(2.0, lapte.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, lapte.unit)
        assertEquals(2, lapte.packageCount)

        val paine = result.items[1]
        assertEquals("Pâine", paine.name)
        assertEquals(3.0, paine.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, paine.unit)
        assertEquals(3, paine.packageCount)

        val oua = result.items[2]
        assertEquals("Ouă", oua.name)
        assertEquals(1.0, oua.quantity, 0.001)
        assertEquals(UnitOfMeasure.CARTON, oua.unit)
        assertEquals(1, oua.packageCount)

        val mere = result.items[3]
        assertEquals("Mere", mere.name)
        assertEquals(2.0, mere.quantity, 0.001)
        assertEquals(UnitOfMeasure.KG, mere.unit)

        val detergent = result.items[4]
        assertEquals("Detergent", detergent.name)
        assertEquals(1.0, detergent.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, detergent.unit)

        val apa = result.items[5]
        assertEquals("Apă minerală", apa.name)
        assertEquals(6.0, apa.quantity, 0.001)
        assertEquals(UnitOfMeasure.BOTTLE, apa.unit)
        assertEquals(6, apa.packageCount)
    }

    @Test
    fun testTypoAndDiacriticsCorrection() {
        val input = """
            iaurt grecesc 4 buc
            faina 1 kg
            ulei floarea soarelui 1 sticla
            piept pui 800 g
            pasta de dinti 1 buc
            rosii 1.5 kg
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals(6, result.items.size)

        assertEquals("Iaurt grecesc", result.items[0].name)
        assertEquals("Făină", result.items[1].name)
        assertEquals("Ulei de floarea-soarelui", result.items[2].name)
        assertEquals(UnitOfMeasure.BOTTLE, result.items[2].unit)

        assertEquals("Piept de pui", result.items[3].name)
        assertEquals(800.0, result.items[3].quantity, 0.001)
        assertEquals(UnitOfMeasure.G, result.items[3].unit)

        assertEquals("Pastă de dinți", result.items[4].name)
        assertEquals("Roșii", result.items[5].name)
        assertEquals(1.5, result.items[5].quantity, 0.001)
        assertEquals(UnitOfMeasure.KG, result.items[5].unit)
    }

    @Test
    fun testPriceExtractionWithVariousRomanianFormats() {
        val input = """
            Unt 1 pachet 14.50 lei
            Cafea boabe 500 g 32,90 RON
            Ciocolată 2 buc la 6.50 lei/buc
        """.trimIndent()

        val result = parser.parse(input)
        assertEquals(3, result.items.size)

        val unt = result.items[0]
        assertEquals(14.50, unt.unitPrice ?: 0.0, 0.001)
        assertEquals(14.50, unt.estimatedLineTotal, 0.001)

        val cafea = result.items[1]
        assertEquals(32.90, cafea.unitPrice ?: 0.0, 0.001)

        val ciocolata = result.items[2]
        assertEquals(2.0, ciocolata.quantity, 0.001)
        assertEquals(6.50, ciocolata.unitPrice ?: 0.0, 0.001)
        assertEquals(13.00, ciocolata.estimatedLineTotal, 0.001)
    }

    @Test
    fun testDuplicateDetectionAndMerging() {
        val input = """
            Mere 2 kg
            Pâine 1 buc
            Mere 1.5 kg
            Pâine 2 bucăți
        """.trimIndent()

        val result = parser.parse(input)
        // Check duplicates detected in raw analysis
        assertTrue(result.duplicatesCount > 0)

        val merged = parser.mergeDuplicates(result.items)
        assertEquals(2, merged.size)

        val mere = merged.find { it.name == "Mere" }
        assertNotNull(mere)
        assertEquals(3.5, mere!!.quantity, 0.001)
        assertEquals(UnitOfMeasure.KG, mere.unit)

        val paine = merged.find { it.name == "Pâine" }
        assertNotNull(paine)
        assertEquals(3.0, paine!!.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, paine.unit)
        assertEquals(3, paine.packageCount)
    }
}
