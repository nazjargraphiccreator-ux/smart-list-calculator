package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.parser.SmartTextImportParser

class SmartMultiStoreImportTest {

    private lateinit var parser: SmartTextImportParser

    @Before
    fun setup() {
        parser = SmartTextImportParser()
    }

    @Test
    fun testUserExactShoppingListPromptParsing() {
        val userPromptInput = """
Lista cumpărături 

Piață : 

1. Cartofi 10 kg
2. Fasole 2kg
3. Branza capra 


Metro:

1. Săpun - 6 buc
2. Bulion sultan - 4 buc
3. Samntana ( galetusa ) - 1 buc 
4. Salam săsesc- 1 buc 
5. Orez - 1 buc  
6. Scărița afumata 3kg 


Auchan : 

1. Cofrag - 30 oua 


Kaufland:

1. File de pui cu susan - 6 
2. Lapte dulce - 2 
3. Cascaval 
4. Iaurt cremosso - 4 buc ( profi )
        """.trimIndent()

        val result = parser.parse(userPromptInput)

        // 1. Verify general parsing result
        assertEquals(14, result.items.size)
        assertEquals("Lista cumpărături", result.suggestedTitle)
        assertEquals(4, result.storesDetected.size)
        assertTrue(result.storesDetected.contains("Piață"))
        assertTrue(result.storesDetected.contains("Metro"))
        assertTrue(result.storesDetected.contains("Auchan"))
        assertTrue(result.storesDetected.contains("Kaufland"))

        // Verify missing space corrections count: at least Fasole 2kg, Salam săsesc- 1, Scărița afumata 3kg
        assertTrue("Expected space corrections >= 3", result.spaceCorrectionsCount >= 3)

        // 2. Verify Piață Store (3 items)
        val piataItems = result.itemsByStore["Piață"]
        assertNotNull(piataItems)
        assertEquals(3, piataItems!!.size)

        val cartofi = piataItems[0]
        assertEquals("Cartofi", cartofi.name)
        assertEquals(10.0, cartofi.quantity, 0.001)
        assertEquals(UnitOfMeasure.KG, cartofi.unit)
        assertEquals("Piață", cartofi.storeName)

        val fasole = piataItems[1]
        assertEquals("Fasole", fasole.name)
        assertEquals(2.0, fasole.quantity, 0.001)
        assertEquals(UnitOfMeasure.KG, fasole.unit)
        assertEquals("Piață", fasole.storeName)
        assertNotNull(fasole.spaceCorrectionNote)
        assertTrue(fasole.spaceCorrectionNote!!.contains("2kg ➔ 2 kg"))

        val branza = piataItems[2]
        assertEquals("Brânză de capră", branza.name)
        assertEquals(1.0, branza.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, branza.unit)
        assertEquals("Piață", branza.storeName)

        // 3. Verify Metro Store (6 items)
        val metroItems = result.itemsByStore["Metro"]
        assertNotNull(metroItems)
        assertEquals(6, metroItems!!.size)

        val sapun = metroItems[0]
        assertEquals("Săpun", sapun.name)
        assertEquals(6.0, sapun.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, sapun.unit)
        assertEquals("Metro", sapun.storeName)

        val bulion = metroItems[1]
        assertEquals("Bulion Sultan", bulion.name)
        assertEquals(4.0, bulion.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, bulion.unit)
        assertEquals("Metro", bulion.storeName)

        val smantana = metroItems[2]
        assertEquals("Smântână", smantana.name)
        assertEquals(1.0, smantana.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, smantana.unit)
        assertEquals("Metro", smantana.storeName)
        assertEquals("Găletușă", smantana.notes)

        val salam = metroItems[3]
        assertEquals("Salam Săsesc", salam.name)
        assertEquals(1.0, salam.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, salam.unit)
        assertEquals("Metro", salam.storeName)
        assertNotNull(salam.spaceCorrectionNote)

        val orez = metroItems[4]
        assertEquals("Orez", orez.name)
        assertEquals(1.0, orez.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, orez.unit)
        assertEquals("Metro", orez.storeName)

        val scarita = metroItems[5]
        assertEquals("Scăriță afumată", scarita.name)
        assertEquals(3.0, scarita.quantity, 0.001)
        assertEquals(UnitOfMeasure.KG, scarita.unit)
        assertEquals("Metro", scarita.storeName)
        assertNotNull(scarita.spaceCorrectionNote)
        assertTrue(scarita.spaceCorrectionNote!!.contains("3kg ➔ 3 kg"))

        // 4. Verify Auchan Store (1 item: Ouă)
        val auchanItems = result.itemsByStore["Auchan"]
        assertNotNull(auchanItems)
        assertEquals(1, auchanItems!!.size)

        val oua = auchanItems[0]
        assertEquals("Ouă", oua.name)
        assertEquals(30.0, oua.quantity, 0.001)
        assertEquals("Auchan", oua.storeName)
        assertNotNull(oua.notes)
        assertTrue(oua.notes!!.contains("Cofraj"))

        // 5. Verify Kaufland Store (4 items)
        val kauflandItems = result.itemsByStore["Kaufland"]
        assertNotNull(kauflandItems)
        assertEquals(4, kauflandItems!!.size)

        val pui = kauflandItems[0]
        assertEquals("File de pui cu susan", pui.name)
        assertEquals(6.0, pui.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, pui.unit)
        assertEquals("Kaufland", pui.storeName)

        val lapte = kauflandItems[1]
        assertEquals("Lapte dulce", lapte.name)
        assertEquals(2.0, lapte.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, lapte.unit)
        assertEquals("Kaufland", lapte.storeName)

        val cascaval = kauflandItems[2]
        assertEquals("Cașcaval", cascaval.name)
        assertEquals(1.0, cascaval.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, cascaval.unit)
        assertEquals("Kaufland", cascaval.storeName)

        val iaurt = kauflandItems[3]
        assertEquals("Iaurt Cremosso", iaurt.name)
        assertEquals(4.0, iaurt.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, iaurt.unit)
        assertEquals("Kaufland", iaurt.storeName)
        assertEquals("profi", iaurt.notes)
    }

    @Test
    fun testNormalizeMissingSpacesSpecificRules() {
        val (res1, note1) = parser.normalizeMissingSpaces("Fasole 2kg")
        assertEquals("Fasole 2 kg", res1)
        assertNotNull(note1)

        val (res2, note2) = parser.normalizeMissingSpaces("Scărița afumata 3kg")
        assertEquals("Scărița afumata 3 kg", res2)
        assertNotNull(note2)

        val (res3, note3) = parser.normalizeMissingSpaces("Salam săsesc- 1 buc")
        assertEquals("Salam săsesc - 1 buc", res3)
        assertNotNull(note3)

        val (res4, note4) = parser.normalizeMissingSpaces("Detergent-1 buc")
        assertEquals("Detergent - 1 buc", res4)
        assertNotNull(note4)
    }
}
