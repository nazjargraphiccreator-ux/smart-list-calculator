package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Test
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.usecase.CalculateFinancialSummaryUseCase

class CalculateFinancialSummaryTest {

    private val calculator = CalculateFinancialSummaryUseCase()

    @Test
    fun `master prompt example calculation - milk bread apples matches 39_00 RON`() {
        val items = listOf(
            ShoppingListItem(
                id = "1",
                shoppingListId = "list_1",
                productId = "milk",
                productNameSnapshot = "Lapte",
                quantity = 2.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 7.50,
                actualUnitPrice = 7.50,
                isPurchased = true
            ),
            ShoppingListItem(
                id = "2",
                shoppingListId = "list_1",
                productId = "bread",
                productNameSnapshot = "Pâine",
                quantity = 3.0,
                unit = UnitOfMeasure.PCS,
                estimatedUnitPrice = 4.00,
                actualUnitPrice = null,
                isPurchased = false
            ),
            ShoppingListItem(
                id = "3",
                shoppingListId = "list_1",
                productId = "apples",
                productNameSnapshot = "Mere",
                quantity = 2.0,
                unit = UnitOfMeasure.KG,
                estimatedUnitPrice = 6.00,
                actualUnitPrice = null,
                isPurchased = false
            )
        )

        val summary = calculator(items, "RON")

        assertEquals(39.00, summary.estimatedTotal, 0.001)
        assertEquals(15.00, summary.actualSpent, 0.001)
        assertEquals(24.00, summary.remainingEstimated, 0.001)
        assertEquals(1, summary.purchasedItemsCount)
        assertEquals(2, summary.remainingItemsCount)
        assertEquals(3, summary.totalItemsCount)
        assertEquals(33, summary.completionRate)
    }

    @Test
    fun `decimal arithmetic precision test with fractional quantities`() {
        val items = listOf(
            ShoppingListItem(
                id = "1",
                shoppingListId = "l",
                productId = "cheese",
                productNameSnapshot = "Brânză",
                quantity = 0.35, // 350 grams
                unit = UnitOfMeasure.KG,
                estimatedUnitPrice = 45.99,
                isPurchased = false
            )
        )

        val summary = calculator(items, "RON")
        // 0.35 * 45.99 = 16.0965 -> rounded half up = 16.10
        assertEquals(16.10, summary.estimatedTotal, 0.001)
    }
}
