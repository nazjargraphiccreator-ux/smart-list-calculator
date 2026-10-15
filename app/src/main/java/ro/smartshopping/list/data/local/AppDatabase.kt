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
import ro.smartshopping.list.data.local.dao.LoyaltyCardDao
import ro.smartshopping.list.data.local.dao.PriceRecordDao
import ro.smartshopping.list.data.local.dao.ProductDao
import ro.smartshopping.list.data.local.dao.ShoppingListDao
import ro.smartshopping.list.data.local.dao.ShoppingListItemDao
import ro.smartshopping.list.data.local.dao.UserDao
import ro.smartshopping.list.data.local.entity.LoyaltyCardEntity
import ro.smartshopping.list.data.local.entity.PriceRecordEntity
import ro.smartshopping.list.data.local.entity.ProductEntity
import ro.smartshopping.list.data.local.entity.ShoppingListEntity
import ro.smartshopping.list.data.local.entity.ShoppingListItemEntity
import ro.smartshopping.list.data.local.entity.UserEntity
import ro.smartshopping.list.domain.model.BarcodeType
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
        UserEntity::class,
        LoyaltyCardEntity::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun shoppingListItemDao(): ShoppingListItemDao
    abstract fun productDao(): ProductDao
    abstract fun priceRecordDao(): PriceRecordDao
    abstract fun userDao(): UserDao
    abstract fun loyaltyCardDao(): LoyaltyCardDao

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

                // Starter Loyalty Cards for Romanian Supermarkets (Militari & General)
                val starterCards = listOf(
                    LoyaltyCardEntity(
                        id = UUID.randomUUID().toString(),
                        storeName = "Kaufland Militari",
                        programName = "Kaufland Card",
                        cardNumber = "2800192837461",
                        barcodeType = BarcodeType.CODE_128,
                        primaryColorHex = "#E11A22",
                        secondaryColorHex = "#7A0006",
                        cardHolderName = "Client Fidel Militari",
                        notes = "Valea Cascadelor nr. 3",
                        isFavorite = true,
                        createdAt = System.currentTimeMillis() - 86400000L * 14,
                        lastScannedAt = System.currentTimeMillis() - 86400000L * 2
                    ),
                    LoyaltyCardEntity(
                        id = UUID.randomUUID().toString(),
                        storeName = "Auchan Militari",
                        programName = "MyClub Auchan",
                        cardNumber = "0448927163548",
                        barcodeType = BarcodeType.CODE_128,
                        primaryColorHex = "#D61821",
                        secondaryColorHex = "#0E7535",
                        cardHolderName = "Client Fidel Auchan",
                        notes = "Centrul Comercial Militari",
                        isFavorite = true,
                        createdAt = System.currentTimeMillis() - 86400000L * 10,
                        lastScannedAt = System.currentTimeMillis() - 86400000L * 3
                    ),
                    LoyaltyCardEntity(
                        id = UUID.randomUUID().toString(),
                        storeName = "Lidl",
                        programName = "Lidl Plus",
                        cardNumber = "9900881122334",
                        barcodeType = BarcodeType.QR_CODE,
                        primaryColorHex = "#0050AA",
                        secondaryColorHex = "#FFCC00",
                        cardHolderName = "Lidl Plus Ro",
                        notes = "Cupon Reducere Activ",
                        isFavorite = false,
                        createdAt = System.currentTimeMillis() - 86400000L * 20,
                        lastScannedAt = null
                    ),
                    LoyaltyCardEntity(
                        id = UUID.randomUUID().toString(),
                        storeName = "Carrefour",
                        programName = "Act For Good",
                        cardNumber = "1092837465012",
                        barcodeType = BarcodeType.CODE_128,
                        primaryColorHex = "#004E98",
                        secondaryColorHex = "#E63946",
                        cardHolderName = "Carrefour Puncte",
                        notes = "Puncte & Vouchere",
                        isFavorite = false,
                        createdAt = System.currentTimeMillis() - 86400000L * 25,
                        lastScannedAt = null
                    )
                )
                db.loyaltyCardDao().insertLoyaltyCards(starterCards)
            }
        }
    }
}
