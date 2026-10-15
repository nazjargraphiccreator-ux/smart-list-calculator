package ro.smartshopping.list.domain.usecase

import ro.smartshopping.list.domain.model.FinancialSummary
import ro.smartshopping.list.domain.model.ShoppingListItem
import java.math.BigDecimal
import java.math.RoundingMode

class CalculateFinancialSummaryUseCase(
    private val completionRateCalculator: CalculateCompletionRateUseCase = CalculateCompletionRateUseCase()
) {

    /**
     * Calculates financial summary with precise decimal arithmetic.
     * Handles:
     * - Estimated Total: Sum of (quantity * estimatedUnitPrice)
     * - Actual Spent / Confirmed Paid: Sum of (quantity * actualUnitPrice) for purchased items
     * - In-Cart Total: Sum of (quantity * unitPrice) for items marked isInCart (not yet paid)
     * - Remaining to Buy: Sum of (quantity * unitPrice) for items neither in-cart nor paid
     * - Budget Tracking & Over-budget indicators
     * - Discounts & Extra fees (e.g. SGR 0.50 lei/btl, bags)
     * - Receipt Total Reconciliation
     */
    operator fun invoke(
        items: List<ShoppingListItem>,
        currency: String = "RON",
        budget: Double? = null,
        discountAmount: Double? = null,
        extraFeesAmount: Double? = null,
        receiptTotal: Double? = null
    ): FinancialSummary {
        var estimatedTotalBd = BigDecimal.ZERO
        var actualSpentBd = BigDecimal.ZERO
        var inCartTotalBd = BigDecimal.ZERO
        var remainingToBuyBd = BigDecimal.ZERO
        var remainingEstimatedBd = BigDecimal.ZERO

        var purchasedCount = 0
        var inCartCount = 0
        var toBuyCount = 0

        for (item in items) {
            val qtyBd = BigDecimal.valueOf(item.quantity)
            val estimatedPrice = item.estimatedUnitPrice?.let { BigDecimal.valueOf(it) } ?: BigDecimal.ZERO
            val itemEstimatedTotal = qtyBd.multiply(estimatedPrice)
            estimatedTotalBd = estimatedTotalBd.add(itemEstimatedTotal)

            if (item.isPurchased) {
                purchasedCount++
                val actualPrice = item.actualUnitPrice?.let { BigDecimal.valueOf(it) } ?: estimatedPrice
                actualSpentBd = actualSpentBd.add(qtyBd.multiply(actualPrice))
            } else if (item.isInCart) {
                inCartCount++
                val cartPrice = item.actualUnitPrice?.let { BigDecimal.valueOf(it) } ?: estimatedPrice
                inCartTotalBd = inCartTotalBd.add(qtyBd.multiply(cartPrice))
                remainingEstimatedBd = remainingEstimatedBd.add(itemEstimatedTotal)
            } else {
                toBuyCount++
                remainingToBuyBd = remainingToBuyBd.add(itemEstimatedTotal)
                remainingEstimatedBd = remainingEstimatedBd.add(itemEstimatedTotal)
            }
        }

        val estimatedTotal = estimatedTotalBd.setScale(2, RoundingMode.HALF_UP).toDouble()
        val actualSpent = actualSpentBd.setScale(2, RoundingMode.HALF_UP).toDouble()
        val inCartTotal = inCartTotalBd.setScale(2, RoundingMode.HALF_UP).toDouble()
        val remainingToBuyEstimated = remainingToBuyBd.setScale(2, RoundingMode.HALF_UP).toDouble()
        val remainingEstimated = remainingEstimatedBd.setScale(2, RoundingMode.HALF_UP).toDouble()
        val variance = BigDecimal.valueOf(actualSpent).subtract(BigDecimal.valueOf(estimatedTotal))
            .setScale(2, RoundingMode.HALF_UP).toDouble()

        val discount = (discountAmount ?: 0.0).coerceAtLeast(0.0)
        val extraFees = (extraFeesAmount ?: 0.0).coerceAtLeast(0.0)
        val netEstimatedTotal = BigDecimal.valueOf(estimatedTotal)
            .subtract(BigDecimal.valueOf(discount))
            .add(BigDecimal.valueOf(extraFees))
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

        // Budget calculations
        val remainingBudget = budget?.let { b ->
            val currentCommitment = if (actualSpent > 0.0) {
                actualSpent + inCartTotal
            } else {
                if (inCartTotal > 0.0) inCartTotal else estimatedTotal
            }
            BigDecimal.valueOf(b).subtract(BigDecimal.valueOf(currentCommitment))
                .setScale(2, RoundingMode.HALF_UP).toDouble()
        }

        val isOverBudget = remainingBudget != null && remainingBudget < 0.0
        val overBudgetAmount = if (isOverBudget && remainingBudget != null) kotlin.math.abs(remainingBudget) else 0.0

        val receiptVariance = receiptTotal?.let { rTotal ->
            val reference = if (actualSpent > 0.0) actualSpent else if (inCartTotal > 0.0) inCartTotal else estimatedTotal
            BigDecimal.valueOf(rTotal).subtract(BigDecimal.valueOf(reference))
                .setScale(2, RoundingMode.HALF_UP).toDouble()
        }

        val completionRate = completionRateCalculator(items)

        return FinancialSummary(
            estimatedTotal = estimatedTotal,
            actualSpent = actualSpent,
            remainingEstimated = remainingEstimated,
            variance = variance,
            purchasedItemsCount = purchasedCount,
            remainingItemsCount = inCartCount + toBuyCount,
            totalItemsCount = items.size,
            completionRate = completionRate,
            currency = currency,
            toBuyItemsCount = toBuyCount,
            inCartItemsCount = inCartCount,
            paidItemsCount = purchasedCount,
            inCartTotal = inCartTotal,
            remainingToBuyEstimated = remainingToBuyEstimated,
            budget = budget,
            remainingBudget = remainingBudget,
            isOverBudget = isOverBudget,
            overBudgetAmount = overBudgetAmount,
            discountAmount = discount,
            extraFeesAmount = extraFees,
            netEstimatedTotal = netEstimatedTotal,
            receiptTotal = receiptTotal,
            receiptVariance = receiptVariance
        )
    }
}
