package ro.smartshopping.list

import android.app.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import ro.smartshopping.list.di.AppContainer
import ro.smartshopping.list.util.StoreNotificationHelper

class SmartShoppingApp : Application() {

    lateinit var container: AppContainer
        private set

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Initialize Android notification channels for store deals & budget alerts
        StoreNotificationHelper.createNotificationChannel(this)

        // Launch background discounts worker
        container.storeDealsWorker.startPeriodicBackgroundWorker(applicationScope)
    }
}

