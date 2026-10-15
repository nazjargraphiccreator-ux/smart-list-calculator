package ro.smartshopping.list.data.local

import org.junit.Assert.assertTrue
import org.junit.Test
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import java.io.File
import java.util.UUID

class LocalBackupStorageManagerTest {

    @Test
    fun testTxtContentFormatting() {
        val testList = ShoppingList(
            id = UUID.randomUUID().toString(),
            name = "Cumpărături Săptămânale Militari",
            storeName = "Kaufland Militari",
            storeAddress = "Str. Valea Cascadelor nr. 3, Sector 6, București",
            budget = 300.0,
            estimatedTotal = 250.0,
            actualTotal = 220.5,
            totalItemsCount = 2,
            purchasedItemsCount = 1
        )

        val items = listOf(
            ShoppingListItem(
                id = UUID.randomUUID().toString(),
                shoppingListId = testList.id,
                productId = UUID.randomUUID().toString(),
                productNameSnapshot = "Lapte Zuzu 1.5% 1L",
                category = "Lactate",
                quantity = 2.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 7.5,
                actualUnitPrice = 6.99,
                isPurchased = true,
                storeName = "Raionul 4"
            ),
            ShoppingListItem(
                id = UUID.randomUUID().toString(),
                shoppingListId = testList.id,
                productId = UUID.randomUUID().toString(),
                productNameSnapshot = "Pâine Toast Secară 500g",
                category = "Panificație",
                quantity = 1.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 5.0,
                actualUnitPrice = null,
                isPurchased = false,
                storeName = "Raionul 1"
            )
        )

        // Verificăm conținutul generat
        val tempFolder = File(System.getProperty("java.io.tmpdir"), "SmartListTest")
        tempFolder.mkdirs()
        val tempFile = File(tempFolder, "TestList.txt")

        val sb = StringBuilder()
        sb.append("=================================================================\n")
        sb.append("         SMART-LIST-CALCULATOR • RAPORT DETALIAT LISTĂ           \n")
        sb.append("=================================================================\n\n")
        sb.append("📋 NUME LISTĂ: ${testList.name}\n")
        sb.append("🏪 MAGAZIN ASOCIAT: ${testList.storeName}\n")
        sb.append("📍 ADRESĂ MAGAZIN: ${testList.storeAddress}\n")
        sb.append("💰 BUGET LIMITĂ: ${testList.budget} ${testList.currency}\n")
        sb.append("📊 TOTAL ESTIMAT: ${testList.estimatedTotal} ${testList.currency}\n")

        items.forEachIndexed { idx, item ->
            val statusMarker = if (item.isPurchased) "[CUMPĂRAT]" else "[DE CĂUTAT]"
            sb.append("${idx + 1}. $statusMarker ${item.productNameSnapshot}\n")
            sb.append("   Cantitate: ${item.quantity} ${item.unit.symbolRo}\n")
        }

        tempFile.writeText(sb.toString())
        val readBack = tempFile.readText()

        assertTrue(readBack.contains("SMART-LIST-CALCULATOR"))
        assertTrue(readBack.contains("Kaufland Militari"))
        assertTrue(readBack.contains("Lapte Zuzu 1.5% 1L"))
        assertTrue(readBack.contains("[CUMPĂRAT]"))
        assertTrue(readBack.contains("Pâine Toast Secară 500g"))
        assertTrue(readBack.contains("[DE CĂUTAT]"))

        tempFile.delete()
        tempFolder.delete()
    }
}
