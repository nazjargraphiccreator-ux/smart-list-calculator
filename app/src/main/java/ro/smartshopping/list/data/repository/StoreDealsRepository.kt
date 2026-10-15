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
        val kauflandRezervelor = "Str. Rezervelor 64, Chiajna (Militari Residence)"
        val auchanMilitari = "Centrul Comercial Militari, Bd. Iuliu Maniu 560A (Intrare Chiajna)"
        val lidlTineretului = "Str. Tineretului 4 / Str. Rezervelor, Chiajna (Militari Residence)"
        val pennyApeductului = "Str. Apeductului 62 / Str. Rezervelor, Chiajna"
        val metroMilitari = "Bd. Iuliu Maniu 500, Militari (la 3 min de Militari Residence)"
        val megaTineretului = "Str. Tineretului 2 / Ballroom Militari Residence"

        val catalog = listOf(
            // ==========================================
            // 1. KAUFLAND MILITARI RESIDENCE (CHIAJNA)
            // ==========================================
            StoreDeal(
                id = "kchiajna_1",
                storeName = "Kaufland Militari Residence",
                storeBranch = kauflandRezervelor,
                productName = "Lapte Zuzu 3.5% 1L",
                category = "Lactate",
                originalPrice = 8.49,
                discountPrice = 6.29,
                discountPercent = 26,
                unit = UnitOfMeasure.L,
                validUntil = "Valabil până duminică",
                aisleLocation = "Raion Lactate • Frigider 2, Culoar A (lângă iaurturi)",
                badge = "Kaufland Card -26%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App",
                proximityText = "📍 La 200m de Militari Residence (Mers pe jos)",
                googleMapsUrl = "https://maps.google.com/?q=Kaufland+Chiajna+Str+Rezervelor+64"
            ),
            StoreDeal(
                id = "kchiajna_2",
                storeName = "Kaufland Militari Residence",
                storeBranch = kauflandRezervelor,
                productName = "File de pui cu susan / Piept pui dezosat",
                category = "Carne & Mezeluri",
                originalPrice = 27.99,
                discountPrice = 19.99,
                discountPercent = 28,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil până duminică",
                aisleLocation = "Vitrina Asistată Carmangerie • Culoar 2",
                badge = "Kaufland Card -28%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App",
                proximityText = "📍 La 200m de Militari Residence (Mers pe jos)",
                googleMapsUrl = "https://maps.google.com/?q=Kaufland+Chiajna+Str+Rezervelor+64"
            ),
            StoreDeal(
                id = "kchiajna_3",
                storeName = "Kaufland Militari Residence",
                storeBranch = kauflandRezervelor,
                productName = "Cartofi Albi Românești",
                category = "Fructe & Legume",
                originalPrice = 3.99,
                discountPrice = 2.49,
                discountPercent = 37,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil toată săptămâna",
                aisleLocation = "Piața Kaufland • Bazinul 1 (Chiar la intrare)",
                badge = "Produs Românesc -37%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App",
                proximityText = "📍 La 200m de Militari Residence (Mers pe jos)",
                googleMapsUrl = "https://maps.google.com/?q=Kaufland+Chiajna+Str+Rezervelor+64"
            ),
            StoreDeal(
                id = "kchiajna_4",
                storeName = "Kaufland Militari Residence",
                storeBranch = kauflandRezervelor,
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
                officialAppName = "Kaufland App",
                proximityText = "📍 La 200m de Militari Residence (Mers pe jos)",
                googleMapsUrl = "https://maps.google.com/?q=Kaufland+Chiajna+Str+Rezervelor+64"
            ),
            StoreDeal(
                id = "kchiajna_5",
                storeName = "Kaufland Militari Residence",
                storeBranch = kauflandRezervelor,
                productName = "Pâine Toast Integrală",
                category = "Brutărie",
                originalPrice = 5.49,
                discountPrice = 3.79,
                discountPercent = 31,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil toată săptămâna",
                aisleLocation = "Brutăria Kaufland • Raft Pâine Caldă & Toast",
                badge = "Brutărie -31%",
                officialCatalogUrl = "https://www.kaufland.ro/oferte.html",
                officialAppName = "Kaufland App",
                proximityText = "📍 La 200m de Militari Residence (Mers pe jos)",
                googleMapsUrl = "https://maps.google.com/?q=Kaufland+Chiajna+Str+Rezervelor+64"
            ),
            StoreDeal(
                id = "kchiajna_6",
                storeName = "Kaufland Militari Residence",
                storeBranch = kauflandRezervelor,
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
                officialAppName = "Kaufland App",
                proximityText = "📍 La 200m de Militari Residence (Mers pe jos)",
                googleMapsUrl = "https://maps.google.com/?q=Kaufland+Chiajna+Str+Rezervelor+64"
            ),
            StoreDeal(
                id = "kchiajna_7",
                storeName = "Kaufland Militari Residence",
                storeBranch = kauflandRezervelor,
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
                officialAppName = "Kaufland App",
                proximityText = "📍 La 200m de Militari Residence (Mers pe jos)",
                googleMapsUrl = "https://maps.google.com/?q=Kaufland+Chiajna+Str+Rezervelor+64"
            ),

            // ==========================================
            // 2. AUCHAN MILITARI (CENTRUL COMERCIAL MILITARI)
            // ==========================================
            StoreDeal(
                id = "auchan_1",
                storeName = "Auchan Militari",
                storeBranch = auchanMilitari,
                productName = "Cafea Boabe Lavazza Crema e Gusto 1kg",
                category = "Băuturi & Cafea",
                originalPrice = 64.99,
                discountPrice = 43.99,
                discountPercent = 32,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil catalog MyClub Auchan",
                aisleLocation = "Aleea Centrală Promoțională • Culoar 1 (Față Casierii)",
                badge = "MyClub Auchan -32%",
                officialCatalogUrl = "https://romania.auchan.ro/promotii",
                officialAppName = "MyClub Auchan",
                proximityText = "📍 La 900m de Militari Residence (2 min cu mașina)",
                googleMapsUrl = "https://maps.google.com/?q=Auchan+Militari+Bd+Iuliu+Maniu+560A"
            ),
            StoreDeal(
                id = "auchan_2",
                storeName = "Auchan Militari",
                storeBranch = auchanMilitari,
                productName = "Ulei de Măsline Extra Virgin Monini Classico 1L",
                category = "Alimente de bază",
                originalPrice = 52.99,
                discountPrice = 36.99,
                discountPercent = 30,
                unit = UnitOfMeasure.L,
                validUntil = "Valabil toată săptămâna",
                aisleLocation = "Raion Ulei & Oțet • Culoar 12, Raft 3",
                badge = "Promo Monini -30%",
                officialCatalogUrl = "https://romania.auchan.ro/promotii",
                officialAppName = "MyClub Auchan",
                proximityText = "📍 La 900m de Militari Residence (2 min cu mașina)",
                googleMapsUrl = "https://maps.google.com/?q=Auchan+Militari+Bd+Iuliu+Maniu+560A"
            ),
            StoreDeal(
                id = "auchan_3",
                storeName = "Auchan Militari",
                storeBranch = auchanMilitari,
                productName = "Telemea de Vacă Proaspătă Gusturi Românești 500g",
                category = "Lactate",
                originalPrice = 24.99,
                discountPrice = 17.99,
                discountPercent = 28,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil până duminică",
                aisleLocation = "Vitrina Asistată Brânzeturi Tradiționale • Culoar 3",
                badge = "Gusturi Românești -28%",
                officialCatalogUrl = "https://romania.auchan.ro/promotii",
                officialAppName = "MyClub Auchan",
                proximityText = "📍 La 900m de Militari Residence (2 min cu mașina)",
                googleMapsUrl = "https://maps.google.com/?q=Auchan+Militari+Bd+Iuliu+Maniu+560A"
            ),
            StoreDeal(
                id = "auchan_4",
                storeName = "Auchan Militari",
                storeBranch = auchanMilitari,
                productName = "Păstrăv Eviscerat Proaspăt",
                category = "Carne & Pește",
                originalPrice = 34.99,
                discountPrice = 25.99,
                discountPercent = 25,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil săptămâna aceasta",
                aisleLocation = "Raion Pescărie Asistată • Pe gheață, Culoar 1",
                badge = "Pescărie Proaspătă -25%",
                officialCatalogUrl = "https://romania.auchan.ro/promotii",
                officialAppName = "MyClub Auchan",
                proximityText = "📍 La 900m de Militari Residence (2 min cu mașina)",
                googleMapsUrl = "https://maps.google.com/?q=Auchan+Militari+Bd+Iuliu+Maniu+560A"
            ),
            StoreDeal(
                id = "auchan_5",
                storeName = "Auchan Militari",
                storeBranch = auchanMilitari,
                productName = "Apă Plată Borsec 2L (bax 6 buc)",
                category = "Băuturi",
                originalPrice = 21.00,
                discountPrice = 15.90,
                discountPercent = 24,
                unit = UnitOfMeasure.PACK,
                validUntil = "Valabil catalog curent",
                aisleLocation = "Raion Ape & Răcoritoare • Palet Promo Intrare",
                badge = "Ofertă Bax -24%",
                officialCatalogUrl = "https://romania.auchan.ro/promotii",
                officialAppName = "MyClub Auchan",
                proximityText = "📍 La 900m de Militari Residence (2 min cu mașina)",
                googleMapsUrl = "https://maps.google.com/?q=Auchan+Militari+Bd+Iuliu+Maniu+560A"
            ),

            // ==========================================
            // 3. LIDL MILITARI RESIDENCE (CHIAJNA - TINERETULUI)
            // ==========================================
            StoreDeal(
                id = "lidlchiajna_1",
                storeName = "Lidl Militari Residence",
                storeBranch = lidlTineretului,
                productName = "Banane Calitatea I",
                category = "Fructe & Legume",
                originalPrice = 6.99,
                discountPrice = 4.69,
                discountPercent = 33,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil joi - duminică",
                aisleLocation = "Piața Lidl • Stand Central Fructe (Intrare)",
                badge = "Lidl Plus -33%",
                officialCatalogUrl = "https://www.lidl.ro/cataloage-saptamanale",
                officialAppName = "Lidl Plus",
                proximityText = "📍 La 150m de Militari Residence (Chiar în cartier)",
                googleMapsUrl = "https://maps.google.com/?q=Lidl+Militari+Residence+Chiajna+Str+Tineretului"
            ),
            StoreDeal(
                id = "lidlchiajna_2",
                storeName = "Lidl Militari Residence",
                storeBranch = lidlTineretului,
                productName = "Ciabatta cu Măsline Proaspătă",
                category = "Brutărie",
                originalPrice = 3.99,
                discountPrice = 2.99,
                discountPercent = 25,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil în limita stocului",
                aisleLocation = "Brutăria Lidl • Cuptorul Central",
                badge = "Cuptorul Lidl -25%",
                officialCatalogUrl = "https://www.lidl.ro/cataloage-saptamanale",
                officialAppName = "Lidl Plus",
                proximityText = "📍 La 150m de Militari Residence (Chiar în cartier)",
                googleMapsUrl = "https://maps.google.com/?q=Lidl+Militari+Residence+Chiajna+Str+Tineretului"
            ),
            StoreDeal(
                id = "lidlchiajna_3",
                storeName = "Lidl Militari Residence",
                storeBranch = lidlTineretului,
                productName = "Iaurt Grecesc Pilos 10% 1kg",
                category = "Lactate",
                originalPrice = 14.99,
                discountPrice = 11.29,
                discountPercent = 24,
                unit = UnitOfMeasure.KG,
                validUntil = "Valabil catalog curent",
                aisleLocation = "Frigider Pilos • Culoar 1, Raft 2",
                badge = "Pilos Promo -24%",
                officialCatalogUrl = "https://www.lidl.ro/cataloage-saptamanale",
                officialAppName = "Lidl Plus",
                proximityText = "📍 La 150m de Militari Residence (Chiar în cartier)",
                googleMapsUrl = "https://maps.google.com/?q=Lidl+Militari+Residence+Chiajna+Str+Tineretului"
            ),
            StoreDeal(
                id = "lidlchiajna_4",
                storeName = "Lidl Militari Residence",
                storeBranch = lidlTineretului,
                productName = "Unt Pilos 82% Grăsime 200g",
                category = "Lactate",
                originalPrice = 11.99,
                discountPrice = 8.89,
                discountPercent = 26,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil catalog curent",
                aisleLocation = "Frigider Pilos • Culoar 1, Raft 3",
                badge = "Lidl Plus -26%",
                officialCatalogUrl = "https://www.lidl.ro/cataloage-saptamanale",
                officialAppName = "Lidl Plus",
                proximityText = "📍 La 150m de Militari Residence (Chiar în cartier)",
                googleMapsUrl = "https://maps.google.com/?q=Lidl+Militari+Residence+Chiajna+Str+Tineretului"
            ),
            StoreDeal(
                id = "lidlchiajna_5",
                storeName = "Lidl Militari Residence",
                storeBranch = lidlTineretului,
                productName = "Mușchi File Afumat Pikok 100g",
                category = "Carne & Mezeluri",
                originalPrice = 5.99,
                discountPrice = 4.19,
                discountPercent = 30,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil catalog curent",
                aisleLocation = "Raion Mezeluri Pikok • Culoar 2",
                badge = "Pikok -30%",
                officialCatalogUrl = "https://www.lidl.ro/cataloage-saptamanale",
                officialAppName = "Lidl Plus",
                proximityText = "📍 La 150m de Militari Residence (Chiar în cartier)",
                googleMapsUrl = "https://maps.google.com/?q=Lidl+Militari+Residence+Chiajna+Str+Tineretului"
            ),

            // ==========================================
            // 4. PENNY CHIAJNA (STR. APEDUCTULUI / REZERVELOR)
            // ==========================================
            StoreDeal(
                id = "penny_1",
                storeName = "Penny Chiajna",
                storeBranch = pennyApeductului,
                productName = "Ceapă Galbenă Românească (sac 2kg)",
                category = "Fructe & Legume",
                originalPrice = 7.99,
                discountPrice = 5.19,
                discountPercent = 35,
                unit = UnitOfMeasure.PACK,
                validUntil = "Valabil săptămâna aceasta",
                aisleLocation = "Piața Penny • Bazinul de Intrare Legume",
                badge = "Penny Card -35%",
                officialCatalogUrl = "https://www.penny.ro/oferte",
                officialAppName = "Penny Card",
                proximityText = "📍 La 400m de Militari Residence",
                googleMapsUrl = "https://maps.google.com/?q=Penny+Chiajna+Str+Apeductului"
            ),
            StoreDeal(
                id = "penny_2",
                storeName = "Penny Chiajna",
                storeBranch = pennyApeductului,
                productName = "Cotlet de Porc Fără Os Feliat 500g",
                category = "Carne & Mezeluri",
                originalPrice = 18.99,
                discountPrice = 14.19,
                discountPercent = 25,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil până duminică",
                aisleLocation = "Măcelăria Hanul Boieresc • Vitrina Frigorifică",
                badge = "Hanul Boieresc -25%",
                officialCatalogUrl = "https://www.penny.ro/oferte",
                officialAppName = "Penny Card",
                proximityText = "📍 La 400m de Militari Residence",
                googleMapsUrl = "https://maps.google.com/?q=Penny+Chiajna+Str+Apeductului"
            ),
            StoreDeal(
                id = "penny_3",
                storeName = "Penny Chiajna",
                storeBranch = pennyApeductului,
                productName = "Smântână Boni 20% 850g",
                category = "Lactate",
                originalPrice = 12.99,
                discountPrice = 9.99,
                discountPercent = 23,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil catalog curent",
                aisleLocation = "Raion Lactate Boni • Frigider 1",
                badge = "Boni -23%",
                officialCatalogUrl = "https://www.penny.ro/oferte",
                officialAppName = "Penny Card",
                proximityText = "📍 La 400m de Militari Residence",
                googleMapsUrl = "https://maps.google.com/?q=Penny+Chiajna+Str+Apeductului"
            ),

            // ==========================================
            // 5. METRO MILITARI (BD. IULIU MANIU 500)
            // ==========================================
            StoreDeal(
                id = "metro_1",
                storeName = "Metro Militari",
                storeBranch = metroMilitari,
                productName = "Bulion Sultan 800g",
                category = "Alimente de bază",
                originalPrice = 12.99,
                discountPrice = 8.99,
                discountPercent = 30,
                unit = UnitOfMeasure.JAR,
                validUntil = "Valabil catalog săptămânal",
                aisleLocation = "Raion Conserve & Sosuri • Culoar 4",
                badge = "Metro Ofertă Volum -30%",
                officialCatalogUrl = "https://www.metro.ro/oferte-si-promotii",
                officialAppName = "Metro Companion",
                proximityText = "📍 La 1.8 km de Militari Residence (3 min cu mașina)",
                googleMapsUrl = "https://maps.google.com/?q=Metro+Cash+Carry+Militari+Bd+Iuliu+Maniu+500"
            ),
            StoreDeal(
                id = "metro_2",
                storeName = "Metro Militari",
                storeBranch = metroMilitari,
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
                officialAppName = "Metro Companion",
                proximityText = "📍 La 1.8 km de Militari Residence (3 min cu mașina)",
                googleMapsUrl = "https://maps.google.com/?q=Metro+Cash+Carry+Militari+Bd+Iuliu+Maniu+500"
            ),
            StoreDeal(
                id = "metro_3",
                storeName = "Metro Militari",
                storeBranch = metroMilitari,
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
                officialAppName = "Metro Companion",
                proximityText = "📍 La 1.8 km de Militari Residence (3 min cu mașina)",
                googleMapsUrl = "https://maps.google.com/?q=Metro+Cash+Carry+Militari+Bd+Iuliu+Maniu+500"
            ),
            StoreDeal(
                id = "metro_4",
                storeName = "Metro Militari",
                storeBranch = metroMilitari,
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
                officialAppName = "Metro Companion",
                proximityText = "📍 La 1.8 km de Militari Residence (3 min cu mașina)",
                googleMapsUrl = "https://maps.google.com/?q=Metro+Cash+Carry+Militari+Bd+Iuliu+Maniu+500"
            ),

            // ==========================================
            // 6. MEGA IMAGE MILITARI RESIDENCE (BALLROOM)
            // ==========================================
            StoreDeal(
                id = "megachiajna_1",
                storeName = "Mega Image Militari Residence",
                storeBranch = megaTineretului,
                productName = "Iaurt din Lapte de Capră Gusturi Românești 300g",
                category = "Lactate",
                originalPrice = 8.99,
                discountPrice = 6.99,
                discountPercent = 22,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil în limita stocului",
                aisleLocation = "Stand Gusturi Românești • Culoar Central",
                badge = "Mega Promo -22%",
                officialCatalogUrl = "https://www.mega-image.ro/promotii",
                officialAppName = "Connect Mega Image",
                proximityText = "📍 La 100m de Militari Residence (Lângă Ballroom)",
                googleMapsUrl = "https://maps.google.com/?q=Mega+Image+Militari+Residence+Str+Tineretului"
            ),
            StoreDeal(
                id = "megachiajna_2",
                storeName = "Mega Image Militari Residence",
                storeBranch = megaTineretului,
                productName = "Salată Caesar Proaspătă Mega Apetit",
                category = "Fresh & Ready",
                originalPrice = 18.99,
                discountPrice = 14.25,
                discountPercent = 25,
                unit = UnitOfMeasure.PCS,
                validUntil = "Valabil azi",
                aisleLocation = "Vitrina Fresh Food • Lângă Casierii",
                badge = "Fresh Deal -25%",
                officialCatalogUrl = "https://www.mega-image.ro/promotii",
                officialAppName = "Connect Mega Image",
                proximityText = "📍 La 100m de Militari Residence (Lângă Ballroom)",
                googleMapsUrl = "https://maps.google.com/?q=Mega+Image+Militari+Residence+Str+Tineretului"
            )
        )

        dealsState.value = catalog
    }

    private fun seedInitialNotifications() {
        val initialNotifications = listOf(
            StoreDealNotification(
                id = "notif_chiajna_1",
                storeName = "Kaufland Militari Residence",
                title = "Reduceri active la 200m de tine!",
                message = "Lapte Zuzu 3.5% la 6.29 Lei (-26%) și Cartofi albi românești la 2.49 Lei (-37%) pe Str. Rezervelor 64.",
                discountHighlight = "-37% Kaufland Rezervelor"
            ),
            StoreDealNotification(
                id = "notif_chiajna_2",
                storeName = "Lidl Militari Residence",
                title = "Piața Lidl Chiajna - Ofertă Fresh",
                message = "Banane Calitatea I la 4.69 Lei/kg și Unt Pilos 82% la 8.89 Lei pe Str. Tineretului 4.",
                discountHighlight = "-33% Lidl Tineretului"
            ),
            StoreDealNotification(
                id = "notif_chiajna_3",
                storeName = "Auchan Militari",
                title = "Ofertă Cafea & Ulei de Măsline",
                message = "Cafea Lavazza Crema e Gusto 1kg la 43.99 Lei (-32%) la Centrul Comercial Militari.",
                discountHighlight = "-32% Auchan Militari"
            ),
            StoreDealNotification(
                id = "notif_chiajna_4",
                storeName = "Metro Militari",
                title = "Prețuri speciale de volum",
                message = "Bulion Sultan 800g la 8.99 Lei și Salam Săsesc Cris-Tim 650g la 19.80 Lei pe Bd. Iuliu Maniu 500.",
                discountHighlight = "-30% Metro Volum"
            )
        )
        notificationsState.value = initialNotifications
    }
}
