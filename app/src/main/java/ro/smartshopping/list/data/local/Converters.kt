package ro.smartshopping.list.data.local

import androidx.room.TypeConverter
import ro.smartshopping.list.domain.model.PriceSourceType
import ro.smartshopping.list.domain.model.ShoppingListStatus
import ro.smartshopping.list.domain.model.UnitOfMeasure

class Converters {

    @TypeConverter
    fun fromUnitOfMeasure(value: UnitOfMeasure?): String? = value?.name

    @TypeConverter
    fun toUnitOfMeasure(value: String?): UnitOfMeasure? {
        return value?.let {
            try {
                UnitOfMeasure.valueOf(it)
            } catch (e: Exception) {
                UnitOfMeasure.PCS
            }
        }
    }

    @TypeConverter
    fun fromShoppingListStatus(value: ShoppingListStatus?): String? = value?.name

    @TypeConverter
    fun toShoppingListStatus(value: String?): ShoppingListStatus? {
        return value?.let {
            try {
                ShoppingListStatus.valueOf(it)
            } catch (e: Exception) {
                ShoppingListStatus.ACTIVE
            }
        }
    }

    @TypeConverter
    fun fromPriceSourceType(value: PriceSourceType?): String? = value?.name

    @TypeConverter
    fun toPriceSourceType(value: String?): PriceSourceType? {
        return value?.let {
            try {
                PriceSourceType.valueOf(it)
            } catch (e: Exception) {
                PriceSourceType.CONFIRMED_PURCHASE
            }
        }
    }

    @TypeConverter
    fun fromBarcodeType(value: ro.smartshopping.list.domain.model.BarcodeType?): String? = value?.name

    @TypeConverter
    fun toBarcodeType(value: String?): ro.smartshopping.list.domain.model.BarcodeType? {
        return value?.let {
            try {
                ro.smartshopping.list.domain.model.BarcodeType.valueOf(it)
            } catch (e: Exception) {
                ro.smartshopping.list.domain.model.BarcodeType.CODE_128
            }
        }
    }
}
