package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.usecase.CalculateFinancialSummaryUseCase
import java.util.UUID

class PrePaymentCalculatorTest {

    private lateinit var calculator: CalculateFinancialSummaryUseCase

    @Before
    fun setup() {
        calculator = CalculateFinancialSummaryUseCase()
    }

    private fun createItem(
        name: String,
        qty: Double,
        estimatedPrice: Double?,
        actualPrice: Double?,
        isPurchased: Boolean,
        isInCart: Boolean
    ) = ShoppingListItem(
        id = UUID.randomUUID().toString(),
        shoppingListId = "list_1",
        productId = UUID.randomUUID().toString(),
        productNameSnapshot = name,
        quantity = qty,
        unit = UnitOfMeasure.PCS,
        estimatedUnitPrice = estimatedPrice,
        actualUnitPrice = actualPrice,
        isPurchased = isPurchased,
        isInCart = isInCart
    )

    @Test
    fun testLiveCartTotalVsRemainingToBuy() {
        val items = listOf(
            // In cart with shelf prices:
            createItem("Lapte", 2.0, estimatedPrice = 6.0, actualPrice = 6.50, isPurchased = false, isInCart = true),
            createItem("Pâine", 1.0, estimatedPrice = 4.0, actualPrice = 4.50, isPurchased = false, isInCart = true),
            // Still on shelf (to buy):
            createItem("Mere", 2.0, estimatedPrice = 5.0, actualPrice = null, isPurchased = false, isInCart = false),
            // Already purchased:
            createItem("Apă", 6.0, estimatedPrice = 3.0, actualPrice = 3.0, isPurchased = true, isInCart = true)
        )

        val summary = calculator(items)

        // In cart: (2 * 6.50) + (1 * 4.50) = 13.00 + 4.50 = 17.50
        assertEquals(17.50, summary.inCartTotal, 0.001)

        // Remaining to buy: 2 * 5.0 = 10.00
        assertEquals(10.00, summary.remainingToBuyEstimated, 0.001)

        // Actual spent on purchased: 6 * 3.0 = 18.00
        assertEquals(18.00, summary.actualSpent, 0.001)

        // Counts: 1 to buy, 2 in cart, 1 purchased
        assertEquals(1, summary.toBuyItemsCount)
        assertEquals(2, summary.inCartItemsCount)
        assertEquals(1, summary.paidItemsCount)
    }

    @Test
    fun testBudgetLimitsAndOverBudgetDetection() {
        val items = listOf(
            createItem("Carne de Vită", 1.0, estimatedPrice = 60.0, actualPrice = 65.0, isPurchased = false, isInCart = true),
            createItem("Brânză maturată", 1.0, estimatedPrice = 40.0, actualPrice = 45.0, isPurchased = false, isInCart = true)
        )

        // Case 1: Within budget (Budget: 150 RON, Current Cart: 110 RON)
        val withinBudgetSummary = calculator(items, budget = 150.0)
        assertFalse(withinBudgetSummary.isOverBudget)
        assertEquals(40.0, withinBudgetSummary.remainingBudget ?: 0.0, 0.001)
        assertEquals(0.0, withinBudgetSummary.overBudgetAmount, 0.001)

        // Case 2: Over budget (Budget: 100 RON, Current Cart: 110 RON -> Over by 10 RON)
        val overBudgetSummary = calculator(items, budget = 100.0)
        assertTrue(overBudgetSummary.isOverBudget)
        assertEquals(-10.0, overBudgetSummary.remainingBudget ?: 0.0, 0.001)
        assertEquals(10.0, overBudgetSummary.overBudgetAmount, 0.001)
    }

    @Test
    fun testDiscountsAndSgrFees() {
        val items = listOf(
            createItem("Suc Portocale", 2.0, estimatedPrice = 8.0, actualPrice = 8.0, isPurchased = false, isInCart = true)
        )

        // Total 16.00 RON. Discount coupon: 3.00 RON, SGR Guarantee fee (2 sticle * 0.50 lei): 1.00 RON
        val summary = calculator(
            items = items,
            discountAmount = 3.00,
            extraFeesAmount = 1.00
        )

        // Net estimated total: 16.00 - 3.00 + 1.00 = 14.00 RON
        assertEquals(14.00, summary.netEstimatedTotal, 0.001)
    }

    @Test
    fun testReceiptReconciliationVariance() {
        val items = listOf(
            createItem("Detergent", 1.0, estimatedPrice = 45.0, actualPrice = 45.0, isPurchased = true, isInCart = true)
        )

        // Actual spent was 45.00 RON, but final receipt total at checkout was 43.50 RON (due to loyalty discount)
        val summary = calculator(
            items = items,
            receiptTotal = 43.50
        )

        // Receipt variance: 43.50 - 45.00 = -1.50 RON
        assertEquals(-1.50, summary.receiptVariance ?: 0.0, 0.001)
    }
}
