package ro.smartshopping.list.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import ro.smartshopping.list.domain.model.PriceSourceType
import ro.smartshopping.list.domain.model.ShoppingListStatus
import ro.smartshopping.list.domain.model.UnitOfMeasure

@Entity(
    tableName = "shopping_lists",
    indices = [
        Index("ownerId"),
        Index("status")
    ]
)
data class ShoppingListEntity(
    @PrimaryKey val id: String,
    val ownerId: String?,
    val name: String,
    val status: ShoppingListStatus,
    val currency: String,
    val shoppingDate: Long?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long?,
    val storeName: String? = null,
    val storeAddress: String? = null,
    val storeLatitude: Double? = null,
    val storeLongitude: Double? = null,
    val budget: Double? = null,
    val receiptTotal: Double? = null,
    val discountAmount: Double? = null,
    val extraFeesAmount: Double? = null
)

@Entity(
    tableName = "shopping_list_items",
    indices = [
        Index("shoppingListId"),
        Index("productId"),
        Index("isPurchased"),
        Index("isInCart")
    ],
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["shoppingListId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ShoppingListItemEntity(
    @PrimaryKey val id: String,
    val shoppingListId: String,
    val productId: String,
    val productNameSnapshot: String,
    val quantity: Double,
    val unit: UnitOfMeasure,
    val estimatedUnitPrice: Double?,
    val actualUnitPrice: Double?,
    val isPurchased: Boolean,
    val category: String,
    val notes: String?,
    val createdAt: Long,
    val purchasedAt: Long?,
    val isInCart: Boolean = false,
    val packageCount: Int? = null,
    val storeName: String? = null
)

@Entity(
    tableName = "products",
    indices = [
        Index("ownerId"),
        Index("name"),
        Index("category")
    ]
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val ownerId: String?,
    val name: String,
    val category: String,
    val brand: String?,
    val defaultUnit: UnitOfMeasure,
    val barcode: String?,
    val lastKnownPrice: Double?,
    val currency: String,
    val createdAt: Long
)

@Entity(
    tableName = "price_records",
    indices = [
        Index("productId"),
        Index("recordedAt"),
        Index("storeName")
    ]
)
data class PriceRecordEntity(
    @PrimaryKey val id: String,
    val productId: String,
    val shoppingListId: String?,
    val unitPrice: Double,
    val quantity: Double,
    val unit: UnitOfMeasure,
    val currency: String,
    val storeName: String?,
    val storeAddress: String? = null,
    val sourceType: PriceSourceType,
    val recordedAt: Long
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String?,
    val email: String?,
    val avatarUrl: String?,
    val createdAt: Long
)

@Entity(
    tableName = "loyalty_cards",
    indices = [
        Index("storeName"),
        Index("isFavorite")
    ]
)
data class LoyaltyCardEntity(
    @PrimaryKey val id: String,
    val storeName: String,
    val programName: String,
    val cardNumber: String,
    val barcodeType: ro.smartshopping.list.domain.model.BarcodeType,
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val cardHolderName: String?,
    val notes: String?,
    val isFavorite: Boolean,
    val createdAt: Long,
    val lastScannedAt: Long?
)
