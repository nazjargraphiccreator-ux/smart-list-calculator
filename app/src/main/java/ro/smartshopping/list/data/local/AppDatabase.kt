package ro.smartshopping.list.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ro.smartshopping.list.data.local.dao.PriceRecordDao
import ro.smartshopping.list.data.local.dao.ProductDao
import ro.smartshopping.list.data.local.dao.ShoppingListDao
import ro.smartshopping.list.data.local.dao.ShoppingListItemDao
import ro.smartshopping.list.data.local.dao.UserDao
import ro.smartshopping.list.data.local.entity.PriceRecordEntity
import ro.smartshopping.list.data.local.entity.ProductEntity
import ro.smartshopping.list.data.local.entity.ShoppingListEntity
import ro.smartshopping.list.data.local.entity.ShoppingListItemEntity
import ro.smartshopping.list.data.local.entity.UserEntity
import ro.smartshopping.list.domain.model.PriceSourceType
import ro.smartshopping.list.domain.model.ShoppingListStatus
import ro.smartshopping.list.domain.model.UnitOfMeasure
import java.util.UUID

@Database(
    entities = [
        ShoppingListEntity::class,
        ShoppingListItemEntity::class,
        ProductEntity::class,
        PriceRecordEntity::class,
        UserEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao
    abstract fun productDao(): ProductDao
    abstract fun priceRecordDao(): PriceRecordDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_shopping_list.db"
                )
                    .addCallback(DatabaseCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Prepopulate with practical starter data for immediate testing
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        prepopulateStarterData(database)
                    }
                }
            }

            private suspend fun prepopulateStarterData(db: AppDatabase) {
                val p1Id = UUID.randomUUID().toString()
                val p2Id = UUID.randomUUID().toString()
                val p3Id = UUID.randomUUID().toString()
                val p4Id = UUID.randomUUID().toString()
                val p5Id = UUID.randomUUID().toString()

                val products = listOf(
                    ProductEntity(p1Id, null, "Lapte Zuzu 3.5%", "Lactate", "Zuzu", UnitOfMeasure.L, null, 7.50, "RON", System.currentTimeMillis()),
                    ProductEntity(p2Id, null, "Pâine Toast Integrală", "Panificație", "Vel Pitar", UnitOfMeasure.PCS, null, 4.00, "RON", System.currentTimeMillis()),
                    ProductEntity(p3Id, null, "Mere Idared", "Fructe & Legume", "Românești", UnitOfMeasure.KG, null, 6.00, "RON", System.currentTimeMillis()),
                    ProductEntity(p4Id, null, "Ouă Mărimea M (10 buc)", "Lactate & Ouă", "Toneli", UnitOfMeasure.PACK, null, 14.50, "RON", System.currentTimeMillis()),
                    ProductEntity(p5Id, null, "Cafea Boabe 500g", "Băuturi", "Lavazza", UnitOfMeasure.PACK, null, 38.00, "RON", System.currentTimeMillis())
                )
                db.productDao().insertProducts(products)

                // Starter Active List: Cumpărături Săptămânale
                val list1Id = UUID.randomUUID().toString()
                val list1 = ShoppingListEntity(
                    id = list1Id,
                    ownerId = null,
                    name = "Cumpărături Săptămânale",
                    status = ShoppingListStatus.ACTIVE,
                    currency = "RON",
                    shoppingDate = System.currentTimeMillis() + 86400000L,
                    notes = "Supermarket Kaufland / Mega Image",
                    createdAt = System.currentTimeMillis() - 3600000L,
                    updatedAt = System.currentTimeMillis(),
                    completedAt = null
                )
                db.shoppingListDao().insertList(list1)

                val items1 = listOf(
                    ShoppingListItemEntity(UUID.randomUUID().toString(), list1Id, p1Id, "Lapte Zuzu 3.5%", 2.0, UnitOfMeasure.L, 7.50, 7.50, true, "Lactate", null, System.currentTimeMillis() - 2000L, System.currentTimeMillis() - 1000L),
                    ShoppingListItemEntity(UUID.randomUUID().toString(), list1Id, p2Id, "Pâine Toast Integrală", 3.0, UnitOfMeasure.PCS, 4.00, null, false, "Panificație", null, System.currentTimeMillis() - 2000L, null),
                    ShoppingListItemEntity(UUID.randomUUID().toString(), list1Id, p3Id, "Mere Idared", 2.0, UnitOfMeasure.KG, 6.00, null, false, "Fructe & Legume", null, System.currentTimeMillis() - 2000L, null),
                    ShoppingListItemEntity(UUID.randomUUID().toString(), list1Id, p4Id, "Ouă Mărimea M (10 buc)", 1.0, UnitOfMeasure.PACK, 14.50, null, false, "Lactate & Ouă", null, System.currentTimeMillis() - 2000L, null)
                )
                db.shoppingListItemDao().insertItems(items1)

                // Starter Price Record for historical charts
                val priceRecords = listOf(
                    PriceRecordEntity(UUID.randomUUID().toString(), p1Id, list1Id, 7.50, 2.0, UnitOfMeasure.L, "RON", "Kaufland", null, PriceSourceType.CONFIRMED_PURCHASE, System.currentTimeMillis() - 86400000L * 7),
                    PriceRecordEntity(UUID.randomUUID().toString(), p1Id, list1Id, 7.80, 2.0, UnitOfMeasure.L, "RON", "Mega Image", null, PriceSourceType.CONFIRMED_PURCHASE, System.currentTimeMillis())
                )
                db.priceRecordDao().insertPriceRecords(priceRecords)
            }
        }
    }
}
