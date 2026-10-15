package ro.smartshopping.list

import org.junit.Assert.assertTrue
import org.junit.Test
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.usecase.FormatListForSharingUseCase

class FormatListForSharingTest {

    private val formatter = FormatListForSharingUseCase()

    @Test
    fun `whatsapp share formatting includes title checkmarks and summary`() {
        val list = ShoppingList(
            id = "1",
            name = "Weekly Shopping",
            currency = "RON"
        )
        val items = listOf(
            ShoppingListItem("1", "1", "milk", "Milk", quantity = 2.0, unit = UnitOfMeasure.PCS, estimatedUnitPrice = 7.50, isPurchased = true),
            ShoppingListItem("2", "1", "bread", "Bread", quantity = 3.0, unit = UnitOfMeasure.PCS, estimatedUnitPrice = 4.00, isPurchased = false),
            ShoppingListItem("3", "1", "apples", "Apples", quantity = 2.0, unit = UnitOfMeasure.KG, estimatedUnitPrice = 6.00, isPurchased = false)
        )

        val text = formatter(list, items, isRomanian = false)

        // Verifications
        assertTrue(text.contains("🛒 WEEKLY SHOPPING"))
        assertTrue(text.contains("☑ Milk — 2 pcs — 7.50 RON/pcs"))
        assertTrue(text.contains("☐ Bread — 3 pcs — 4.00 RON/pcs"))
        assertTrue(text.contains("☐ Apples — 2 kg — 6.00 RON/kg"))
        assertTrue(text.contains("Progress: 1/3 items (33%)"))
        assertTrue(text.contains("Estimated total: 39.00 RON"))
        assertTrue(text.contains("Purchased: 15.00 RON"))
        assertTrue(text.contains("Remaining: 24.00 RON"))
    }
}
