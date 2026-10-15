package ro.smartshopping.list

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import ro.smartshopping.list.data.repository.StoreDealsRepository
import ro.smartshopping.list.data.repository.StoreDealsRepositoryImpl
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.usecase.SmartDiscountAgentUseCase
import java.util.UUID

class SmartDiscountAgentTest {

    private lateinit var dealsRepository: StoreDealsRepository
    private lateinit var agentUseCase: SmartDiscountAgentUseCase

    @Before
    fun setup() {
        dealsRepository = StoreDealsRepositoryImpl()
        agentUseCase = SmartDiscountAgentUseCase(dealsRepository)
    }

    private fun createItem(name: String, quantity: Double, unit: UnitOfMeasure = UnitOfMeasure.PCS) = ShoppingListItem(
        id = UUID.randomUUID().toString(),
        shoppingListId = "list_militari",
        productId = UUID.randomUUID().toString(),
        productNameSnapshot = name,
        quantity = quantity,
        unit = unit,
        estimatedUnitPrice = null,
        actualUnitPrice = null,
        isPurchased = false,
        isInCart = false
    )

    @Test
    fun testFindDealAndAisleLocationForKauflandAndAuchanMilitari() = runBlocking {
        // Lapte Zuzu is available in Kaufland Militari Residence (Str. Rezervelor 64)
        val kDeal = dealsRepository.findDealForProduct("Lapte Zuzu 3.5%", "Kaufland")
        assertNotNull(kDeal)
        assertTrue(kDeal!!.storeName.contains("Kaufland Militari Residence"))
        assertEquals(6.29, kDeal.discountPrice, 0.001)
        assertTrue(kDeal.aisleLocation.contains("Raion Lactate"))

        // Cafea Lavazza is available in Auchan Militari (Centrul Comercial Militari)
        val aDeal = dealsRepository.findDealForProduct("Cafea Boabe Lavazza", "Auchan")
        assertNotNull(aDeal)
        assertTrue(aDeal!!.storeName.contains("Auchan Militari"))
        assertEquals(43.99, aDeal.discountPrice, 0.001)
        assertTrue(aDeal.aisleLocation.contains("Aleea Centrală"))
    }

    @Test
    fun testAgentBasketComparisonAndBestStoreRecommendation() = runBlocking {
        val items = listOf(
            createItem("Lapte Zuzu 3.5%", 2.0, UnitOfMeasure.L),
            createItem("Pâine Toast Integrală", 1.0, UnitOfMeasure.PCS),
            createItem("Mere Idared Românești", 2.0, UnitOfMeasure.KG)
        )

        val summary = agentUseCase(items)

        assertEquals(3, summary.totalItemsCount)
        assertEquals(3, summary.matchedItemsCount)

        // Verify that savings were calculated
        assertTrue(summary.kauflandSavings > 0)
        assertTrue(summary.auchanSavings > 0)

        // Check recommendation has Militari store context
        assertTrue(summary.bestOverallStore.contains("Militari"))
        assertTrue(summary.recommendedStrategy.isNotBlank())

        // Check aisle location is populated for each item
        for (match in summary.matches) {
            assertNotNull(match.matchedDeal)
            assertNotNull(match.aisleLocation)
            assertTrue(match.aisleLocation!!.isNotBlank())
        }
    }

    @Test
    fun testAutomatedNotificationsFeed() = runBlocking {
        val initialNotifs = dealsRepository.getDealsMatchingList(listOf("Lapte Zuzu 3.5%", "Detergent Ariel Pods"))
        assertTrue(initialNotifs.isNotEmpty())

        val notifs = dealsRepository.getDealsByStoreFlow("Kaufland")
        assertNotNull(notifs)
    }
}
