package ro.smartshopping.list

import android.app.Application
import ro.smartshopping.list.di.AppContainer

class SmartShoppingApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
