package ro.smartshopping.list.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.navArgument
import ro.smartshopping.list.di.AppContainer
import ro.smartshopping.list.ui.components.ActiveListFloatingBar
import ro.smartshopping.list.ui.components.BottomNavBar
import ro.smartshopping.list.ui.screens.analytics.PriceTrendsScreen
import ro.smartshopping.list.ui.screens.analytics.PriceTrendsViewModel
import ro.smartshopping.list.ui.screens.history.HistoryScreen
import ro.smartshopping.list.ui.screens.history.HistoryViewModel
import ro.smartshopping.list.ui.screens.lists.ListDetailScreen
import ro.smartshopping.list.ui.screens.lists.ListDetailViewModel
import ro.smartshopping.list.ui.screens.lists.ListsScreen
import ro.smartshopping.list.ui.screens.lists.ListsViewModel
import ro.smartshopping.list.ui.screens.products.ProductsScreen
import ro.smartshopping.list.ui.screens.products.ProductsViewModel
import ro.smartshopping.list.ui.screens.profile.ProfileScreen
import ro.smartshopping.list.ui.screens.profile.ProfileViewModel
import ro.smartshopping.list.ui.screens.shopping.ShoppingModeScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    container: AppContainer,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    val activeListId by container.activeSessionManager.activeListId.collectAsState()
    val activeListName by container.activeSessionManager.activeListName.collectAsState()
    val activeStoreName by container.activeSessionManager.activeStoreName.collectAsState()
    val checkedCount by container.activeSessionManager.checkedCount.collectAsState()
    val totalCount by container.activeSessionManager.totalCount.collectAsState()
    val totalAmount by container.activeSessionManager.totalAmount.collectAsState()

    val isAlreadyInList = currentRoute?.startsWith("list_detail") == true ||
            currentRoute?.startsWith("shopping_mode") == true ||
            currentRoute?.startsWith("pre_payment") == true

    val shouldShowBottomBar = currentRoute in listOf(
        Screen.Lists.route,
        Screen.SmartAgent.route,
        Screen.LoyaltyWallet.route,
        Screen.PriceTrends.route,
        Screen.History.route,
        Screen.Products.route,
        Screen.Profile.route
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            Column {
                ActiveListFloatingBar(
                    visible = activeListId != null && !isAlreadyInList,
                    listName = activeListName ?: "Lista Curentă",
                    storeName = activeStoreName,
                    checkedCount = checkedCount,
                    totalCount = totalCount,
                    totalAmount = totalAmount,
                    onReturnToList = {
                        activeListId?.let { id ->
                            navController.navigate(Screen.ListDetail.createRoute(id)) {
                                popUpTo(Screen.Lists.route) { saveState = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onDismiss = {
                        container.activeSessionManager.clear()
                    }
                )

                if (shouldShowBottomBar) {
                    BottomNavBar(
                        navController = navController,
                        activeListId = activeListId
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Lists.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Lists.route) {
                val viewModel = remember {
                    ListsViewModel(
                        repository = container.shoppingListRepository,
                        formatShareUseCase = container.formatShareUseCase,
                        loyaltyRepository = container.loyaltyCardRepository,
                        dealsRepository = container.storeDealsRepository
                    )
                }
                ListsScreen(
                    viewModel = viewModel,
                    parser = container.textImportParser,
                    productRepository = container.productRepository,
                    onNavigateToListDetail = { listId ->
                        navController.navigate(Screen.ListDetail.createRoute(listId))
                    },
                    onNavigateToShoppingMode = { listId ->
                        navController.navigate(Screen.ShoppingMode.createRoute(listId))
                    },
                    onNavigateToWallet = {
                        navController.navigate(Screen.LoyaltyWallet.route)
                    },
                    onNavigateToAgent = {
                        navController.navigate(Screen.SmartAgent.route)
                    }
                )
            }

            composable(Screen.SmartAgent.route) {
                val viewModel = remember {
                    ro.smartshopping.list.ui.screens.agent.SmartAgentViewModel(
                        listRepository = container.shoppingListRepository,
                        dealsRepository = container.storeDealsRepository,
                        agentUseCase = container.smartDiscountAgent
                    )
                }
                ro.smartshopping.list.ui.screens.agent.SmartAgentScreen(
                    viewModel = viewModel,
                    onNavigateToListDetail = { listId ->
                        navController.navigate(Screen.ListDetail.createRoute(listId))
                    }
                )
            }

            composable(Screen.LoyaltyWallet.route) {
                val viewModel = remember {
                    ro.smartshopping.list.ui.screens.wallet.LoyaltyWalletViewModel(
                        repository = container.loyaltyCardRepository
                    )
                }
                ro.smartshopping.list.ui.screens.wallet.LoyaltyWalletScreen(
                    viewModel = viewModel,
                    onNavigateBackToList = {
                        val activeId = activeListId
                        if (activeId != null) {
                            navController.navigate(Screen.ListDetail.createRoute(activeId)) {
                                popUpTo(Screen.Lists.route) { saveState = true }
                                launchSingleTop = true
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(Screen.Products.route) {
                val viewModel = remember {
                    ProductsViewModel(repository = container.productRepository)
                }
                ProductsScreen(viewModel = viewModel)
            }

            composable(Screen.History.route) {
                val viewModel = remember {
                    HistoryViewModel(
                        repository = container.shoppingListRepository,
                        carryForwardUseCase = container.carryForwardUseCase
                    )
                }
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToListDetail = { listId ->
                        navController.navigate(Screen.ListDetail.createRoute(listId))
                    }
                )
            }

            composable(Screen.PriceTrends.route) {
                val viewModel = remember {
                    PriceTrendsViewModel(
                        productRepository = container.productRepository,
                        priceRepository = container.priceTrackingRepository
                    )
                }
                PriceTrendsScreen(viewModel = viewModel)
            }

            composable(Screen.Profile.route) {
                val viewModel = remember {
                    ProfileViewModel(authRepository = container.authRepository)
                }
                ProfileScreen(viewModel = viewModel)
            }

            composable(
                route = Screen.ListDetail.route,
                arguments = listOf(navArgument("listId") { type = NavType.StringType })
            ) { backStackEntry ->
                val listId = backStackEntry.arguments?.getString("listId") ?: ""
                val viewModel = remember(listId) {
                    ListDetailViewModel(
                        listId = listId,
                        listRepository = container.shoppingListRepository,
                        productRepository = container.productRepository,
                        priceRepository = container.priceTrackingRepository,
                        financialCalculator = container.financialCalculator,
                        recordPriceUseCase = container.recordPriceUseCase,
                        formatShareUseCase = container.formatShareUseCase,
                        storeDealsRepository = container.storeDealsRepository
                    )
                }
                ListDetailScreen(
                    viewModel = viewModel,
                    parser = container.textImportParser,
                    activeSessionManager = container.activeSessionManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToShoppingMode = {
                        navController.navigate(Screen.ShoppingMode.createRoute(listId))
                    },
                    onNavigateToPrePayment = {
                        navController.navigate(Screen.PrePaymentCalculator.createRoute(listId))
                    },
                    onNavigateToWallet = {
                        navController.navigate(Screen.LoyaltyWallet.route)
                    },
                    onNavigateToAgent = {
                        navController.navigate(Screen.SmartAgent.route)
                    }
                )
            }

            composable(
                route = Screen.ShoppingMode.route,
                arguments = listOf(navArgument("listId") { type = NavType.StringType })
            ) { backStackEntry ->
                val listId = backStackEntry.arguments?.getString("listId") ?: ""
                val viewModel = remember(listId) {
                    ListDetailViewModel(
                        listId = listId,
                        listRepository = container.shoppingListRepository,
                        productRepository = container.productRepository,
                        priceRepository = container.priceTrackingRepository,
                        financialCalculator = container.financialCalculator,
                        recordPriceUseCase = container.recordPriceUseCase,
                        formatShareUseCase = container.formatShareUseCase,
                        storeDealsRepository = container.storeDealsRepository
                    )
                }
                ShoppingModeScreen(
                    viewModel = viewModel,
                    activeSessionManager = container.activeSessionManager,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToPrePayment = {
                        navController.navigate(Screen.PrePaymentCalculator.createRoute(listId))
                    },
                    onNavigateToWallet = {
                        navController.navigate(Screen.LoyaltyWallet.route)
                    }
                )
            }

            composable(
                route = Screen.PrePaymentCalculator.route,
                arguments = listOf(navArgument("listId") { type = NavType.StringType })
            ) { backStackEntry ->
                val listId = backStackEntry.arguments?.getString("listId") ?: ""
                val viewModel = remember(listId) {
                    ListDetailViewModel(
                        listId = listId,
                        listRepository = container.shoppingListRepository,
                        productRepository = container.productRepository,
                        priceRepository = container.priceTrackingRepository,
                        financialCalculator = container.financialCalculator,
                        recordPriceUseCase = container.recordPriceUseCase,
                        formatShareUseCase = container.formatShareUseCase,
                        storeDealsRepository = container.storeDealsRepository
                    )
                }
                ro.smartshopping.list.ui.screens.shopping.PrePaymentCalculatorScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
