package ro.smartshopping.list.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import ro.smartshopping.list.data.local.entity.PriceRecordEntity
import ro.smartshopping.list.data.local.entity.ProductEntity
import ro.smartshopping.list.data.local.entity.ShoppingListEntity
import ro.smartshopping.list.data.local.entity.ShoppingListItemEntity
import ro.smartshopping.list.data.local.entity.UserEntity
import ro.smartshopping.list.domain.model.ShoppingListStatus

@Dao
interface ShoppingListDao {
    @Query("SELECT * FROM shopping_lists ORDER BY updatedAt DESC")
    fun getAllListsFlow(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE status = :status ORDER BY updatedAt DESC")
    fun getListsByStatusFlow(status: ShoppingListStatus): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    suspend fun getListById(id: String): ShoppingListEntity?

    @Query("SELECT * FROM shopping_lists WHERE id = :id LIMIT 1")
    fun getListByIdFlow(id: String): Flow<ShoppingListEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertList(list: ShoppingListEntity)

    @Update
    suspend fun updateList(list: ShoppingListEntity)

    @Delete
    suspend fun deleteList(list: ShoppingListEntity)

    @Query("DELETE FROM shopping_lists WHERE id = :id")
    suspend fun deleteListById(id: String)

    @Query("UPDATE shopping_lists SET storeName = :storeName, storeAddress = :storeAddress, storeLatitude = :lat, storeLongitude = :lng, budget = :budget, receiptTotal = :receiptTotal, discountAmount = :discount, extraFeesAmount = :extraFees, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStoreAndBudget(
        id: String,
        storeName: String?,
        storeAddress: String?,
        lat: Double?,
        lng: Double?,
        budget: Double?,
        receiptTotal: Double?,
        discount: Double?,
        extraFees: Double?,
        updatedAt: Long = System.currentTimeMillis()
    )
}

@Dao
interface ShoppingListItemDao {
    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId ORDER BY isPurchased ASC, isInCart ASC, createdAt ASC")
    fun getItemsForListFlow(listId: String): Flow<List<ShoppingListItemEntity>>

    @Query("SELECT * FROM shopping_list_items WHERE shoppingListId = :listId")
    suspend fun getItemsForList(listId: String): List<ShoppingListItemEntity>

    @Query("SELECT * FROM shopping_list_items WHERE id = :id LIMIT 1")
    suspend fun getItemById(id: String): ShoppingListItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: ShoppingListItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ShoppingListItemEntity>)

    @Update
    suspend fun updateItem(item: ShoppingListItemEntity)

    @Delete
    suspend fun deleteItem(item: ShoppingListItemEntity)

    @Query("DELETE FROM shopping_list_items WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("UPDATE shopping_list_items SET isPurchased = :isPurchased, actualUnitPrice = :actualPrice, purchasedAt = :purchasedAt WHERE id = :id")
    suspend fun updatePurchaseStatus(id: String, isPurchased: Boolean, actualPrice: Double?, purchasedAt: Long?)

    @Query("UPDATE shopping_list_items SET isInCart = :isInCart WHERE id = :id")
    suspend fun updateCartStatus(id: String, isInCart: Boolean)

    @Query("UPDATE shopping_list_items SET isInCart = :isInCart, isPurchased = :isPurchased, actualUnitPrice = :actualPrice, purchasedAt = :purchasedAt WHERE id = :id")
    suspend fun updateThreeStageStatus(id: String, isInCart: Boolean, isPurchased: Boolean, actualPrice: Double?, purchasedAt: Long?)
}

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProductsFlow(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' OR category LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchProductsFlow(query: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun getProductById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun findProductByName(name: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProducts(products: List<ProductEntity>)

    @Update
    suspend fun updateProduct(product: ProductEntity)

    @Delete
    suspend fun deleteProduct(product: ProductEntity)
}

@Dao
interface PriceRecordDao {
    @Query("SELECT * FROM price_records WHERE productId = :productId ORDER BY recordedAt DESC")
    fun getPriceRecordsForProductFlow(productId: String): Flow<List<PriceRecordEntity>>

    @Query("SELECT * FROM price_records WHERE productId = :productId ORDER BY recordedAt DESC")
    suspend fun getPriceRecordsForProduct(productId: String): List<PriceRecordEntity>

    @Query("SELECT * FROM price_records WHERE productId = :productId AND storeName = :storeName ORDER BY recordedAt DESC")
    suspend fun getPriceRecordsForProductAndStore(productId: String, storeName: String): List<PriceRecordEntity>

    @Query("SELECT DISTINCT storeName FROM price_records WHERE storeName IS NOT NULL AND storeName != '' ORDER BY storeName ASC")
    fun getAllRecordedStoresFlow(): Flow<List<String>>

    @Query("SELECT * FROM price_records ORDER BY recordedAt DESC LIMIT :limit")
    fun getRecentPriceRecordsFlow(limit: Int = 50): Flow<List<PriceRecordEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecord(record: PriceRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPriceRecords(records: List<PriceRecordEntity>)
}

@Dao
interface UserDao {
    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getCurrentUser(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("DELETE FROM users")
    suspend fun clearUser()
}
