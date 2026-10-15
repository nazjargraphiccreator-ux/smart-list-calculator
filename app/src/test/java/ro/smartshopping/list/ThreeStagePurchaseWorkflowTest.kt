package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.smartshopping.list.domain.model.ItemPurchaseStage
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import java.util.UUID

class ThreeStagePurchaseWorkflowTest {

    @Test
    fun testThreeStageTransitions() {
        val initialItem = ShoppingListItem(
            id = UUID.randomUUID().toString(),
            shoppingListId = "list_test",
            productId = "prod_1",
            productNameSnapshot = "Pâine",
            quantity = 2.0,
            unit = UnitOfMeasure.PCS,
            packageCount = 2,
            estimatedUnitPrice = 4.5,
            actualUnitPrice = null,
            isPurchased = false,
            isInCart = false
        )

        // Stage 1: TO_BUY
        assertEquals(ItemPurchaseStage.TO_BUY, initialItem.purchaseStage)
        assertFalse(initialItem.isInCart)
        assertFalse(initialItem.isPurchased)

        // Stage 2: Move to IN_CART
        val inCartItem = initialItem.copy(isInCart = true, isPurchased = false)
        assertEquals(ItemPurchaseStage.IN_CART, inCartItem.purchaseStage)
        assertTrue(inCartItem.isInCart)
        assertFalse(inCartItem.isPurchased)

        // Stage 3: Move to PURCHASED
        val purchasedItem = inCartItem.copy(isPurchased = true, actualUnitPrice = 4.5)
        assertEquals(ItemPurchaseStage.PURCHASED, purchasedItem.purchaseStage)
        assertTrue(purchasedItem.isPurchased)

        // Rollback / Undo to IN_CART
        val rolledBackItem = purchasedItem.copy(isPurchased = false, isInCart = true)
        assertEquals(ItemPurchaseStage.IN_CART, rolledBackItem.purchaseStage)

        // Rollback / Undo to TO_BUY
        val fullyResetItem = rolledBackItem.copy(isInCart = false)
        assertEquals(ItemPurchaseStage.TO_BUY, fullyResetItem.purchaseStage)
    }

    @Test
    fun testQuantityAndPackageSteppers() {
        var item = ShoppingListItem(
            id = UUID.randomUUID().toString(),
            shoppingListId = "list_test",
            productId = "prod_2",
            productNameSnapshot = "Lapte",
            quantity = 1.0,
            unit = UnitOfMeasure.PCS,
            packageCount = 1,
            estimatedUnitPrice = 7.0,
            isPurchased = false,
            isInCart = false
        )

        // Increment quantity and package count (+ button)
        val step = 1.0
        item = item.copy(
            quantity = item.quantity + step,
            packageCount = (item.packageCount ?: 1) + 1
        )
        assertEquals(2.0, item.quantity, 0.001)
        assertEquals(2, item.packageCount)

        // Increment again
        item = item.copy(
            quantity = item.quantity + step,
            packageCount = (item.packageCount ?: 1) + 1
        )
        assertEquals(3.0, item.quantity, 0.001)
        assertEquals(3, item.packageCount)

        // Decrement quantity and package count (- button)
        item = item.copy(
            quantity = (item.quantity - step).coerceAtLeast(0.01),
            packageCount = ((item.packageCount ?: 1) - 1).coerceAtLeast(1)
        )
        assertEquals(2.0, item.quantity, 0.001)
        assertEquals(2, item.packageCount)
    }
}
