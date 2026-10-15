package ro.smartshopping.list.domain.usecase

import ro.smartshopping.list.data.repository.StoreDealsRepository
import ro.smartshopping.list.domain.model.AgentMatchResult
import ro.smartshopping.list.domain.model.BasketComparisonSummary
import ro.smartshopping.list.domain.model.ShoppingListItem
import java.math.BigDecimal
import java.math.RoundingMode

class SmartDiscountAgentUseCase(
    private val dealsRepository: StoreDealsRepository
) {

    suspend operator fun invoke(items: List<ShoppingListItem>): BasketComparisonSummary {
        val matches = mutableListOf<AgentMatchResult>()

        var kauflandTotalBd = BigDecimal.ZERO
        var kauflandSavingsBd = BigDecimal.ZERO
        var auchanTotalBd = BigDecimal.ZERO
        var auchanSavingsBd = BigDecimal.ZERO
        var matchedCount = 0

        for (item in items) {
            val kDeal = dealsRepository.findDealForProduct(item.productNameSnapshot, "Kaufland")
            val aDeal = dealsRepository.findDealForProduct(item.productNameSnapshot, "Auchan")

            val bestDeal = when {
                kDeal != null && aDeal != null -> if (kDeal.discountPrice <= aDeal.discountPrice) kDeal else aDeal
                kDeal != null -> kDeal
                aDeal != null -> aDeal
                else -> null
            }

            val qtyBd = BigDecimal.valueOf(item.quantity)
            val fallbackPriceBd = BigDecimal.valueOf(item.estimatedUnitPrice ?: item.actualUnitPrice ?: 0.0)

            // Kaufland pricing for this item
            val kPrice = kDeal?.discountPrice ?: item.estimatedUnitPrice ?: item.actualUnitPrice ?: 0.0
            val kItemTotal = qtyBd.multiply(BigDecimal.valueOf(kPrice))
            kauflandTotalBd = kauflandTotalBd.add(kItemTotal)
            if (kDeal != null && kDeal.originalPrice > kDeal.discountPrice) {
                val savingPerUnit = BigDecimal.valueOf(kDeal.originalPrice - kDeal.discountPrice)
                kauflandSavingsBd = kauflandSavingsBd.add(qtyBd.multiply(savingPerUnit))
            }

            // Auchan pricing for this item
            val aPrice = aDeal?.discountPrice ?: item.estimatedUnitPrice ?: item.actualUnitPrice ?: 0.0
            val aItemTotal = qtyBd.multiply(BigDecimal.valueOf(aPrice))
            auchanTotalBd = auchanTotalBd.add(aItemTotal)
            if (aDeal != null && aDeal.originalPrice > aDeal.discountPrice) {
                val savingPerUnit = BigDecimal.valueOf(aDeal.originalPrice - aDeal.discountPrice)
                auchanSavingsBd = auchanSavingsBd.add(qtyBd.multiply(savingPerUnit))
            }

            val unitSavings = if (bestDeal != null) bestDeal.originalPrice - bestDeal.discountPrice else 0.0
            val totalSavings = unitSavings * item.quantity

            if (bestDeal != null) {
                matchedCount++
            }

            matches.add(
                AgentMatchResult(
                    item = item,
                    matchedDeal = bestDeal,
                    kauflandDeal = kDeal,
                    auchanDeal = aDeal,
                    bestStore = bestDeal?.storeName,
                    unitSavings = unitSavings,
                    totalSavings = totalSavings,
                    aisleLocation = bestDeal?.aisleLocation ?: "Informații raft indisponibile"
                )
            )
        }

        val kTotal = kauflandTotalBd.setScale(2, RoundingMode.HALF_UP).toDouble()
        val kSavings = kauflandSavingsBd.setScale(2, RoundingMode.HALF_UP).toDouble()
        val aTotal = auchanTotalBd.setScale(2, RoundingMode.HALF_UP).toDouble()
        val aSavings = auchanSavingsBd.setScale(2, RoundingMode.HALF_UP).toDouble()

        val bestOverallStore: String
        val recommendedStrategy: String

        if (items.isEmpty()) {
            bestOverallStore = "Kaufland Militari & Auchan Militari"
            recommendedStrategy = "Adaugă produse în listă pentru ca Agentul să calculeze cele mai bune prețuri din Militari."
        } else if (kTotal < aTotal) {
            val diff = aTotal - kTotal
            bestOverallStore = "Kaufland Militari (Valea Cascadelor)"
            recommendedStrategy = "Economisești ${String.format("%.2f", diff)} lei cumpărând coșul integral de la Kaufland Militari! Produsele din lista ta au reduceri active."
        } else if (aTotal < kTotal) {
            val diff = kTotal - aTotal
            bestOverallStore = "Auchan Militari (Centrul Comercial)"
            recommendedStrategy = "Economisești ${String.format("%.2f", diff)} lei cumpărând coșul integral de la Auchan Militari! Profită de ofertele promoționale."
        } else {
            bestOverallStore = "Prețuri egale la Kaufland & Auchan"
            recommendedStrategy = "Ambele magazine din Militari oferă prețuri similare. Poți alege magazinul cel mai apropiat."
        }

        return BasketComparisonSummary(
            totalItemsCount = items.size,
            matchedItemsCount = matchedCount,
            kauflandTotal = kTotal,
            kauflandSavings = kSavings,
            auchanTotal = aTotal,
            auchanSavings = aSavings,
            bestOverallStore = bestOverallStore,
            recommendedStrategy = recommendedStrategy,
            matches = matches
        )
    }
}
