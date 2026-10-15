package ro.smartshopping.list.ui.navigation

sealed class Screen(val route: String, val titleRo: String, val titleEn: String) {
    data object Lists : Screen("lists", "Liste", "Lists")
    data object SmartAgent : Screen("smart_agent", "Agent Reduceri", "Deals Agent")
    data object Products : Screen("products", "Produse", "Products")
    data object History : Screen("history", "Istoric", "History")
    data object PriceTrends : Screen("price_trends", "Prețuri", "Price Trends")
    data object Profile : Screen("profile", "Profil", "Profile")

    data object ListDetail : Screen("list_detail/{listId}", "Detalii Listă", "List Details") {
        fun createRoute(listId: String) = "list_detail/$listId"
    }

    data object ShoppingMode : Screen("shopping_mode/{listId}", "Mod Magazin", "Shopping Mode") {
        fun createRoute(listId: String) = "shopping_mode/$listId"
    }

    data object PrePaymentCalculator : Screen("pre_payment/{listId}", "Calcul Înainte de Plată", "Pre-Payment Calculator") {
        fun createRoute(listId: String) = "pre_payment/$listId"
    }
}
