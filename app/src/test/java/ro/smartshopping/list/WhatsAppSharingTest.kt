package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.smartshopping.list.domain.model.ItemPurchaseStage
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.util.ProductEmojiHelper
import ro.smartshopping.list.util.WhatsAppShareHelper
import java.util.UUID

class WhatsAppSharingTest {

    @Test
    fun testProductEmojiHelperRomanianProducts() {
        assertEquals("🥛", ProductEmojiHelper.getEmojiForProduct("Lapte Zuzu 3.5%", "Lactate"))
        assertEquals("🧀", ProductEmojiHelper.getEmojiForProduct("Cașcaval Rucăr Napolact", "Lactate"))
        assertEquals("🥚", ProductEmojiHelper.getEmojiForProduct("Ouă proaspete Toneli", "Lactate & Ouă"))
        assertEquals("🥖", ProductEmojiHelper.getEmojiForProduct("Pâine Toast Integrală", "Panificație"))
        assertEquals("🍗", ProductEmojiHelper.getEmojiForProduct("Piept de pui dezosat Fragedo", "Carne"))
        assertEquals("🥩", ProductEmojiHelper.getEmojiForProduct("Cotlet de porc fără os", "Carne"))
        assertEquals("🥔", ProductEmojiHelper.getEmojiForProduct("Cartofi albi românești", "Legume"))
        assertEquals("🍌", ProductEmojiHelper.getEmojiForProduct("Banane calitatea I", "Fructe"))
        assertEquals("☕", ProductEmojiHelper.getEmojiForProduct("Cafea boabe Lavazza Crema", "Băuturi"))
        assertEquals("💧", ProductEmojiHelper.getEmojiForProduct("Apă plată Borsec 2L", "Băuturi"))
        assertEquals("🫧", ProductEmojiHelper.getEmojiForProduct("Detergent capsule Ariel Pods", "Curățenie"))
    }

    @Test
    fun testCleanPhoneNumberRomanianFormats() {
        // Standard Romanian mobile numbers
        assertEquals("40722123456", WhatsAppShareHelper.cleanPhoneNumber("0722123456"))
        assertEquals("40722123456", WhatsAppShareHelper.cleanPhoneNumber("0722 123 456"))
        assertEquals("40722123456", WhatsAppShareHelper.cleanPhoneNumber("+40 722 123 456"))
        assertEquals("40722123456", WhatsAppShareHelper.cleanPhoneNumber("+40722123456"))
        assertEquals("40722123456", WhatsAppShareHelper.cleanPhoneNumber("0040722123456"))
        assertEquals("40722123456", WhatsAppShareHelper.cleanPhoneNumber("40722123456"))

        // Validation
        assertTrue(WhatsAppShareHelper.isValidPhoneNumber("0722123456"))
        assertTrue(WhatsAppShareHelper.isValidPhoneNumber("+40744112233"))
        assertFalse(WhatsAppShareHelper.isValidPhoneNumber("123"))
    }

    @Test
    fun testBuildWhatsAppUrlDirectToSpecificNumber() {
        val phone = "0722123456"
        val message = "Lista mea de cumparaturi"
        val url = WhatsAppShareHelper.buildWhatsAppUrl(phone, message)

        assertTrue(url.startsWith("https://api.whatsapp.com/send?phone=40722123456"))
        assertTrue(url.contains("text=Lista%20mea%20de%20cumparaturi"))
    }

    @Test
    fun testFormatRemainingListOnlyIncludesUnpurchasedItemsWithEmojis() {
        val list = ShoppingList(
            id = "list_1",
            name = "Cumpărături Săptămânale",
            storeName = "Kaufland Militari Residence",
            storeAddress = "Str. Rezervelor 64, Chiajna",
            currency = "RON",
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )

        val item1 = ShoppingListItem(
            id = UUID.randomUUID().toString(),
            shoppingListId = list.id,
            productId = UUID.randomUUID().toString(),
            productNameSnapshot = "Lapte Zuzu 3.5%",
            quantity = 2.0,
            unit = UnitOfMeasure.L,
            estimatedUnitPrice = 6.29,
            actualUnitPrice = 6.29,
            isPurchased = true, // ALREADY PURCHASED!
            category = "Lactate"
        )

        val item2 = ShoppingListItem(
            id = UUID.randomUUID().toString(),
            shoppingListId = list.id,
            productId = UUID.randomUUID().toString(),
            productNameSnapshot = "Pâine Toast",
            quantity = 1.0,
            unit = UnitOfMeasure.PCS,
            estimatedUnitPrice = 4.50,
            actualUnitPrice = null,
            isPurchased = false, // STILL TO BUY!
            category = "Brutărie"
        )

        val item3 = ShoppingListItem(
            id = UUID.randomUUID().toString(),
            shoppingListId = list.id,
            productId = UUID.randomUUID().toString(),
            productNameSnapshot = "Piept de pui Fragedo",
            quantity = 1.5,
            unit = UnitOfMeasure.KG,
            estimatedUnitPrice = 24.0,
            actualUnitPrice = null,
            isPurchased = false, // STILL TO BUY!
            category = "Carne"
        )

        val formattedText = WhatsAppShareHelper.formatRemainingListWithIcons(
            list = list,
            items = listOf(item1, item2, item3),
            onlyRemaining = true
        )

        // Must contain list name and store context
        assertTrue(formattedText.contains("CUMPĂRĂTURI SĂPTĂMÂNALE"))
        assertTrue(formattedText.contains("Kaufland Militari Residence"))
        assertTrue(formattedText.contains("RĂMASE DE CUMPĂRAT"))

        // Must NOT contain item1 because it was already purchased!
        assertFalse(formattedText.contains("Lapte Zuzu 3.5%"))

        // Must contain item2 and item3 with icons
        assertTrue(formattedText.contains("Pâine Toast"))
        assertTrue(formattedText.contains("🥖"))
        assertTrue(formattedText.contains("Piept de pui Fragedo"))
        assertTrue(formattedText.contains("🍗") || formattedText.contains("🥩"))

        // Check summary counts
        assertTrue(formattedText.contains("Rămase de luat:* 2 produse"))
        assertTrue(formattedText.contains("Total estimat rămas"))
    }
}
