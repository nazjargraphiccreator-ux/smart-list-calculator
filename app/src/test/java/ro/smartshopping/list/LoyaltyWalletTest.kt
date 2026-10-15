package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.smartshopping.list.domain.model.BarcodeType
import ro.smartshopping.list.domain.model.LoyaltyCard
import ro.smartshopping.list.util.BarcodeHelper
import java.util.UUID

class LoyaltyWalletTest {

    @Test
    fun testLoyaltyCardMasking() {
        val card = LoyaltyCard(
            id = UUID.randomUUID().toString(),
            storeName = "Kaufland Militari",
            programName = "Kaufland Card",
            cardNumber = "2800192837461",
            barcodeType = BarcodeType.CODE_128,
            primaryColorHex = "#E11A22",
            secondaryColorHex = "#7A0006",
            cardHolderName = "Ion Popescu",
            isFavorite = true
        )

        assertEquals("Kaufland Militari", card.storeName)
        assertEquals("Kaufland Card", card.programName)
        assertTrue(card.isFavorite)

        // Verificăm numărul mascat
        val masked = card.maskedNumber
        assertTrue(masked.endsWith("7461"))
        assertTrue(masked.contains("•"))
    }

    @Test
    fun testBarcodeGenerationCode128() {
        val content = "2800192837461"
        val bitMatrix = BarcodeHelper.generateBitMatrix(
            content = content,
            barcodeType = BarcodeType.CODE_128,
            width = 400,
            height = 150
        )

        assertNotNull("BitMatrix generat pentru Code 128 nu trebuie să fie null", bitMatrix)
        assertTrue((bitMatrix?.width ?: 0) >= 400)
        assertTrue((bitMatrix?.height ?: 0) >= 150)
    }

    @Test
    fun testBarcodeGenerationQrCode() {
        val qrContent = "LIDL-PLUS-RO-123456"
        val bitMatrix = BarcodeHelper.generateBitMatrix(
            content = qrContent,
            barcodeType = BarcodeType.QR_CODE,
            width = 300,
            height = 300
        )

        assertNotNull("BitMatrix generat pentru QR Code nu trebuie să fie null", bitMatrix)
        assertTrue((bitMatrix?.width ?: 0) >= 300)
        assertTrue((bitMatrix?.height ?: 0) >= 300)
    }
}
