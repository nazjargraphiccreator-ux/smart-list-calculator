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
        val metroBranch = "Bd. Iuliu Maniu 500, Militari"
        val lidlBranch = "Str. Valea Oltului 199, Militari"

        val catalog = listOf(
            // METRO MILITARI OFFICIAL DEALS
            StoreDeal(
                id = "md_1",
                storeName = "Metro Militari",
                storeBranch = metroBranch,
                productName = "Săpun Solid Protex/Dove (6 buc)",
                category = "Îngrijire personală",
                originalPrice = 24.99,
                discountPrice = 17.49,
                discountPercent = 30,
                unit = UnitOfMeasure.PACK,
                validUntil = "Valabil catalog săptămânal",
                aisleLocation = "Raion Cosmetice & Igienă • Palet Baxuri",
                badge = "Metro Multipack -30%",
                officialCatalogUrl = "https://www.metro.ro/oferte-si-promotii",
                officialAppName = "Metro Companion"
            ),
            StoreDeal(
                id = "md_2",
                storeName = "Metro Militari",
                storeBranch = metroBranch,
                productName = "Bulion Sultan 800g",
                category = "Alimente de bază",
                originalPrice = 12.99,
                discountPrice = 8.99,
                discountPercent = 30,
                unit = UnitOfMeasure.JAR,
                validUntil = "Valabil catalog săptămânal",
                aisleLocation = "Raion Conserve & Sosuri • Culoar 4",
                badge = "Metro Ofertă Volum",
                officialCatalogUrl = "https://www.metro.ro/oferte-si-promotii",
                officialAppName = "Metro Companion"
            ),
            StoreDeal(
                id = "md_3",
                storeName = "Metro Militari",
                storeBranch = metroBranch,
                productName = "Smântână Găletușă 1kg (20% grăsime)",
                category = "Lactate",
                originalPrice = 19.99,
                discountPrice = 14.99,
                discountPercent = 25,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil catalog săptămânal",
                aisleLocation = "Raion Lactate Gastro • Frigider 3",
                badge = "Metro Gastro -25%",
                officialCatalogUrl = "https://www.metro.ro/oferte-si-promotii",
                officialAppName = "Metro Companion"
            ),
            StoreDeal(
                id = "md_4",
                storeName = "Metro Militari",
                storeBranch = metroBranch,
                productName = "Salam Săsesc Cris-Tim 650g",
                category = "Carne & Mezeluri",
                originalPrice = 26.50,
                discountPrice = 19.80,
                discountPercent = 25,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil săptămâna aceasta",
                aisleLocation = "Raion Mezeluri • Vitrină Producători Români",
                badge = "Promo Cris-Tim -25%",
                officialCatalogUrl = "https://www.metro.ro/oferte-si-promotii",
                officialAppName = "Metro Companion"
            ),
            StoreDeal(
                id = "md_5",
                storeName = "Metro Militari",
                storeBranch = metroBranch,
                productName = "Orez Deroni Bob Rotund 1kg",
                category = "Alimente de bază",
                originalPrice = 8.99,
                discountPrice = 6.49,
                discountPercent = 28,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil săptămâna aceasta",
                aisleLocation = "Raion Băcănie • Culoar 7",
                badge = "Preț Special Bax",
                officialCatalogUrl = "https://www.metro.ro/oferte-si-promotii",
                officialAppName = "Metro Companion"
            ),
            StoreDeal(
                id = "md_6",
                storeName = "Metro Militari",
                storeBranch = metroBranch,
                productName = "Scăriță afumată de porc",
                category = "Carne & Mezeluri",
                originalPrice = 32.99,
                discountPrice = 24.99,
                discountPercent = 24,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil catalog curent",
                aisleLocation = "Carmangerie Metro • Raft Carne Afumată",
                badge = "Oferta Măcelarului -24%",
                officialCatalogUrl = "https://www.metro.ro/oferte-si-promotii",
                officialAppName = "Metro Companion"
            ),

            // KAUFLAND MILITARI DEALS
            StoreDeal(
                id = "kd_1",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "File de pui cu susan",
                category = "Carne & Mezeluri",
                originalPrice = 27.99,
                discountPrice = 19.99,
                discountPercent = 28,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil până duminică",
                aisleLocation = "Vitrina Asistată Carne • Culoar 2",
                badge = "Kaufland Card -28%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App"
            ),
            StoreDeal(
                id = "kd_2",
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
                badge = "Kaufland Card -26%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App"
            ),
            StoreDeal(
                id = "kd_3",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Cașcaval Rucăr / Dalia 400g",
                category = "Lactate",
                originalPrice = 18.99,
                discountPrice = 13.99,
                discountPercent = 26,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Brânzeturi • Raft 4",
                badge = "Kaufland Promo -26%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App"
            ),
            StoreDeal(
                id = "kd_4",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Iaurt Cremosso Danone 125g",
                category = "Lactate",
                originalPrice = 3.49,
                discountPrice = 2.49,
                discountPercent = 28,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Iaurturi • Frigider 1",
                badge = "Ofertă Cremosso -28%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App"
            ),
            StoreDeal(
                id = "kd_5",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Cartofi Albi Românești",
                category = "Fructe & Legume",
                originalPrice = 3.99,
                discountPrice = 2.49,
                discountPercent = 37,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil toată săptămâna",
                aisleLocation = "Piața Kaufland • Bazinul 1",
                badge = "Produs Românesc -37%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App"
            ),
            StoreDeal(
                id = "kd_6",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Fasole Boabe Uscată",
                category = "Fructe & Legume",
                originalPrice = 11.99,
                discountPrice = 8.49,
                discountPercent = 29,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Legume Uscate • Culoar 3",
                badge = "Super Preț",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App"
            ),
            StoreDeal(
                id = "kd_7",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Brânză de capră proaspătă",
                category = "Lactate",
                originalPrice = 38.99,
                discountPrice = 29.99,
                discountPercent = 23,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil până duminică",
                aisleLocation = "Vitrina cu Brânzeturi Tradiționale",
                badge = "Gourmet -23%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App"
            ),
            StoreDeal(
                id = "kd_8",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Pâine Toast Integrală",
                category = "Brutărie",
                originalPrice = 5.49,
                discountPrice = 3.79,
                discountPercent = 31,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil săptămâna aceasta",
                aisleLocation = "Brutăria Kaufland • Raft 1",
                badge = "Promo Brutărie",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App"
            ),
            StoreDeal(
                id = "kd_9",
                storeName = "Kaufland Militari",
                storeBranch = kauflandBranch,
                productName = "Mere Idared Românești",
                category = "Fructe & Legume",
                originalPrice = 4.99,
                discountPrice = 3.29,
                discountPercent = 34,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil toată săptămâna",
                aisleLocation = "Piața Kaufland • Raion Fructe & Legume",
                badge = "Produs Românesc -34%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App"
            ),

            // AUCHAN MILITARI OFFICIAL DEALS
            StoreDeal(
                id = "ad_0",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Lapte Zuzu 3.5%",
                category = "Lactate",
                originalPrice = 8.29,
                discountPrice = 6.49,
                discountPercent = 22,
                unit = UnitOfMeasure.L,
                validUntil = "Valabil săptămâna aceasta",
                aisleLocation = "Raion Lactate • Vitrina Frigorifică",
                badge = "Auchan Promo -22%",
                officialCatalogUrl = "https://www.auchan.ro/promotii",
                officialAppName = "MyAuchan App"
            ),
            StoreDeal(
                id = "ad_1",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Cofraj Ouă Mărimea M (30 buc)",
                category = "Lactate & Ouă",
                originalPrice = 35.99,
                discountPrice = 26.99,
                discountPercent = 25,
                unit = UnitOfMeasure.CARTON,
                validUntil = "Valabil până marți",
                aisleLocation = "Raion Ouă • Palet Central",
                badge = "Ofertă Cofraj 30 -25%",
                officialCatalogUrl = "https://www.auchan.ro/promotii",
                officialAppName = "MyAuchan App"
            ),
            StoreDeal(
                id = "ad_2",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Pâine Toast Integrală",
                category = "Brutărie",
                originalPrice = 5.29,
                discountPrice = 3.49,
                discountPercent = 34,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil azi și mâine",
                aisleLocation = "Raion Brutărie Proprie • Stand 2",
                badge = "Flash Deal -34%",
                officialCatalogUrl = "https://www.auchan.ro/promotii",
                officialAppName = "MyAuchan App"
            ),
            StoreDeal(
                id = "ad_3",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Detergent Rufe Ariel Pods 40buc",
                category = "Curățenie & Casă",
                originalPrice = 69.99,
                discountPrice = 45.99,
                discountPercent = 34,
                unit = UnitOfMeasure.PACK,
                validUntil = "Valabil până marți",
                aisleLocation = "Raion Curățenie • Raft 14",
                badge = "Auchan Flash -34%",
                officialCatalogUrl = "https://www.auchan.ro/promotii",
                officialAppName = "MyAuchan App"
            ),
            StoreDeal(
                id = "ad_4",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Apă Minerală Borsec 2L (6 sticle)",
                category = "Băuturi",
                originalPrice = 18.99,
                discountPrice = 13.99,
                discountPercent = 26,
                unit = UnitOfMeasure.BOTTLE,
                validUntil = "Valabil săptămâna aceasta",
                aisleLocation = "Raion Ape • Palet Promoții",
                badge = "Bax Promo -26%",
                officialCatalogUrl = "https://www.auchan.ro/promotii",
                officialAppName = "MyAuchan App"
            ),
            StoreDeal(
                id = "ad_5",
                storeName = "Auchan Militari",
                storeBranch = auchanBranch,
                productName = "Mere Idared Românești",
                category = "Fructe & Legume",
                originalPrice = 5.49,
                discountPrice = 3.49,
                discountPercent = 36,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil săptămâna aceasta",
                aisleLocation = "Piața Auchan • Fructe Proaspete",
                badge = "Auchan -36%",
                officialCatalogUrl = "https://www.auchan.ro/promotii",
                officialAppName = "MyAuchan App"
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
