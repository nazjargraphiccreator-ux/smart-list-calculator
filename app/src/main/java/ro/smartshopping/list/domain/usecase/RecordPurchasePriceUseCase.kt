package ro.smartshopping.list.domain.usecase

import ro.smartshopping.list.domain.model.PriceRecord
import ro.smartshopping.list.domain.model.PriceSourceType
import ro.smartshopping.list.domain.model.ShoppingListItem
import java.util.UUID

class RecordPurchasePriceUseCase {

    /**
     * Creates a permanent historical PriceRecord when a purchase is confirmed.
     * Guaranteed rules:
     * - Only creates record if actual price or estimated price is > 0.
     * - Never overwrites an old record; creates a timestamped new entry.
     */
    operator fun invoke(
        item: ShoppingListItem,
        currency: String = "RON",
        storeName: String? = null,
        storeAddress: String? = null
    ): PriceRecord? {
        val confirmedPrice = item.actualUnitPrice ?: item.estimatedUnitPrice ?: return null
        if (confirmedPrice <= 0.0) return null

        return PriceRecord(
            id = UUID.randomUUID().toString(),
            productId = item.productId,
            shoppingListId = item.shoppingListId,
            unitPrice = confirmedPrice,
            quantity = item.quantity,
            unit = item.unit,
            currency = currency,
            storeName = storeName ?: item.storeName,
            storeAddress = storeAddress,
            sourceType = if (item.actualUnitPrice != null) PriceSourceType.CONFIRMED_PURCHASE else PriceSourceType.ESTIMATED,
            recordedAt = System.currentTimeMillis()
        )
    }
}
