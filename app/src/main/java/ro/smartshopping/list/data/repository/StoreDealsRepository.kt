package ro.smartshopping.list.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import ro.smartshopping.list.domain.model.StoreDeal
import ro.smartshopping.list.domain.model.StoreDealNotification
import ro.smartshopping.list.domain.model.UnitOfMeasure
import java.util.UUID

interface StoreDealsRepository {
    fun getAllDealsFlow(): Flow<List<StoreDeal>>
    fun getDealsByStoreFlow(storeName: String): Flow<List<StoreDeal>>
    fun getNotificationsFlow(): Flow<List<StoreDealNotification>>
    suspend fun findDealForProduct(productName: String, targetStore: String? = null): StoreDeal?
    suspend fun getDealsMatchingList(productNames: List<String>): List<StoreDeal>
    suspend fun addNotification(notification: StoreDealNotification)
    suspend fun markNotificationAsRead(id: String)
    suspend fun refreshDeals()
}

class StoreDealsRepositoryImpl : StoreDealsRepository {

    private val dealsState = MutableStateFlow<List<StoreDeal>>(emptyList())
    private val notificationsState = MutableStateFlow<List<StoreDealNotification>>(emptyList())

    init {
        loadCatalog()
        seedInitialNotifications()
    }

    override fun getAllDealsFlow(): Flow<List<StoreDeal>> = dealsState.asStateFlow()

    override fun getDealsByStoreFlow(storeName: String): Flow<List<StoreDeal>> {
        return dealsState.map { deals ->
            deals.filter { it.storeName.contains(storeName, ignoreCase = true) }
        }
    }

    override fun getNotificationsFlow(): Flow<List<StoreDealNotification>> = notificationsState.asStateFlow()

    override suspend fun findDealForProduct(productName: String, targetStore: String?): StoreDeal? {
        val normalized = productName.lowercase().trim()
        val allDeals = dealsState.value
        val filtered = if (targetStore != null) {
            allDeals.filter { it.storeName.contains(targetStore, ignoreCase = true) }
        } else {
            allDeals
        }

        return filtered.firstOrNull { deal ->
            val dealNorm = deal.productName.lowercase()
            normalized.contains(dealNorm) || dealNorm.contains(normalized) ||
                    fuzzyWordMatch(normalized, dealNorm)
        }
    }

    override suspend fun getDealsMatchingList(productNames: List<String>): List<StoreDeal> {
        val matches = mutableListOf<StoreDeal>()
        for (name in productNames) {
            val deal = findDealForProduct(name)
            if (deal != null) {
                matches.add(deal)
            }
        }
        return matches
    }

    override suspend fun addNotification(notification: StoreDealNotification) {
        val current = notificationsState.value.toMutableList()
        current.add(0, notification)
        notificationsState.value = current
    }

    override suspend fun markNotificationAsRead(id: String) {
        val current = notificationsState.value.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
        notificationsState.value = current
    }

    override suspend fun refreshDeals() {
        loadCatalog()
    }

    private fun fuzzyWordMatch(str1: String, str2: String): Boolean {
        val words1 = str1.split(" ").filter { it.length > 2 }
        val words2 = str2.split(" ").filter { it.length > 2 }
        return words1.any { w1 -> words2.any { w2 -> w1.equals(w2, ignoreCase = true) } }
    }

    private fun loadCatalog() {
        val kauflandBranch = "Str. Valea Cascadelor 3, Militari"
        val auchanBranch = "Centrul Comercial Militari, Bd. Iuliu Maniu 560A"

        val catalog = listOf(
            // KAUFLAND MILITARI DEALS
            StoreDeal(
                id = "kd_1",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Lapte Zuzu 3.5%",
                category = "Lactate",
                originalPrice = 8.49,
                discountPrice = 6.29,
                discountPercent = 26,
                unit = UnitOfMeasure.L,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Lactate • Frigider 2, Culoar A",
                badge = "Kaufland Card -26%"
            ),
            StoreDeal(
                id = "kd_2",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Pâine Toast Integrală",
                category = "Brutărie",
                originalPrice = 4.99,
                discountPrice = 3.69,
                discountPercent = 26,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Panificație • Raft 3",
                badge = "Super Preț"
            ),
            StoreDeal(
                id = "kd_3",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Ouă Proaspete Mărimea M (10 buc)",
                category = "Lactate & Ouă",
                originalPrice = 14.99,
                discountPrice = 10.99,
                discountPercent = 27,
                unit = UnitOfMeasure.CARTON,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Ouă • Insula Centrală",
                badge = "Promoție Weekend"
            ),
            StoreDeal(
                id = "kd_4",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Mere Idared Românești",
                category = "Fructe & Legume",
                originalPrice = 5.99,
                discountPrice = 3.99,
                discountPercent = 33,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Fructe • Bazin 4",
                badge = "Produs Românesc -33%"
            ),
            StoreDeal(
                id = "kd_5",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Cafea Boabe Jacobs Kronung 500g",
                category = "Băuturi calde",
                originalPrice = 34.99,
                discountPrice = 24.99,
                discountPercent = 29,
                unit = UnitOfMeasure.G,
                validUntil = "Valabil toată săptămâna",
                aisleLocation = "Raion Cafea • Raft 6, Culoar B",
                badge = "Top Brand -29%"
            ),
            StoreDeal(
                id = "kd_6",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Ulei Floarea-Soarelui Floriol 1L",
                category = "Băcănie",
                originalPrice = 9.49,
                discountPrice = 6.99,
                discountPercent = 26,
                unit = UnitOfMeasure.BOTTLE,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Băcănie • Raft 8, Mijloc",
                badge = "Ofertă Specială"
            ),
            StoreDeal(
                id = "kd_7",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Detergent Automat Ariel Pods 40buc",
                category = "Curățenie & Casă",
                originalPrice = 69.99,
                discountPrice = 49.99,
                discountPercent = 29,
                unit = UnitOfMeasure.PACK,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Detergenți • Culoar 11",
                badge = "Reducere Mare -29%"
            ),

            // AUCHAN MILITARI DEALS
            StoreDeal(
                id = "ad_1",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Lapte Zuzu 3.5%",
                category = "Lactate",
                originalPrice = 8.59,
                discountPrice = 6.49,
                discountPercent = 24,
                unit = UnitOfMeasure.L,
                validUntil = "Valabil până marți",
                aisleLocation = "Raion Lactate • Aleea Frigo 4",
                badge = "Ofertă MyAuchan"
            ),
            StoreDeal(
                id = "ad_2",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Pâine Toast Feliată Integrală",
                category = "Brutărie",
                originalPrice = 5.29,
                discountPrice = 3.49,
                discountPercent = 34,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil azi și mâine",
                aisleLocation = "Raion Brutărie Proprie • Stand 2",
                badge = "Flash Deal -34%"
            ),
            StoreDeal(
                id = "ad_3",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Ouă Proaspete Mărimea M (10 buc)",
                category = "Lactate & Ouă",
                originalPrice = 14.50,
                discountPrice = 11.20,
                discountPercent = 23,
                unit = UnitOfMeasure.CARTON,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Ouă • Palet 1",
                badge = "Preț Redus"
            ),
            StoreDeal(
                id = "ad_4",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Mere Golden & Idared",
                category = "Fructe & Legume",
                originalPrice = 5.79,
                discountPrice = 3.49,
                discountPercent = 40,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil toată săptămâna",
                aisleLocation = "Piața Auchan Fructe • Insula 3",
                badge = "Super Ofertă -40%"
            ),
            StoreDeal(
                id = "ad_5",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Detergent Rufe Ariel Pods 40buc",
                category = "Curățenie & Casă",
                originalPrice = 69.99,
                discountPrice = 45.99,
                discountPercent = 34,
                unit = UnitOfMeasure.PACK,
                validUntil = "Valabil până marți",
                aisleLocation = "Raion Curățenie • Raft 14, Capăt de rând",
                badge = "Auchan Flash -34%"
            ),
            StoreDeal(
                id = "ad_6",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Apă Minerală Borsec 2L (6 sticle)",
                category = "Băuturi",
                originalPrice = 18.99,
                discountPrice = 13.99,
                discountPercent = 26,
                unit = UnitOfMeasure.BOTTLE,
                validUntil = "Valabil săptămâna aceasta",
                aisleLocation = "Raion Ape & Răcoritoare • Palet Promoții",
                badge = "Bax Promo -26%"
            ),
            StoreDeal(
                id = "ad_7",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Piept de Pui Dezosat Fragedo",
                category = "Carne & Mezeluri",
                originalPrice = 28.99,
                discountPrice = 21.99,
                discountPercent = 24,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil azi",
                aisleLocation = "Măcelăria Auchan • Vitrină frigorifică",
                badge = "Carne Proaspătă -24%"
            )
        )

        dealsState.value = catalog
    }

    private fun seedInitialNotifications() {
        val notifications = listOf(
            StoreDealNotification(
                id = UUID.randomUUID().toString(),
                storeName = "Kaufland Militari",
                title = "Reduceri active la Kaufland Valea Cascadelor!",
                message = "Produsele tale uzuale (Lapte Zuzu, Cafea Jacobs, Ouă) au reduceri de până la -33% săptămâna aceasta.",
                discountHighlight = "-33% la Lactate & Cafea",
                timestamp = System.currentTimeMillis() - 1000L * 60 * 30 // 30 min ago
            ),
            StoreDealNotification(
                id = UUID.randomUUID().toString(),
                storeName = "Auchan Militari",
                title = "Ofertă Flash la Auchan Militari (Bd. Iuliu Maniu)!",
                message = "Super reducere -34% la Detergent Ariel și -40% la Mere. Stoc limitat în magazinul din Militari.",
                discountHighlight = "-40% Fructe & -34% Detergent",
                timestamp = System.currentTimeMillis() - 1000L * 60 * 120 // 2h ago
            )
        )
        notificationsState.value = notifications
    }
}
