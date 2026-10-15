package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.usecase.CarryForwardUnpurchasedUseCase

class CarryForwardUnpurchasedTest {

    private val carryForward = CarryForwardUnpurchasedUseCase()

    @Test
    fun `carry forward copies only unpurchased items and resets status`() {
        val originalListId = "list_old"
        val newListId = "list_new"

        val originalItems = listOf(
            ShoppingListItem(
                id = "item_1",
                shoppingListId = originalListId,
                productId = "prod_milk",
                productNameSnapshot = "Lapte",
                quantity = 2.0,
                unit = UnitOfMeasure.L,
                estimatedUnitPrice = 7.50,
                actualUnitPrice = 7.50,
                isPurchased = true
            ),
            ShoppingListItem(
                id = "item_2",
                shoppingListId = originalListId,
                productId = "prod_bread",
                productNameSnapshot = "Pâine",
                quantity = 3.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 4.00,
                actualUnitPrice = null,
                isPurchased = false
            ),
            ShoppingListItem(
                id = "item_3",
                shoppingListId = originalListId,
                productId = "prod_apples",
                productNameSnapshot = "Mere",
                quantity = 1.5,
                unit = UnitOfMeasure.KG,
                estimatedUnitPrice = 6.00,
                actualUnitPrice = null,
                isPurchased = false
            )
        )

        val result = carryForward(
            sourceItems = originalItems,
            targetShoppingListId = newListId
        )

        // Only bread and apples should be carried forward (2 items)
        assertEquals(2, result.size)

        // Verify Bread
        val bread = result.find { it.productId == "prod_bread" }!!
        assertEquals(newListId, bread.shoppingListId)
        assertNotEquals("item_2", bread.id) // New unique ID
        assertEquals("Pâine", bread.productNameSnapshot)
        assertEquals(3.0, bread.quantity, 0.001)
        assertEquals(UnitOfMeasure.PCS, bread.unit)
        assertEquals(4.00, bread.estimatedUnitPrice ?: 0.0, 0.001)
        assertNull(bread.actualUnitPrice)
        assertFalse(bread.isPurchased) // Reset to unpurchased

        // Verify Apples
        val apples = result.find { it.productId == "prod_apples" }!!
        assertEquals(newListId, apples.shoppingListId)
        assertEquals(1.5, apples.quantity, 0.001)
        assertEquals(UnitOfMeasure.KG, apples.unit)
        assertFalse(apples.isPurchased)

        // Verify original items were NOT mutated
        assertTrue(originalItems[0].isPurchased)
        assertEquals(originalListId, originalItems[1].shoppingListId)
    }
}
