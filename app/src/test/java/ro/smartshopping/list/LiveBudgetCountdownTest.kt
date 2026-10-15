package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.usecase.CalculateFinancialSummaryUseCase

class LiveBudgetCountdownTest {

    private val calculator = CalculateFinancialSummaryUseCase()

    @Test
    fun `budget is deducted in real-time as items are added to cart`() {
        val items = listOf(
            ShoppingListItem(
                id = "1",
                shoppingListId = "list_budget",
                productId = "meat",
                productNameSnapshot = "Ceafă de porc",
                quantity = 1.5,
                unit = UnitOfMeasure.KG,
                estimatedUnitPrice = 30.00,
                isInCart = true,
                isPurchased = false
            ),
            ShoppingListItem(
                id = "2",
                shoppingListId = "list_budget",
                productId = "water",
                productNameSnapshot = "Apă plată 5L",
                quantity = 2.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 7.50,
                isInCart = false,
                isPurchased = false
            )
        )

        val budget = 100.00
        val summary = calculator(items = items, currency = "RON", budget = budget)

        // Ceafă de porc: 1.5 * 30.0 = 45.00 in cart
        assertEquals(45.00, summary.inCartTotal, 0.001)
        // Budget deduction: 100.00 - 45.00 = 55.00
        assertEquals(55.00, summary.remainingBudget ?: 0.0, 0.001)
        assertFalse(summary.isOverBudget)
        assertEquals(0.0, summary.overBudgetAmount, 0.001)
    }

    @Test
    fun `budget exceeded alert triggers before payment when in-cart total surpasses budget`() {
        val items = listOf(
            ShoppingListItem(
                id = "1",
                shoppingListId = "list_budget",
                productId = "cheese",
                productNameSnapshot = "Cascaval",
                quantity = 2.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 35.00,
                isInCart = true,
                isPurchased = false
            ),
            ShoppingListItem(
                id = "2",
                shoppingListId = "list_budget",
                productId = "coffee",
                productNameSnapshot = "Cafea boabe",
                quantity = 1.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 60.00,
                isInCart = true,
                isPurchased = false
            )
        )

        // Total in cart: 2 * 35 (70) + 60 = 130 RON
        val budget = 100.00
        val summary = calculator(items = items, currency = "RON", budget = budget)

        assertEquals(130.00, summary.inCartTotal, 0.001)
        assertEquals(-30.00, summary.remainingBudget ?: 0.0, 0.001)
        assertTrue("Budget must be flagged as exceeded before payment", summary.isOverBudget)
        assertEquals(30.00, summary.overBudgetAmount, 0.001)
    }

    @Test
    fun `budget tracks combined in-cart and purchased items at cashier`() {
        val items = listOf(
            ShoppingListItem(
                id = "1",
                shoppingListId = "list_budget",
                productId = "oil",
                productNameSnapshot = "Ulei floarea soarelui",
                quantity = 2.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 8.00,
                actualUnitPrice = 7.50,
                isInCart = true,
                isPurchased = true // Paid at cashier
            ),
            ShoppingListItem(
                id = "2",
                shoppingListId = "list_budget",
                productId = "sugar",
                productNameSnapshot = "Zahăr 1kg",
                quantity = 4.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 5.00,
                actualUnitPrice = 5.00,
                isInCart = true,
                isPurchased = false // Still in cart
            )
        )

        // Paid: 2 * 7.50 = 15.00 RON
        // In Cart: 4 * 5.00 = 20.00 RON
        // Total commitment: 35.00 RON
        val budget = 50.00
        val summary = calculator(items = items, currency = "RON", budget = budget)

        assertEquals(15.00, summary.actualSpent, 0.001)
        assertEquals(20.00, summary.inCartTotal, 0.001)
        assertEquals(15.00, summary.remainingBudget ?: 0.0, 0.001)
        assertFalse(summary.isOverBudget)
    }
}
