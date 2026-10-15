package ro.smartshopping.list.ui.screens.lists

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.smartshopping.list.data.local.LocalBackupStorageManager
import ro.smartshopping.list.data.repository.LoyaltyCardRepository
import ro.smartshopping.list.data.repository.ProductRepository
import ro.smartshopping.list.data.repository.ShoppingListRepository
import ro.smartshopping.list.data.repository.StoreDealsRepository
import ro.smartshopping.list.domain.model.ItemPurchaseStage
import ro.smartshopping.list.domain.model.LoyaltyCard
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.ShoppingListStatus
import ro.smartshopping.list.domain.model.StoreDeal
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.parser.ParsedShoppingItem
import ro.smartshopping.list.domain.parser.SmartTextImportParser
import ro.smartshopping.list.domain.usecase.FormatListForSharingUseCase
import ro.smartshopping.list.ui.components.CompletionProgressBar
import ro.smartshopping.list.ui.components.ProductBarcodeScannerDialog
import ro.smartshopping.list.ui.components.TextImportDialog
import ro.smartshopping.list.ui.screens.wallet.FullScreenBarcodeDialog
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import ro.smartshopping.list.util.ShareIntentHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ListsViewModel(
    private val repository: ShoppingListRepository,
    private val formatShareUseCase: FormatListForSharingUseCase,
    val loyaltyRepository: LoyaltyCardRepository? = null,
    val dealsRepository: StoreDealsRepository? = null
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")
    private val storeFilter = MutableStateFlow<String?>("TOATE")
    private val globalBudgetOverride = MutableStateFlow<Double?>(null)

    private val loyaltyCardsFlow = loyaltyRepository?.getAllCardsFlow() ?: flowOf(emptyList())
    private val storeDealsFlow = dealsRepository?.getAllDealsFlow() ?: flowOf(emptyList())

    val uiState: StateFlow<ListsUiState> = combine(
        repository.getListsByStatusFlow(ShoppingListStatus.ACTIVE),
        searchQuery,
        storeFilter,
        loyaltyCardsFlow,
        storeDealsFlow
    ) { lists, query, filter, cards, deals ->
        val filteredByQuery = if (query.isBlank()) {
            lists
        } else {
            lists.filter {
                it.name.contains(query, ignoreCase = true) ||
                (it.notes?.contains(query, ignoreCase = true) == true) ||
                (it.storeName?.contains(query, ignoreCase = true) == true)
            }
        }

        val filteredByStore = if (filter == null || filter == "TOATE") {
            filteredByQuery
        } else {
            filteredByQuery.filter { it.storeName?.contains(filter, ignoreCase = true) == true }
        }

        // Aggregate Executive Statistics
        val totalEstimated = lists.sumOf { it.estimatedTotal }
        val totalSpent = lists.sumOf { it.actualTotal }

        // Sum of list budgets or fallback sensible default matching mockup
        val configuredBudget = lists.mapNotNull { it.budget }.sum()
        val totalBudget = globalBudgetOverride.value ?: if (configuredBudget > 0.0) configuredBudget else if (totalEstimated > 0.0) totalEstimated * 1.25 else 245.00
        val remainingBudget = totalBudget - totalSpent
        val isOverBudget = remainingBudget < 0.0

        val totalItems = lists.sumOf { it.totalItemsCount }
        val purchasedItems = lists.sumOf { it.purchasedItemsCount }
        val avgCompletion = if (totalItems > 0) ((purchasedItems.toDouble() / totalItems) * 100).toInt() else 0

        // Potential savings from deals matching products
        val dealsSavings = deals.sumOf { (it.originalPrice - it.discountPrice).coerceAtLeast(0.0) }

        ListsUiState(
            lists = filteredByStore,
            allActiveLists = lists,
            query = query,
            selectedStoreFilter = filter ?: "TOATE",
            loyaltyCards = cards,
            activeDeals = deals,
            activeDealsCount = deals.size,
            potentialSavings = dealsSavings,
            totalBudget = totalBudget,
            totalEstimated = totalEstimated,
            totalSpent = totalSpent,
            totalRemainingBudget = remainingBudget,
            isOverBudget = isOverBudget,
            totalItemsCount = totalItems,
            totalPurchasedCount = purchasedItems,
            averageCompletionRate = avgCompletion,
            isLoading = false
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListsUiState())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun updateStoreFilter(store: String) {
        storeFilter.value = store
    }

    fun setGlobalBudget(budget: Double?) {
        globalBudgetOverride.value = budget
    }

    fun createNewList(name: String, notes: String? = null) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.createList(name = name, notes = notes)
        }
    }

    fun createListFromParsedItems(
        name: String,
        items: List<ParsedShoppingItem>,
        productRepository: ProductRepository,
        onCreated: (String) -> Unit
    ) {
        viewModelScope.launch {
            val listId = repository.createList(name = name)
            val newItems = items.map { parsed ->
                val product = productRepository.findOrCreateProduct(
                    name = parsed.name,
                    category = parsed.category,
                    defaultUnit = parsed.unit,
                    estimatedPrice = parsed.unitPrice
                )
                ShoppingListItem(
                    id = UUID.randomUUID().toString(),
                    shoppingListId = listId,
                    productId = product.id,
                    productNameSnapshot = product.name,
                    quantity = parsed.quantity,
                    unit = parsed.unit,
                    packageCount = parsed.packageCount,
                    estimatedUnitPrice = parsed.unitPrice ?: product.lastKnownPrice,
                    actualUnitPrice = null,
                    isPurchased = false,
                    isInCart = false,
                    category = product.category,
                    notes = parsed.notes,
                    storeName = parsed.storeName
                )
            }
            repository.addItems(newItems)
            onCreated(listId)
        }
    }

    fun createMultiStoreLists(
        itemsByStore: Map<String, List<ParsedShoppingItem>>,
        baseTitle: String,
        productRepository: ProductRepository,
        onCreated: (firstListId: String) -> Unit
    ) {
        viewModelScope.launch {
            var firstCreatedListId: String? = null

            for ((store, storeItems) in itemsByStore) {
                val listName = if (baseTitle.isNotBlank() && baseTitle != "Listă Cumpărături") {
                    "$baseTitle - $store"
                } else {
                    "Cumpărături $store"
                }

                val listId = repository.createList(name = listName)
                repository.updateStoreAndBudget(
                    id = listId,
                    storeName = store,
                    storeAddress = null,
                    lat = null,
                    lng = null,
                    budget = null,
                    receiptTotal = null
                )

                if (firstCreatedListId == null) {
                    firstCreatedListId = listId
                }

                val newItems = storeItems.map { parsed ->
                    val product = productRepository.findOrCreateProduct(
                        name = parsed.name,
                        category = parsed.category,
                        defaultUnit = parsed.unit,
                        estimatedPrice = parsed.unitPrice
                    )
                    ShoppingListItem(
                        id = UUID.randomUUID().toString(),
                        shoppingListId = listId,
                        productId = product.id,
                        productNameSnapshot = product.name,
                        quantity = parsed.quantity,
                        unit = parsed.unit,
                        packageCount = parsed.packageCount,
                        estimatedUnitPrice = parsed.unitPrice ?: product.lastKnownPrice,
                        actualUnitPrice = null,
                        isPurchased = false,
                        isInCart = false,
                        category = product.category,
                        notes = parsed.notes,
                        storeName = store
                    )
                }
                repository.addItems(newItems)
            }

            firstCreatedListId?.let { onCreated(it) }
        }
    }

    fun addProductFromBarcode(
        name: String,
        quantity: Double,
        unit: UnitOfMeasure,
        price: Double?,
        barcode: String,
        productRepository: ProductRepository
    ) {
        viewModelScope.launch {
            val lists = uiState.value.allActiveLists
            val targetListId = if (lists.isNotEmpty()) {
                lists.first().id
            } else {
                repository.createList("Cumpărături Rapide Raft")
            }

            val product = productRepository.findOrCreateProduct(
                name = name,
                defaultUnit = unit,
                estimatedPrice = price
            ).copy(barcode = barcode)
            productRepository.saveProduct(product)

            val newItem = ShoppingListItem(
                id = UUID.randomUUID().toString(),
                shoppingListId = targetListId,
                productId = product.id,
                productNameSnapshot = product.name,
                quantity = quantity,
                unit = unit,
                estimatedUnitPrice = price ?: product.lastKnownPrice,
                actualUnitPrice = price,
                isPurchased = false,
                isInCart = true,
                category = product.category,
                notes = "Scanat Raft: $barcode"
            )
            repository.addItem(newItem)
        }
    }

    fun duplicateList(listId: String, currentName: String) {
        viewModelScope.launch {
            repository.duplicateList(listId, "$currentName (Copie)", preservePrices = true)
        }
    }

    fun completeList(listId: String) {
        viewModelScope.launch {
            repository.updateListStatus(listId, ShoppingListStatus.COMPLETED)
        }
    }

    fun deleteList(listId: String) {
        viewModelScope.launch {
            repository.deleteList(listId)
        }
    }

    fun shareList(context: Context, list: ShoppingList) {
        viewModelScope.launch {
            val items = repository.getItemsForList(list.id)
            val shareText = formatShareUseCase(list, items, isRomanian = true)
            ShareIntentHelper.shareText(context, shareText, "Trimite: ${list.name}")
        }
    }
}

data class ListsUiState(
    val lists: List<ShoppingList> = emptyList(),
    val allActiveLists: List<ShoppingList> = emptyList(),
    val query: String = "",
    val selectedStoreFilter: String = "TOATE",
    val loyaltyCards: List<LoyaltyCard> = emptyList(),
    val activeDeals: List<StoreDeal> = emptyList(),
    val activeDealsCount: Int = 0,
    val potentialSavings: Double = 0.0,
    val totalBudget: Double = 245.0,
    val totalEstimated: Double = 0.0,
    val totalSpent: Double = 0.0,
    val totalRemainingBudget: Double = 104.40,
    val isOverBudget: Boolean = false,
    val totalItemsCount: Int = 0,
    val totalPurchasedCount: Int = 0,
    val averageCompletionRate: Int = 0,
    val isLoading: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListsScreen(
    viewModel: ListsViewModel,
    parser: SmartTextImportParser,
    productRepository: ProductRepository,
    onNavigateToListDetail: (String) -> Unit,
    onNavigateToShoppingMode: (String) -> Unit,
    onNavigateToWallet: () -> Unit = {},
    onNavigateToAgent: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showBudgetDialog by remember { mutableStateOf(false) }
    var showShelfScanner by remember { mutableStateOf(false) }
    var selectedCardForDisplay by remember { mutableStateOf<LoyaltyCard?>(null) }

    val currentDateFormatted = remember {
        SimpleDateFormat("d MMM yyyy", Locale("ro", "RO")).format(Date()).uppercase()
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF00FF88), Color(0xFF00B0FF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Smart-list-Calculator",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00FF88))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Offline & Securizat • Militari",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Quick Action: Shelf Barcode Scanner
                    IconButton(onClick = { showShelfScanner = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan la Raft",
                            tint = Color(0xFF00FF88)
                        )
                    }

                    // Quick Action: Loyalty Wallet Shortcut
                    IconButton(onClick = onNavigateToWallet) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = "Portofel Carduri",
                            tint = Color(0xFF00E5FF)
                        )
                    }

                    // Quick Action: WhatsApp Smart Import
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Import Text",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFF00FF88),
                contentColor = Color.Black,
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Listă Nouă")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Listă Nouă", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            // 1. HERO CARD BUGET ȘI NUMĂRĂTOARE INVERSĂ (Emerald Green Gradient conform mockup_app_overview.jpg)
            item {
                DashboardHeroBudgetCard(
                    totalBudget = state.totalBudget,
                    remainingBudget = state.totalRemainingBudget,
                    isOverBudget = state.isOverBudget,
                    completionRate = state.averageCompletionRate,
                    onEditBudget = { showBudgetDialog = true },
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // 2. QUICK ACTION BAR (4 Butoane Premium Rapide)
            item {
                DashboardQuickActionBar(
                    onScanShelf = { showShelfScanner = true },
                    onOpenWallet = onNavigateToWallet,
                    onImportText = { showImportDialog = true },
                    onOpenDeals = onNavigateToAgent
                )
            }

            // 3. BENTO STATS GRID (Statistici Profesionale 2026)
            item {
                DashboardBentoStatsGrid(
                    state = state,
                    onNavigateToWallet = onNavigateToWallet,
                    onNavigateToAgent = onNavigateToAgent
                )
            }

            // 4. PORTOFEL CARDURI RAPIDE LA CASĂ (conforming to mockup_loyalty_wallet.jpg)
            if (state.loyaltyCards.isNotEmpty()) {
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Carduri Fidelitate Magazin",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            TextButton(onClick = onNavigateToWallet) {
                                Text("Vezi Toate (${state.loyaltyCards.size})", fontSize = 12.sp, color = Color(0xFF00E5FF))
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            items(state.loyaltyCards, key = { it.id }) { card ->
                                QuickLoyaltyCardPill(
                                    card = card,
                                    onClick = { selectedCardForDisplay = card }
                                )
                            }
                        }
                    }
                }
            }

            // 5. SECȚIUNEA "LISTĂ CUMPĂRĂTURI" (cu dată calendar & filtre magazin conform mockup)
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LISTĂ CUMPĂRĂTURI",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = currentDateFormatted,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF00FF88).copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, Color(0xFF00FF88).copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = "${state.allActiveLists.size} Liste Active",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF88)
                            )
                        }
                    }

                    // Search bar
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        placeholder = { Text("Caută după produs, listă sau magazin...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true
                    )

                    // Store Filter Pills (TOATE, METRO, KAUFLAND, AUCHAN, LIDL, PENNY)
                    val storeTabs = listOf("TOATE", "METRO", "KAUFLAND", "AUCHAN", "LIDL", "PENNY")
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        items(storeTabs) { tab ->
                            val isSelected = state.selectedStoreFilter.equals(tab, ignoreCase = true)
                            StoreFilterPill(
                                title = tab,
                                isSelected = isSelected,
                                onClick = { viewModel.updateStoreFilter(tab) }
                            )
                        }
                    }
                }
            }

            // 6. LISTELE CURENTE
            if (state.lists.isEmpty() && !state.isLoading) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (state.query.isNotBlank()) "Nicio listă nu corespunde căutării" else "Nu ai nicio listă pentru magazinul selectat",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showCreateDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88), contentColor = Color.Black)
                                ) {
                                    Text("+ Listă Nouă", fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(onClick = { showImportDialog = true }) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Lipește WhatsApp")
                                }
                            }
                        }
                    }
                }
            } else {
                items(state.lists, key = { it.id }) { list ->
                    ShoppingListCard(
                        list = list,
                        onClick = { onNavigateToListDetail(list.id) },
                        onShoppingMode = { onNavigateToShoppingMode(list.id) },
                        onShare = { viewModel.shareList(context, list) },
                        onDuplicate = { viewModel.duplicateList(list.id, list.name) },
                        onComplete = { viewModel.completeList(list.id) },
                        onDelete = { viewModel.deleteList(list.id) }
                    )
                }
            }
        }
    }

    // Full Screen Barcode Dialog for Loyalty Card
    selectedCardForDisplay?.let { card ->
        FullScreenBarcodeDialog(
            card = card,
            onClose = { selectedCardForDisplay = null }
        )
    }

    // Shelf Product Barcode Scanner Dialog
    if (showShelfScanner) {
        ProductBarcodeScannerDialog(
            itemsInList = emptyList(),
            currency = "RON",
            productRepository = productRepository,
            onPutInCart = { _, _ -> },
            onAddNewProductToCart = { name, quantity, unit, price, barcode ->
                viewModel.addProductFromBarcode(name, quantity, unit, price, barcode, productRepository)
                Toast.makeText(context, "Produs scanat și adăugat în listă!", Toast.LENGTH_SHORT).show()
                showShelfScanner = false
            },
            onDismiss = { showShelfScanner = false }
        )
    }

    // Global Budget Dialog
    if (showBudgetDialog) {
        var tempBudget by remember { mutableStateOf(String.format(Locale.US, "%.2f", state.totalBudget)) }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Setează Bugetul Global") },
            text = {
                Column {
                    Text(
                        text = "Ajustează limita totală de cheltuieli pentru cumpărăturile curente:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = tempBudget,
                        onValueChange = { tempBudget = it },
                        label = { Text("Buget Total (RON)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val parsed = tempBudget.replace(',', '.').toDoubleOrNull()
                    viewModel.setGlobalBudget(parsed)
                    showBudgetDialog = false
                }) {
                    Text("Salvează")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBudgetDialog = false }) {
                    Text("Anulează")
                }
            }
        )
    }

    // Create List Dialog
    if (showCreateDialog) {
        CreateListDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, notes ->
                viewModel.createNewList(name, notes)
                showCreateDialog = false
            }
        )
    }

    // Smart Text Import Dialog (WhatsApp / SMS)
    if (showImportDialog) {
        TextImportDialog(
            parser = parser,
            onDismiss = { showImportDialog = false },
            onConfirmImport = { parsedItems ->
                val listName = "Cumpărături (${parsedItems.size} produse)"
                viewModel.createListFromParsedItems(
                    name = listName,
                    items = parsedItems,
                    productRepository = productRepository,
                    onCreated = { newListId ->
                        showImportDialog = false
                        onNavigateToListDetail(newListId)
                    }
                )
            },
            onConfirmMultiStoreImport = { itemsByStore, baseTitle ->
                viewModel.createMultiStoreLists(
                    itemsByStore = itemsByStore,
                    baseTitle = baseTitle,
                    productRepository = productRepository,
                    onCreated = { firstListId ->
                        showImportDialog = false
                        onNavigateToListDetail(firstListId)
                    }
                )
            }
        )
    }
}

/**
 * 1. HERO BUDGET CARD (Design conform mockup_app_overview.jpg)
 */
@Composable
fun DashboardHeroBudgetCard(
    totalBudget: Double,
    remainingBudget: Double,
    isOverBudget: Boolean,
    completionRate: Int,
    onEditBudget: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp))
            .clickable { onEditBudget() },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        colors = if (isOverBudget) {
                            listOf(Color(0xFF4A000A), Color(0xFF1E0308))
                        } else {
                            listOf(Color(0xFF003820), Color(0xFF082618), Color(0xFF0F1E17))
                        }
                    )
                )
                .border(
                    BorderStroke(
                        1.5.dp,
                        if (isOverBudget) Color(0xFFFF3366) else Color(0xFF00FF88).copy(alpha = 0.5f)
                    ),
                    RoundedCornerShape(22.dp)
                )
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "BUGET CUMPĂRĂTURI: ${String.format(Locale.US, "%.2f", totalBudget)} RON",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White.copy(alpha = 0.95f),
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isOverBudget) {
                                    "DEPĂȘIT CU: ${String.format(Locale.US, "%.2f", kotlin.math.abs(remainingBudget))} RON ⚠️"
                                } else {
                                    "RĂMAS: ${String.format(Locale.US, "%.2f", remainingBudget)} RON"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isOverBudget) Color(0xFFFF5252) else Color(0xFF00FF88)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        IconButton(onClick = onEditBudget) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editează Buget",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Glowing Neon Progress Bar
                val progressFraction = if (totalBudget > 0.0) {
                    ((totalBudget - remainingBudget.coerceAtLeast(0.0)) / totalBudget).toFloat().coerceIn(0f, 1f)
                } else 0f

                LinearProgressIndicator(
                    progress = { progressFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color = if (isOverBudget) Color(0xFFFF3366) else Color(0xFF00FF88),
                    trackColor = Color.Black.copy(alpha = 0.4f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cheltuit: ${(progressFraction * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Atinge cardul pt setare buget",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

/**
 * 2. QUICK ACTION BAR
 */
@Composable
fun DashboardQuickActionBar(
    onScanShelf: () -> Unit,
    onOpenWallet: () -> Unit,
    onImportText: () -> Unit,
    onOpenDeals: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickActionPill(
            icon = Icons.Default.QrCodeScanner,
            label = "Scan Raft",
            color = Color(0xFF00FF88),
            onClick = onScanShelf,
            modifier = Modifier.weight(1f)
        )
        QuickActionPill(
            icon = Icons.Default.CreditCard,
            label = "Carduri",
            color = Color(0xFF00E5FF),
            onClick = onOpenWallet,
            modifier = Modifier.weight(1f)
        )
        QuickActionPill(
            icon = Icons.Default.AutoAwesome,
            label = "Import",
            color = Color(0xFFB388FF),
            onClick = onImportText,
            modifier = Modifier.weight(1f)
        )
        QuickActionPill(
            icon = Icons.Default.LocalOffer,
            label = "Reduceri",
            color = Color(0xFFFFD600),
            onClick = onOpenDeals,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionPill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.25f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
    }
}

/**
 * 3. BENTO STATS GRID (Statistici & Monitorizare Magazine)
 */
@Composable
fun DashboardBentoStatsGrid(
    state: ListsUiState,
    onNavigateToWallet: () -> Unit,
    onNavigateToAgent: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 1: Economii Reduceri Active
            BentoStatCard(
                title = "Economii Reduceri",
                value = "${String.format(Locale.US, "%.2f", state.potentialSavings)} RON",
                subtitle = "${state.activeDealsCount} promoții Militari",
                accentColor = Color(0xFFFFD600),
                icon = Icons.Default.TrendingUp,
                onClick = onNavigateToAgent,
                modifier = Modifier.weight(1f)
            )

            // Card 2: Portofel Fidelitate
            BentoStatCard(
                title = "Carduri Salvate",
                value = "${state.loyaltyCards.size} Carduri",
                subtitle = "Gata de scanare POS",
                accentColor = Color(0xFF00E5FF),
                icon = Icons.Default.CreditCard,
                onClick = onNavigateToWallet,
                modifier = Modifier.weight(1f)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Card 3: Total Cheltuit
            BentoStatCard(
                title = "Total Cheltuit",
                value = "${String.format(Locale.US, "%.2f", state.totalSpent)} RON",
                subtitle = "Din ${String.format(Locale.US, "%.2f", state.totalEstimated)} est.",
                accentColor = Color(0xFF00FF88),
                icon = Icons.Default.ShoppingCart,
                onClick = {},
                modifier = Modifier.weight(1f)
            )

            // Card 4: Progres Cumpărături
            BentoStatCard(
                title = "Progres Articole",
                value = "${state.totalPurchasedCount}/${state.totalItemsCount}",
                subtitle = "${state.averageCompletionRate}% Finalizat",
                accentColor = Color(0xFFB388FF),
                icon = Icons.Default.CheckCircle,
                onClick = {},
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun BentoStatCard(
    title: String,
    value: String,
    subtitle: String,
    accentColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * 4. QUICK LOYALTY CARD PILL (Conform mockup_loyalty_wallet.jpg)
 */
@Composable
fun QuickLoyaltyCardPill(
    card: LoyaltyCard,
    onClick: () -> Unit
) {
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(card.primaryColorHex))
    } catch (_: Exception) {
        Color(0xFFE11A22)
    }
    val secondaryColor = try {
        Color(android.graphics.Color.parseColor(card.secondaryColorHex))
    } catch (_: Exception) {
        Color(0xFF7A0006)
    }

    Card(
        modifier = Modifier
            .width(180.dp)
            .height(105.dp)
            .shadow(6.dp, RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    )
                )
                .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(14.dp))
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.storeName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1
                    )
                    Icon(
                        imageVector = Icons.Default.QrCode,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sim chip auriu
                    Box(
                        modifier = Modifier
                            .size(24.dp, 18.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFFFFDF7A), Color(0xFFD4AF37))
                                )
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = card.programName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.maskedNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "SCAN ⚡",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00FF88)
                    )
                }
            }
        }
    }
}

/**
 * STORE FILTER PILL
 */
@Composable
fun StoreFilterPill(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        color = if (isSelected) Color(0xFF00FF88) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        contentColor = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF00FF88) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}

/**
 * 6. SHOPPING LIST CARD (Design conform mockup_app_overview.jpg)
 */
@Composable
fun ShoppingListCard(
    list: ShoppingList,
    onClick: () -> Unit,
    onShoppingMode: () -> Unit,
    onShare: () -> Unit,
    onDuplicate: () -> Unit,
    onComplete: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val storeNameUpper = (list.storeName ?: "GENERAL").uppercase()

    // Store badge color system matching Romanian hypermarket branding
    val (badgeBg, badgeText) = when {
        storeNameUpper.contains("AUCHAN") -> Pair(Color(0xFFD61821), Color.White)
        storeNameUpper.contains("KAUFLAND") -> Pair(Color(0xFFE11A22), Color.White)
        storeNameUpper.contains("METRO") -> Pair(Color(0xFF002D72), Color(0xFFFFD100))
        storeNameUpper.contains("LIDL") -> Pair(Color(0xFF0050AA), Color(0xFFFFCC00))
        storeNameUpper.contains("PENNY") -> Pair(Color(0xFFE30613), Color(0xFFFFCC00))
        else -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Checkbox icon, Title, Store Badge, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular checkmark box (as in mockup)
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (list.isCompleted) Color(0xFF00FF88) else Color.White.copy(alpha = 0.1f)
                            )
                            .border(
                                BorderStroke(
                                    1.5.dp,
                                    if (list.isCompleted) Color(0xFF00FF88) else Color.White.copy(alpha = 0.4f)
                                ),
                                RoundedCornerShape(6.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (list.isCompleted) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = list.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = "${list.totalItemsCount} produse • Est: ${String.format(Locale.US, "%.2f", list.estimatedTotal)} ${list.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Store badge pill (AUCHAN, KAUFLAND, METRO, LIDL)
                    if (!list.storeName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = badgeBg,
                            contentColor = badgeText,
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = list.storeName.uppercase(),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opțiuni")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Deschide Detalii") },
                                leadingIcon = { Icon(Icons.Default.Edit, null) },
                                onClick = {
                                    menuExpanded = false
                                    onClick()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pornire Mod Magazin") },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null) },
                                onClick = {
                                    menuExpanded = false
                                    onShoppingMode()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Finalizează Cumpărăturile") },
                                leadingIcon = { Icon(Icons.Default.CheckCircle, null) },
                                onClick = {
                                    menuExpanded = false
                                    onComplete()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplică Lista") },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                                onClick = {
                                    menuExpanded = false
                                    onDuplicate()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Salvează fișier TXT pe telefon") },
                                leadingIcon = { Icon(Icons.Default.Save, null) },
                                onClick = {
                                    menuExpanded = false
                                    val file = LocalBackupStorageManager.exportListToTxt(
                                        context = context,
                                        list = list,
                                        items = emptyList()
                                    )
                                    Toast.makeText(
                                        context,
                                        "Fișier TXT salvat în Documents/SmartListCalculator:\n${file.name}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Trimite pe WhatsApp") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    menuExpanded = false
                                    onShare()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Șterge", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    menuExpanded = false
                                    onDelete()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Completion progress
            CompletionProgressBar(
                purchasedCount = list.purchasedItemsCount,
                totalCount = list.totalItemsCount,
                completionPercentage = list.completionPercentage
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Totals and Shopping Mode Button Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    if (list.actualTotal > 0.0) {
                        Text(
                            text = "Cheltuit la casă: ${String.format(Locale.US, "%.2f", list.actualTotal)} ${list.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF00FF88),
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            text = "Gata de cumpărături",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Trimite WhatsApp")
                    }
                    Button(
                        onClick = onShoppingMode,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Magazin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun CreateListDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Listă Nouă de Cumpărături") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Denumire listă (ex: Metro Săptămânal)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notițe (opțional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), notes.trim().ifBlank { null }) },
                enabled = name.isNotBlank()
            ) {
                Text("Creează")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anulează")
            }
        }
    )
}
