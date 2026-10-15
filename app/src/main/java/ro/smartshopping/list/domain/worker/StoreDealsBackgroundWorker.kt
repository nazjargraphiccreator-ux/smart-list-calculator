package ro.smartshopping.list.domain.worker

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ro.smartshopping.list.data.repository.ShoppingListRepository
import ro.smartshopping.list.data.repository.StoreDealsRepository
import ro.smartshopping.list.domain.model.StoreDealNotification
import ro.smartshopping.list.util.StoreNotificationHelper
import java.util.UUID

class StoreDealsBackgroundWorker(
    private val context: Context,
    private val listRepository: ShoppingListRepository,
    private val storeDealsRepository: StoreDealsRepository
) {
    private var workerJob: Job? = null

    /**
     * Starts continuous periodic background discount checks.
     * Checks official store deals every [intervalMillis] (defaults to 15 minutes).
     */
    fun startPeriodicBackgroundWorker(
        scope: CoroutineScope,
        intervalMillis: Long = 1000L * 60 * 15
    ) {
        if (workerJob?.isActive == true) return

        workerJob = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    checkAndNotifyStoreDeals()
                } catch (e: Exception) {
                    // Gracefully handle network/IO errors in worker
                }
                delay(intervalMillis)
            }
        }
    }

    fun stopWorker() {
        workerJob?.cancel()
        workerJob = null
    }

    /**
     * Executes an immediate discount audit across all active shopping lists.
     * Compares products against official retailer promotions (Metro, Kaufland, Auchan, Lidl).
     */
    suspend fun checkAndNotifyStoreDeals(): Int {
        val activeLists = listRepository.getAllListsFlow().first()
        var totalDealsFound = 0

        for (list in activeLists) {
            val items = listRepository.getItemsForListFlow(list.id).first()
            if (items.isEmpty()) continue

            val productNames = items.map { it.productNameSnapshot }
            val matchingDeals = storeDealsRepository.getDealsMatchingList(productNames)

            if (matchingDeals.isNotEmpty()) {
                totalDealsFound += matchingDeals.size
                val bestDeal = matchingDeals.maxByOrNull { it.discountPercent } ?: matchingDeals.first()

                // Trigger in-app notification state
                storeDealsRepository.addNotification(
                    StoreDealNotification(
                        id = UUID.randomUUID().toString(),
                        storeName = bestDeal.storeName,
                        title = "Reduceri noi pentru \"${list.name}\"",
                        message = "Au fost identificate ${matchingDeals.size} produse la reducere în cataloagele oficiale! Ex: ${bestDeal.productName} (-${bestDeal.discountPercent}%).",
                        discountHighlight = "-${bestDeal.discountPercent}% la ${bestDeal.productName}",
                        timestamp = System.currentTimeMillis()
                    )
                )

                // Trigger Android system notification
                StoreNotificationHelper.postStoreDealNotification(
                    context = context,
                    notificationId = list.id.hashCode(),
                    storeName = bestDeal.storeName,
                    title = "Reduceri active pentru \"${list.name}\"",
                    message = "${matchingDeals.size} produse din lista ta sunt în promoție oficială la ${bestDeal.storeName}!",
                    discountHighlight = "-${bestDeal.discountPercent}% la ${bestDeal.productName}"
                )
            }
        }

        return totalDealsFound
    }
}
