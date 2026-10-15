package ro.smartshopping.list.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import ro.smartshopping.list.data.local.dao.ShoppingListDao
import ro.smartshopping.list.data.local.dao.ShoppingListItemDao
import ro.smartshopping.list.data.local.entity.ShoppingListEntity
import ro.smartshopping.list.data.local.entity.ShoppingListItemEntity
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.ShoppingListStatus
import ro.smartshopping.list.domain.usecase.CalculateCompletionRateUseCase
import ro.smartshopping.list.domain.usecase.CalculateFinancialSummaryUseCase
import java.util.UUID

interface ShoppingListRepository {
    fun getAllListsFlow(): Flow<List<ShoppingList>>
    fun getListsByStatusFlow(status: ShoppingListStatus): Flow<List<ShoppingList>>
    fun getListByIdFlow(id: String): Flow<ShoppingList?>
    suspend fun getListById(id: String): ShoppingList?
    suspend fun createList(name: String, currency: String = "RON", shoppingDate: Long? = null, notes: String? = null): String
    suspend fun updateList(list: ShoppingList)
    suspend fun updateListStatus(id: String, status: ShoppingListStatus)
    suspend fun deleteList(id: String)
    suspend fun duplicateList(sourceListId: String, newName: String, preservePrices: Boolean = true): String

    // Items
    fun getItemsForListFlow(listId: String): Flow<List<ShoppingListItem>>
    suspend fun getItemsForList(listId: String): List<ShoppingListItem>
    suspend fun addItem(item: ShoppingListItem)
    suspend fun addItems(items: List<ShoppingListItem>)
    suspend fun updateItem(item: ShoppingListItem)
    suspend fun deleteItem(id: String)
    suspend fun togglePurchaseStatus(itemId: String, isPurchased: Boolean, actualPrice: Double? = null)
    suspend fun updateCartStatus(itemId: String, isInCart: Boolean)
    suspend fun updateItemThreeStage(itemId: String, stage: ro.smartshopping.list.domain.model.ItemPurchaseStage, actualPrice: Double? = null)
    suspend fun updateStoreAndBudget(
        id: String,
        storeName: String?,
        storeAddress: String?,
        lat: Double?,
        lng: Double?,
        budget: Double?,
        receiptTotal: Double?,
        discount: Double? = null,
        extraFees: Double? = null
    )
}

class ShoppingListRepositoryImpl(
    private val listDao: ShoppingListDao,
    private val itemDao: ShoppingListItemDao,
    private val completionCalculator: CalculateCompletionRateUseCase = CalculateCompletionRateUseCase(),
    private val financialCalculator: CalculateFinancialSummaryUseCase = CalculateFinancialSummaryUseCase()
) : ShoppingListRepository {

    override fun getAllListsFlow(): Flow<List<ShoppingList>> {
        return listDao.getAllListsFlow().map { entities ->
            entities.map { entity ->
                val items = itemDao.getItemsForList(entity.id)
                mapToDomainWithMetrics(entity, items)
            }
        }
    }

    override fun getListsByStatusFlow(status: ShoppingListStatus): Flow<List<ShoppingList>> {
        return listDao.getListsByStatusFlow(status).map { entities ->
            entities.map { entity ->
                val items = itemDao.getItemsForList(entity.id)
                mapToDomainWithMetrics(entity, items)
            }
        }
    }

    override fun getListByIdFlow(id: String): Flow<ShoppingList?> {
        return combine(
            listDao.getListByIdFlow(id),
            itemDao.getItemsForListFlow(id)
        ) { entity, items ->
            entity?.let { mapToDomainWithMetrics(it, items) }
        }
    }

    override suspend fun getListById(id: String): ShoppingList? {
        val entity = listDao.getListById(id) ?: return null
        val items = itemDao.getItemsForList(id)
        return mapToDomainWithMetrics(entity, items)
    }

    override suspend fun createList(name: String, currency: String, shoppingDate: Long?, notes: String?): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = ShoppingListEntity(
            id = id,
            ownerId = null,
            name = name.trim(),
            status = ShoppingListStatus.ACTIVE,
            currency = currency,
            shoppingDate = shoppingDate,
            notes = notes,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )
        listDao.insertList(entity)
        return id
    }

    override suspend fun updateList(list: ShoppingList) {
        val entity = ShoppingListEntity(
            id = list.id,
            ownerId = list.ownerId,
            name = list.name,
            status = list.status,
            currency = list.currency,
            shoppingDate = list.shoppingDate,
            notes = list.notes,
            createdAt = list.createdAt,
            updatedAt = System.currentTimeMillis(),
            completedAt = list.completedAt
        )
        listDao.updateList(entity)
    }

    override suspend fun updateListStatus(id: String, status: ShoppingListStatus) {
        val existing = listDao.getListById(id) ?: return
        val completedAt = if (status == ShoppingListStatus.COMPLETED) System.currentTimeMillis() else null
        val updated = existing.copy(
            status = status,
            completedAt = completedAt,
            updatedAt = System.currentTimeMillis()
        )
        listDao.updateList(updated)
    }

    override suspend fun deleteList(id: String) {
        listDao.deleteListById(id)
    }

    override suspend fun duplicateList(sourceListId: String, newName: String, preservePrices: Boolean): String {
        val sourceList = listDao.getListById(sourceListId) ?: throw IllegalArgumentException("List not found")
        val sourceItems = itemDao.getItemsForList(sourceListId)

        val newListId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val newListEntity = ShoppingListEntity(
            id = newListId,
            ownerId = sourceList.ownerId,
            name = newName.trim(),
            status = ShoppingListStatus.ACTIVE,
            currency = sourceList.currency,
            shoppingDate = null,
            notes = sourceList.notes,
            createdAt = now,
            updatedAt = now,
            completedAt = null
        )
        listDao.insertList(newListEntity)

        val newItems = sourceItems.map { src ->
            ShoppingListItemEntity(
                id = UUID.randomUUID().toString(),
                shoppingListId = newListId,
                productId = src.productId,
                productNameSnapshot = src.productNameSnapshot,
                quantity = src.quantity,
                unit = src.unit,
                estimatedUnitPrice = if (preservePrices) (src.estimatedUnitPrice ?: src.actualUnitPrice) else null,
                actualUnitPrice = null,
                isPurchased = false,
                category = src.category,
                notes = src.notes,
                createdAt = now,
                purchasedAt = null
            )
        }
        itemDao.insertItems(newItems)
        return newListId
    }

    override fun getItemsForListFlow(listId: String): Flow<List<ShoppingListItem>> {
        return itemDao.getItemsForListFlow(listId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getItemsForList(listId: String): List<ShoppingListItem> {
        return itemDao.getItemsForList(listId).map { it.toDomain() }
    }

    override suspend fun addItem(item: ShoppingListItem) {
        itemDao.insertItem(item.toEntity())
        touchList(item.shoppingListId)
    }

    override suspend fun addItems(items: List<ShoppingListItem>) {
        if (items.isEmpty()) return
        itemDao.insertItems(items.map { it.toEntity() })
        touchList(items.first().shoppingListId)
    }

    override suspend fun updateItem(item: ShoppingListItem) {
        itemDao.updateItem(item.toEntity())
        touchList(item.shoppingListId)
    }

    override suspend fun deleteItem(id: String) {
        val item = itemDao.getItemById(id)
        itemDao.deleteItemById(id)
        item?.let { touchList(it.shoppingListId) }
    }

    override suspend fun togglePurchaseStatus(itemId: String, isPurchased: Boolean, actualPrice: Double?) {
        val purchasedAt = if (isPurchased) System.currentTimeMillis() else null
        itemDao.updatePurchaseStatus(itemId, isPurchased, actualPrice, purchasedAt)
        val item = itemDao.getItemById(itemId)
        item?.let { touchList(it.shoppingListId) }
    }

    override suspend fun updateCartStatus(itemId: String, isInCart: Boolean) {
        itemDao.updateCartStatus(itemId, isInCart)
        val item = itemDao.getItemById(itemId)
        item?.let { touchList(it.shoppingListId) }
    }

    override suspend fun updateItemThreeStage(
        itemId: String,
        stage: ro.smartshopping.list.domain.model.ItemPurchaseStage,
        actualPrice: Double?
    ) {
        val purchasedAt = if (stage == ro.smartshopping.list.domain.model.ItemPurchaseStage.PURCHASED) System.currentTimeMillis() else null
        val isPurchased = (stage == ro.smartshopping.list.domain.model.ItemPurchaseStage.PURCHASED)
        val isInCart = (stage == ro.smartshopping.list.domain.model.ItemPurchaseStage.IN_CART)
        itemDao.updateThreeStageStatus(itemId, isInCart, isPurchased, actualPrice, purchasedAt)
        val item = itemDao.getItemById(itemId)
        item?.let { touchList(it.shoppingListId) }
    }

    override suspend fun updateStoreAndBudget(
        id: String,
        storeName: String?,
        storeAddress: String?,
        lat: Double?,
        lng: Double?,
        budget: Double?,
        receiptTotal: Double?,
        discount: Double?,
        extraFees: Double?
    ) {
        listDao.updateStoreAndBudget(
            id = id,
            storeName = storeName,
            storeAddress = storeAddress,
            lat = lat,
            lng = lng,
            budget = budget,
            receiptTotal = receiptTotal,
            discount = discount,
            extraFees = extraFees
        )
    }

    private suspend fun touchList(listId: String) {
        val list = listDao.getListById(listId) ?: return
        listDao.updateList(list.copy(updatedAt = System.currentTimeMillis()))
    }

    private fun mapToDomainWithMetrics(entity: ShoppingListEntity, items: List<Any>): ShoppingList {
        val domainItems = items.map {
            when (it) {
                is ShoppingListItemEntity -> it.toDomain()
                is ShoppingListItem -> it
                else -> throw IllegalArgumentException()
            }
        }
        val summary = financialCalculator(
            items = domainItems,
            currency = entity.currency,
            budget = entity.budget,
            discountAmount = entity.discountAmount,
            extraFeesAmount = entity.extraFeesAmount,
            receiptTotal = entity.receiptTotal
        )
        return ShoppingList(
            id = entity.id,
            ownerId = entity.ownerId,
            name = entity.name,
            status = entity.status,
            currency = entity.currency,
            shoppingDate = entity.shoppingDate,
            notes = entity.notes,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            completedAt = entity.completedAt,
            storeName = entity.storeName,
            storeAddress = entity.storeAddress,
            storeLatitude = entity.storeLatitude,
            storeLongitude = entity.storeLongitude,
            budget = entity.budget,
            receiptTotal = entity.receiptTotal,
            discountAmount = entity.discountAmount,
            extraFeesAmount = entity.extraFeesAmount,
            totalItemsCount = summary.totalItemsCount,
            toBuyItemsCount = summary.toBuyItemsCount,
            inCartItemsCount = summary.inCartItemsCount,
            purchasedItemsCount = summary.purchasedItemsCount,
            completionPercentage = summary.completionRate,
            estimatedTotal = summary.estimatedTotal,
            actualTotal = summary.actualSpent
        )
    }

    private fun ShoppingListItemEntity.toDomain() = ShoppingListItem(
        id = id,
        shoppingListId = shoppingListId,
        productId = productId,
        productNameSnapshot = productNameSnapshot,
        quantity = quantity,
        unit = unit,
        estimatedUnitPrice = estimatedUnitPrice,
        actualUnitPrice = actualUnitPrice,
        isPurchased = isPurchased,
        category = category,
        notes = notes,
        createdAt = createdAt,
        purchasedAt = purchasedAt,
        isInCart = isInCart,
        packageCount = packageCount,
        storeName = storeName
    )

    private fun ShoppingListItem.toEntity() = ShoppingListItemEntity(
        id = id,
        shoppingListId = shoppingListId,
        productId = productId,
        productNameSnapshot = productNameSnapshot,
        quantity = quantity,
        unit = unit,
        estimatedUnitPrice = estimatedUnitPrice,
        actualUnitPrice = actualUnitPrice,
        isPurchased = isPurchased,
        category = category,
        notes = notes,
        createdAt = createdAt,
        purchasedAt = purchasedAt,
        isInCart = isInCart,
        packageCount = packageCount,
        storeName = storeName
    )
}
