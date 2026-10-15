package ro.smartshopping.list.ui.screens.shopping

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ro.smartshopping.list.domain.model.ItemPurchaseStage
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.StoreDeal
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.ui.components.CompletionProgressBar
import ro.smartshopping.list.ui.components.LiveBudgetCountdownCard
import ro.smartshopping.list.ui.components.ProductBarcodeScannerDialog
import ro.smartshopping.list.ui.components.StoreDealsBanner
import ro.smartshopping.list.ui.screens.lists.ListDetailViewModel
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import ro.smartshopping.list.util.StoreLocationHelper
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingModeScreen(
    viewModel: ListDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToPrePayment: () -> Unit,
    onNavigateToWallet: () -> Unit = {},
    activeSessionManager: ro.smartshopping.list.data.session.ActiveShoppingSessionManager? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showBarcodeScanner by remember { mutableStateOf(false) }
    var matchingDeals by remember { mutableStateOf<List<StoreDeal>>(emptyList()) }

    LaunchedEffect(state.items) {
        val names = state.items.map { it.productNameSnapshot }
        matchingDeals = viewModel.storeDealsRepository?.getDealsMatchingList(names) ?: emptyList()
    }

    // Keep activeSessionManager in sync
    LaunchedEffect(state.list, state.items, state.summary) {
        val currentList = state.list
        if (currentList != null) {
            val checked = state.items.count { it.isPurchased || it.isInCart }
            activeSessionManager?.updateActiveList(
                listId = currentList.id,
                name = currentList.name,
                store = currentList.storeName,
                checked = checked,
                total = state.items.size,
                amount = if (state.summary.actualSpent > 0.0) state.summary.actualSpent else state.summary.estimatedTotal
            )
        }
    }

    // Sorting: TO_BUY first, then IN_CART, then PURCHASED at bottom
    val sortedItems = remember(state.items) {
        state.items.sortedWith(
            compareBy<ShoppingListItem> {
                when (it.purchaseStage) {
                    ItemPurchaseStage.TO_BUY -> 0
                    ItemPurchaseStage.IN_CART -> 1
                    ItemPurchaseStage.PURCHASED -> 2
                }
            }.thenBy { it.createdAt }
        )
    }

    val currency = state.list?.currency ?: "RON"
    val summary = state.summary

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Mod Magazin (1 Mână)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.list?.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ieși")
                    }
                },
                actions = {
                    // Barcode Scanner button at shelf
                    IconButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanează Cod Raft",
                            tint = Color(0xFF00FF88)
                        )
                    }

                    IconButton(
                        onClick = onNavigateToWallet,
                        modifier = Modifier.padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = "Card Magazin",
                            tint = Color(0xFF00FF88)
                        )
                    }

                    Button(
                        onClick = onNavigateToPrePayment,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Calcul Înainte", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        },
        bottomBar = {
            // Running live spending and in-cart summary at bottom
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total În Coș",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", summary.inCartTotal)} $currency",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { showBarcodeScanner = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF00FF88).copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan la Raft",
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Achitat / Plătit",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", summary.actualSpent)} $currency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreenLight
                        )
                    }

                    Button(
                        onClick = onNavigateToPrePayment,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Calcul Casă", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Store location header banner if available
            state.list?.let { list ->
                if (!list.storeName.isNullOrBlank()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Magazin: ${list.storeName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = {
                                StoreLocationHelper.openStoreInGoogleMaps(context, list.storeName, list.storeAddress)
                            }
                        ) {
                            Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Google Maps", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Live Budget Countdown Card (Real-Time Subtraction + Exceed Alert)
            LiveBudgetCountdownCard(
                summary = summary,
                listName = state.list?.name ?: "Listă Cumpărături",
                onUpdateBudget = { newBudget ->
                    viewModel.updateListBudget(newBudget)
                },
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Official Store Deals Banner (Metro, Kaufland, Auchan, Lidl)
            if (matchingDeals.isNotEmpty()) {
                StoreDealsBanner(
                    matchingDeals = matchingDeals,
                    onApplyDealPrice = { name, discountPrice ->
                        viewModel.applyDiscountPriceToItem(name, discountPrice)
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // 3-Stage shopping stats chips: Planificate, În coș, Plătite
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StageCounterChip(
                    label = "Planificate",
                    count = summary.totalItemsCount,
                    color = MaterialTheme.colorScheme.outline
                )
                StageCounterChip(
                    label = "În Coș",
                    count = summary.inCartItemsCount,
                    color = MaterialTheme.colorScheme.primary
                )
                StageCounterChip(
                    label = "Plătite",
                    count = summary.paidItemsCount,
                    color = SuccessGreenLight
                )
            }

            // Live Completion bar
            CompletionProgressBar(
                purchasedCount = summary.paidItemsCount + summary.inCartItemsCount,
                totalCount = summary.totalItemsCount,
                completionPercentage = summary.completionRate,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            // One-Handed Supermarket List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortedItems, key = { it.id }) { item ->
                    ThreeStageShoppingRow(
                        item = item,
                        currency = currency,
                        onNextStage = {
                            val next = when (item.purchaseStage) {
                                ItemPurchaseStage.TO_BUY -> ItemPurchaseStage.IN_CART
                                ItemPurchaseStage.IN_CART -> ItemPurchaseStage.PURCHASED
                                ItemPurchaseStage.PURCHASED -> ItemPurchaseStage.TO_BUY
                            }
                            viewModel.updateItemPurchaseStage(item.id, next)

                            scope.launch {
                                val message = when (next) {
                                    ItemPurchaseStage.IN_CART -> "Pus în coș: ${item.productNameSnapshot}"
                                    ItemPurchaseStage.PURCHASED -> "Plătit: ${item.productNameSnapshot}"
                                    ItemPurchaseStage.TO_BUY -> "Resetat: ${item.productNameSnapshot}"
                                }
                                val result = snackbarHostState.showSnackbar(
                                    message = message,
                                    actionLabel = "Anulează (Undo)",
                                    duration = SnackbarDuration.Short
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    viewModel.updateItemPurchaseStage(item.id, item.purchaseStage)
                                }
                            }
                        },
                        onUpdateQuantity = { newQty ->
                            viewModel.updateItemQuantity(item.id, newQty)
                        }
                    )
                }
            }
        }
    }

    if (showBarcodeScanner) {
        ProductBarcodeScannerDialog(
            itemsInList = state.items,
            currency = currency,
            productRepository = viewModel.productRepository,
            onPutInCart = { itemId, price ->
                viewModel.updateItemPurchaseStage(itemId, ItemPurchaseStage.IN_CART, price)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Produs scanat și pus în coș!",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onAddNewProductToCart = { name, quantity, unit, price, barcode ->
                viewModel.addOrUpdateProductFromBarcode(name, quantity, unit, price, barcode, putInCart = true)
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Produs nou adăugat direct în coș!",
                        duration = SnackbarDuration.Short
                    )
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

@Composable
private fun StageCounterChip(label: String, count: Int, color: Color) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "$label: $count",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun ThreeStageShoppingRow(
    item: ShoppingListItem,
    currency: String,
    onNextStage: () -> Unit,
    onUpdateQuantity: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.purchaseStage) {
                ItemPurchaseStage.PURCHASED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ItemPurchaseStage.IN_CART -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ItemPurchaseStage.TO_BUY -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large circular button with one-tap stage progression
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when (item.purchaseStage) {
                                ItemPurchaseStage.PURCHASED -> SuccessGreenLight
                                ItemPurchaseStage.IN_CART -> MaterialTheme.colorScheme.primary
                                ItemPurchaseStage.TO_BUY -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            }
                        )
                        .clickable { onNextStage() },
                    contentAlignment = Alignment.Center
                ) {
                    when (item.purchaseStage) {
                        ItemPurchaseStage.PURCHASED -> Icon(Icons.Default.Check, contentDescription = "Plătit", tint = Color.White, modifier = Modifier.size(24.dp))
                        ItemPurchaseStage.IN_CART -> Icon(Icons.Default.ShoppingCart, contentDescription = "În coș", tint = Color.White, modifier = Modifier.size(22.dp))
                        ItemPurchaseStage.TO_BUY -> Text("+ Coș", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Product details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNextStage() }
                ) {
                    Text(
                        text = item.productNameSnapshot,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.isPurchased) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity} ${item.unit.getSymbol(true)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = " • ${item.purchaseStage.labelRo}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = when (item.purchaseStage) {
                                ItemPurchaseStage.PURCHASED -> SuccessGreenLight
                                ItemPurchaseStage.IN_CART -> MaterialTheme.colorScheme.primary
                                ItemPurchaseStage.TO_BUY -> MaterialTheme.colorScheme.outline
                            }
                        )
                    }
                }

                // Line Total
                val price = item.effectiveUnitPrice
                if (price != null && price > 0.0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", item.calculatedLineTotal)} $currency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (item.isPurchased) SuccessGreenLight else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Quick stepper row for one-handed quantity adjustments in store
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = {
                            val step = if (item.unit == UnitOfMeasure.KG || item.unit == UnitOfMeasure.L) 0.5 else 1.0
                            val next = (item.quantity - step).coerceAtLeast(0.1)
                            onUpdateQuantity(next)
                        },
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity} ${item.unit.getSymbol(true)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            val step = if (item.unit == UnitOfMeasure.KG || item.unit == UnitOfMeasure.L) 0.5 else 1.0
                            onUpdateQuantity(item.quantity + step)
                        },
                        modifier = Modifier.size(32.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }

                TextButton(onClick = onNextStage) {
                    Text(
                        text = when (item.purchaseStage) {
                            ItemPurchaseStage.TO_BUY -> "Pune în coș →"
                            ItemPurchaseStage.IN_CART -> "Marchează Plătit ✓"
                            ItemPurchaseStage.PURCHASED -> "Anulează plata"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
