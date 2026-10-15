package ro.smartshopping.list.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.smartshopping.list.data.local.dao.PriceRecordDao
import ro.smartshopping.list.data.local.dao.ProductDao
import ro.smartshopping.list.data.local.entity.PriceRecordEntity
import ro.smartshopping.list.data.local.entity.ProductEntity
import ro.smartshopping.list.domain.model.PriceRecord
import ro.smartshopping.list.domain.model.Product
import ro.smartshopping.list.domain.model.ProductPriceHistory
import ro.smartshopping.list.domain.model.UnitOfMeasure
import java.util.UUID

interface ProductRepository {
    fun getAllProductsFlow(): Flow<List<Product>>
    fun searchProductsFlow(query: String): Flow<List<Product>>
    suspend fun getProductById(id: String): Product?
    suspend fun findProductByBarcode(barcode: String): Product?
    suspend fun findOrCreateProduct(name: String, category: String = "Alimente", defaultUnit: UnitOfMeasure = UnitOfMeasure.PCS, estimatedPrice: Double? = null): Product
    suspend fun saveProduct(product: Product)
    suspend fun updateProduct(product: Product)
    suspend fun deleteProduct(id: String)
}

class ProductRepositoryImpl(
    private val productDao: ProductDao
) : ProductRepository {

    override fun getAllProductsFlow(): Flow<List<Product>> {
        return productDao.getAllProductsFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun searchProductsFlow(query: String): Flow<List<Product>> {
        return productDao.searchProductsFlow(query.trim()).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProductById(id: String): Product? {
        return productDao.getProductById(id)?.toDomain()
    }

    override suspend fun findProductByBarcode(barcode: String): Product? {
        return productDao.findProductByBarcode(barcode.trim())?.toDomain()
    }

    override suspend fun findOrCreateProduct(
        name: String,
        category: String,
        defaultUnit: UnitOfMeasure,
        estimatedPrice: Double?
    ): Product {
        val existing = productDao.findProductByName(name.trim())
        if (existing != null) {
            return existing.toDomain()
        }
        val newId = UUID.randomUUID().toString()
        val newEntity = ProductEntity(
            id = newId,
            ownerId = null,
            name = name.trim(),
            category = category,
            brand = null,
            defaultUnit = defaultUnit,
            barcode = null,
            lastKnownPrice = estimatedPrice,
            currency = "RON",
            createdAt = System.currentTimeMillis()
        )
        productDao.insertProduct(newEntity)
        return newEntity.toDomain()
    }

    override suspend fun saveProduct(product: Product) {
        productDao.insertProduct(product.toEntity())
    }

    override suspend fun updateProduct(product: Product) {
        productDao.updateProduct(product.toEntity())
    }

    override suspend fun deleteProduct(id: String) {
        val entity = productDao.getProductById(id) ?: return
        productDao.deleteProduct(entity)
    }

    private fun ProductEntity.toDomain() = Product(
        id = id,
        ownerId = ownerId,
        name = name,
        category = category,
        brand = brand,
        defaultUnit = defaultUnit,
        barcode = barcode,
        lastKnownPrice = lastKnownPrice,
        currency = currency,
        createdAt = createdAt
    )

    private fun Product.toEntity() = ProductEntity(
        id = id,
        ownerId = ownerId,
        name = name,
        category = category,
        brand = brand,
        defaultUnit = defaultUnit,
        barcode = barcode,
        lastKnownPrice = lastKnownPrice,
        currency = currency,
        createdAt = createdAt
    )
}

interface PriceTrackingRepository {
    fun getPriceRecordsForProductFlow(productId: String): Flow<List<PriceRecord>>
    suspend fun getProductPriceHistory(productId: String): ProductPriceHistory
    fun getRecentPriceRecordsFlow(limit: Int = 50): Flow<List<PriceRecord>>
    fun getRecordedStoresFlow(): Flow<List<String>>
    suspend fun recordPrice(record: PriceRecord)
}

class PriceTrackingRepositoryImpl(
    private val priceRecordDao: PriceRecordDao,
    private val productDao: ProductDao
) : PriceTrackingRepository {

    override fun getPriceRecordsForProductFlow(productId: String): Flow<List<PriceRecord>> {
        return priceRecordDao.getPriceRecordsForProductFlow(productId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getProductPriceHistory(productId: String): ProductPriceHistory {
        val product = productDao.getProductById(productId)
        val records = priceRecordDao.getPriceRecordsForProduct(productId).map { it.toDomain() }

        val prices = records.map { it.unitPrice }
        val latestPrice = prices.firstOrNull()
        val previousPrice = prices.getOrNull(1)
        val lowestPrice = prices.minOrNull()
        val highestPrice = prices.maxOrNull()
        val averagePrice = if (prices.isNotEmpty()) prices.average() else null

        val priceChangeAbs = if (latestPrice != null && previousPrice != null) latestPrice - previousPrice else null
        val priceChangePct = if (latestPrice != null && previousPrice != null && previousPrice > 0.0) {
            ((latestPrice - previousPrice) / previousPrice) * 100.0
        } else null

        val storeGroups = records.filter { !it.storeName.isNullOrBlank() }.groupBy { it.storeName!! }
        val storeComparisons = storeGroups.map { (store, storeRecords) ->
            val storePrices = storeRecords.map { it.unitPrice }
            ro.smartshopping.list.domain.model.StorePriceComparison(
                storeName = store,
                storeAddress = storeRecords.firstOrNull()?.storeAddress,
                latestPrice = storePrices.first(),
                lowestPrice = storePrices.min(),
                highestPrice = storePrices.max(),
                lastRecordedAt = storeRecords.first().recordedAt,
                currency = product?.currency ?: "RON"
            )
        }.sortedBy { it.latestPrice }

        return ProductPriceHistory(
            productId = productId,
            productName = product?.name ?: "Produs",
            records = records,
            latestPrice = latestPrice,
            previousPrice = previousPrice,
            lowestPrice = lowestPrice,
            highestPrice = highestPrice,
            averagePrice = averagePrice,
            priceChangeAbsolute = priceChangeAbs,
            priceChangePercentage = priceChangePct,
            currency = product?.currency ?: "RON",
            storeComparisons = storeComparisons
        )
    }

    override fun getRecentPriceRecordsFlow(limit: Int): Flow<List<PriceRecord>> {
        return priceRecordDao.getRecentPriceRecordsFlow(limit).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getRecordedStoresFlow(): Flow<List<String>> {
        return priceRecordDao.getAllRecordedStoresFlow()
    }

    override suspend fun recordPrice(record: PriceRecord) {
        priceRecordDao.insertPriceRecord(record.toEntity())
        // Also update product's lastKnownPrice
        val product = productDao.getProductById(record.productId)
        if (product != null) {
            productDao.updateProduct(product.copy(lastKnownPrice = record.unitPrice))
        }
    }

    private fun PriceRecordEntity.toDomain() = PriceRecord(
        id = id,
        productId = productId,
        shoppingListId = shoppingListId,
        unitPrice = unitPrice,
        quantity = quantity,
        unit = unit,
        currency = currency,
        storeName = storeName,
        storeAddress = storeAddress,
        sourceType = sourceType,
        recordedAt = recordedAt
    )

    private fun PriceRecord.toEntity() = PriceRecordEntity(
        id = id,
        productId = productId,
        shoppingListId = shoppingListId,
        unitPrice = unitPrice,
        quantity = quantity,
        unit = unit,
        currency = currency,
        storeName = storeName,
        storeAddress = storeAddress,
        sourceType = sourceType,
        recordedAt = recordedAt
    )
}
