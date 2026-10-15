package ro.smartshopping.list

import org.junit.Assert.assertEquals
import org.junit.Test
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.usecase.CalculateCompletionRateUseCase

class CalculateCompletionRateTest {

    private val calculator = CalculateCompletionRateUseCase()

    @Test
    fun `empty list returns 0 percent without division by zero error`() {
        val result = calculator(emptyList())
        assertEquals(0, result)
    }

    @Test
    fun `zero total count returns 0 percent`() {
        val result = calculator.fromCounts(0, 0)
        assertEquals(0, result)
    }

    @Test
    fun `example from master prompt - 12 of 20 items purchased returns 60 percent`() {
        val items = (1..20).map { i ->
            ShoppingListItem(
                id = "item_$i",
                shoppingListId = "list_1",
                productId = "prod_$i",
                productNameSnapshot = "Produs $i",
                isPurchased = i <= 12
            )
        }

        val result = calculator(items)
        assertEquals(60, result)
    }

    @Test
    fun `all items purchased returns 100 percent`() {
        val items = listOf(
            ShoppingListItem("1", "l", "p", "Lapte", isPurchased = true),
            ShoppingListItem("2", "l", "p", "Pâine", isPurchased = true)
        )
        val result = calculator(items)
        assertEquals(100, result)
    }

    @Test
    fun `one of three items purchased rounds correctly to 33 percent`() {
        val items = listOf(
            ShoppingListItem("1", "l", "p", "A", isPurchased = true),
            ShoppingListItem("2", "l", "p", "B", isPurchased = false),
            ShoppingListItem("3", "l", "p", "C", isPurchased = false)
        )
        val result = calculator(items)
        assertEquals(33, result)
    }
}
