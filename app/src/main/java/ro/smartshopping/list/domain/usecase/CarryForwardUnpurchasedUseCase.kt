package ro.smartshopping.list.domain.usecase

import ro.smartshopping.list.domain.model.ShoppingListItem
import java.util.UUID

class CarryForwardUnpurchasedUseCase {

    /**
     * Extracts unpurchased items from a previous list and transforms them into
     * fresh items ready to be inserted into a new shopping list.
     *
     * Invariants guaranteed:
     * - Only items where isPurchased == false are carried forward.
     * - Newly created items have isPurchased = false and actualUnitPrice = null.
     * - Preserves product identity, name snapshot, quantity, and unit.
     * - Carries forward the estimated unit price or latest known price as an estimate.
     * - Generates clean unique IDs for the new list.
     * - The original list items remain completely unmodified.
     */
    operator fun invoke(
        sourceItems: List<ShoppingListItem>,
        targetShoppingListId: String,
        selectedItemIds: Set<String>? = null
    ): List<ShoppingListItem> {
        val eligibleItems = sourceItems.filter { item ->
            !item.isPurchased && (selectedItemIds == null || selectedItemIds.contains(item.id))
        }

        return eligibleItems.map { sourceItem ->
            ShoppingListItem(
                id = UUID.randomUUID().toString(),
                shoppingListId = targetShoppingListId,
                productId = sourceItem.productId,
                productNameSnapshot = sourceItem.productNameSnapshot,
                quantity = sourceItem.quantity,
                unit = sourceItem.unit,
                estimatedUnitPrice = sourceItem.estimatedUnitPrice ?: sourceItem.actualUnitPrice,
                actualUnitPrice = null,
                isPurchased = false,
                isInCart = false,
                packageCount = sourceItem.packageCount,
                category = sourceItem.category,
                notes = sourceItem.notes,
                createdAt = System.currentTimeMillis(),
                purchasedAt = null
            )
        }
    }
}
