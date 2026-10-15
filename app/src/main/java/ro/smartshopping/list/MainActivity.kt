package ro.smartshopping.list

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import ro.smartshopping.list.ui.navigation.AppNavHost
import ro.smartshopping.list.ui.theme.SmartShoppingTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as SmartShoppingApp).container

        setContent {
            SmartShoppingTheme {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    container = appContainer
                )
            }
        }
    }
}
