package ro.smartshopping.list.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ro.smartshopping.list.data.local.dao.LoyaltyCardDao
import ro.smartshopping.list.data.local.entity.LoyaltyCardEntity
import ro.smartshopping.list.domain.model.LoyaltyCard

interface LoyaltyCardRepository {
    fun getAllCardsFlow(): Flow<List<LoyaltyCard>>
    fun getFavoriteCardsFlow(): Flow<List<LoyaltyCard>>
    suspend fun getCardById(id: String): LoyaltyCard?
    suspend fun getCardForStore(storeName: String): LoyaltyCard?
    suspend fun saveCard(card: LoyaltyCard)
    suspend fun deleteCard(id: String)
    suspend fun toggleFavorite(id: String, isFavorite: Boolean)
    suspend fun markCardScanned(id: String)
}

class LoyaltyCardRepositoryImpl(
    private val loyaltyCardDao: LoyaltyCardDao
) : LoyaltyCardRepository {

    override fun getAllCardsFlow(): Flow<List<LoyaltyCard>> {
        return loyaltyCardDao.getAllLoyaltyCardsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getFavoriteCardsFlow(): Flow<List<LoyaltyCard>> {
        return loyaltyCardDao.getFavoriteLoyaltyCardsFlow().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getCardById(id: String): LoyaltyCard? {
        return loyaltyCardDao.getLoyaltyCardById(id)?.toDomain()
    }

    override suspend fun getCardForStore(storeName: String): LoyaltyCard? {
        return loyaltyCardDao.getLoyaltyCardForStore(storeName)?.toDomain()
    }

    override suspend fun saveCard(card: LoyaltyCard) {
        loyaltyCardDao.insertLoyaltyCard(card.toEntity())
    }

    override suspend fun deleteCard(id: String) {
        loyaltyCardDao.deleteLoyaltyCardById(id)
    }

    override suspend fun toggleFavorite(id: String, isFavorite: Boolean) {
        loyaltyCardDao.updateFavorite(id, isFavorite)
    }

    override suspend fun markCardScanned(id: String) {
        loyaltyCardDao.updateLastScanned(id)
    }

    private fun LoyaltyCardEntity.toDomain() = LoyaltyCard(
        id = id,
        storeName = storeName,
        programName = programName,
        cardNumber = cardNumber,
        barcodeType = barcodeType,
        primaryColorHex = primaryColorHex,
        secondaryColorHex = secondaryColorHex,
        cardHolderName = cardHolderName,
        notes = notes,
        isFavorite = isFavorite,
        createdAt = createdAt,
        lastScannedAt = lastScannedAt
    )

    private fun LoyaltyCard.toEntity() = LoyaltyCardEntity(
        id = id,
        storeName = storeName,
        programName = programName,
        cardNumber = cardNumber,
        barcodeType = barcodeType,
        primaryColorHex = primaryColorHex,
        secondaryColorHex = secondaryColorHex,
        cardHolderName = cardHolderName,
        notes = notes,
        isFavorite = isFavorite,
        createdAt = createdAt,
        lastScannedAt = lastScannedAt
    )
}
