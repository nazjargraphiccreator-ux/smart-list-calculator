package ro.smartshopping.list.ui.screens.lists

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.smartshopping.list.data.repository.PriceTrackingRepository
import ro.smartshopping.list.data.repository.ProductRepository
import ro.smartshopping.list.data.repository.ShoppingListRepository
import ro.smartshopping.list.domain.model.FinancialSummary
import ro.smartshopping.list.domain.model.ItemPurchaseStage
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.parser.ParsedShoppingItem
import ro.smartshopping.list.domain.parser.SmartTextImportParser
import ro.smartshopping.list.domain.usecase.CalculateFinancialSummaryUseCase
import ro.smartshopping.list.domain.usecase.FormatListForSharingUseCase
import ro.smartshopping.list.domain.usecase.RecordPurchasePriceUseCase
import ro.smartshopping.list.domain.model.StoreDeal
import ro.smartshopping.list.ui.components.FinancialSummaryCard
import ro.smartshopping.list.ui.components.LiveBudgetCountdownCard
import ro.smartshopping.list.ui.components.ProductBarcodeScannerDialog
import ro.smartshopping.list.ui.components.StoreDealsBanner
import ro.smartshopping.list.ui.components.StorePickerDialog
import ro.smartshopping.list.ui.components.TextImportDialog
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import ro.smartshopping.list.util.ShareIntentHelper
import ro.smartshopping.list.util.StoreLocationHelper
import java.util.Locale
import java.util.UUID

class ListDetailViewModel(
    private val listId: String,
    private val listRepository: ShoppingListRepository,
    val productRepository: ProductRepository,
    private val priceRepository: PriceTrackingRepository,
    private val financialCalculator: CalculateFinancialSummaryUseCase,
    private val recordPriceUseCase: RecordPurchasePriceUseCase,
    private val formatShareUseCase: FormatListForSharingUseCase,
    val storeDealsRepository: ro.smartshopping.list.data.repository.StoreDealsRepository? = null
) : ViewModel() {

    val uiState: StateFlow<ListDetailUiState> = combine(
        listRepository.getListByIdFlow(listId),
        listRepository.getItemsForListFlow(listId)
    ) { list, items ->
        val summary = financialCalculator(
            items = items,
            currency = list?.currency ?: "RON",
            budget = list?.budget,
            discountAmount = list?.discountAmount,
            extraFeesAmount = list?.extraFeesAmount,
            receiptTotal = list?.receiptTotal
        )
        ListDetailUiState(list = list, items = items, summary = summary, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListDetailUiState())

    fun toggleItemPurchase(item: ShoppingListItem) {
        viewModelScope.launch {
            val newStatus = !item.isPurchased
            val actualPrice = if (newStatus) (item.actualUnitPrice ?: item.estimatedUnitPrice) else null
            listRepository.togglePurchaseStatus(item.id, newStatus, actualPrice)

            if (newStatus) {
                val record = recordPriceUseCase(
                    item = item.copy(isPurchased = true, actualUnitPrice = actualPrice),
                    currency = uiState.value.list?.currency ?: "RON",
                    storeName = uiState.value.list?.storeName,
                    storeAddress = uiState.value.list?.storeAddress
                )
                if (record != null) {
                    priceRepository.recordPrice(record)
                }
            }
        }
    }

    fun toggleItemCart(item: ShoppingListItem) {
        viewModelScope.launch {
            listRepository.updateCartStatus(item.id, !item.isInCart)
        }
    }

    fun updateItemPurchaseStage(itemId: String, stage: ItemPurchaseStage, actualPrice: Double? = null) {
        viewModelScope.launch {
            val item = uiState.value.items.find { it.id == itemId } ?: return@launch
            val effectivePrice = actualPrice ?: item.actualUnitPrice ?: item.estimatedUnitPrice
            listRepository.updateItemThreeStage(itemId, stage, if (stage == ItemPurchaseStage.PURCHASED) effectivePrice else null)

            if (stage == ItemPurchaseStage.PURCHASED) {
                val record = recordPriceUseCase(
                    item = item.copy(isPurchased = true, actualUnitPrice = effectivePrice),
                    currency = uiState.value.list?.currency ?: "RON",
                    storeName = uiState.value.list?.storeName,
                    storeAddress = uiState.value.list?.storeAddress
                )
                if (record != null) {
                    priceRepository.recordPrice(record)
                }
            }
        }
    }

    fun updateItemQuantity(itemId: String, quantity: Double) {
        viewModelScope.launch {
            val item = uiState.value.items.find { it.id == itemId } ?: return@launch
            val updated = item.copy(quantity = quantity.coerceAtLeast(0.01))
            listRepository.updateItem(updated)
        }
    }

    fun addItemToList(name: String, quantity: Double, unit: UnitOfMeasure, estimatedPrice: Double?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val product = productRepository.findOrCreateProduct(name, defaultUnit = unit, estimatedPrice = estimatedPrice)
            val newItem = ShoppingListItem(
                id = UUID.randomUUID().toString(),
                shoppingListId = listId,
                productId = product.id,
                productNameSnapshot = product.name,
                quantity = quantity,
                unit = unit,
                estimatedUnitPrice = estimatedPrice ?: product.lastKnownPrice,
                actualUnitPrice = null,
                isPurchased = false,
                isInCart = false,
                category = product.category
            )
            listRepository.addItem(newItem)
        }
    }

    fun importParsedItems(parsedItems: List<ParsedShoppingItem>) {
        if (parsedItems.isEmpty()) return
        viewModelScope.launch {
            val newItems = mutableListOf<ShoppingListItem>()
            for (parsed in parsedItems) {
                val product = productRepository.findOrCreateProduct(
                    name = parsed.name,
                    category = parsed.category,
                    defaultUnit = parsed.unit,
                    estimatedPrice = parsed.unitPrice
                )
                val newItem = ShoppingListItem(
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
                    category = product.category
                )
                newItems.add(newItem)
            }
            listRepository.addItems(newItems)
        }
    }

    fun updateStore(storeName: String, storeAddress: String?) {
        viewModelScope.launch {
            val current = uiState.value.list ?: return@launch
            listRepository.updateStoreAndBudget(
                id = listId,
                storeName = storeName,
                storeAddress = storeAddress,
                lat = current.storeLatitude,
                lng = current.storeLongitude,
                budget = current.budget,
                receiptTotal = current.receiptTotal,
                discount = current.discountAmount,
                extraFees = current.extraFeesAmount
            )
        }
    }

    fun updateListBudget(budget: Double?) {
        viewModelScope.launch {
            val current = uiState.value.list ?: return@launch
            listRepository.updateStoreAndBudget(
                id = listId,
                storeName = current.storeName,
                storeAddress = current.storeAddress,
                lat = current.storeLatitude,
                lng = current.storeLongitude,
                budget = budget,
                receiptTotal = current.receiptTotal,
                discount = current.discountAmount,
                extraFees = current.extraFeesAmount
            )
        }
    }

    fun updateListDiscountsAndFees(discount: Double?, extraFees: Double?) {
        viewModelScope.launch {
            val current = uiState.value.list ?: return@launch
            listRepository.updateStoreAndBudget(
                id = listId,
                storeName = current.storeName,
                storeAddress = current.storeAddress,
                lat = current.storeLatitude,
                lng = current.storeLongitude,
                budget = current.budget,
                receiptTotal = current.receiptTotal,
                discount = discount,
                extraFees = extraFees
            )
        }
    }

    fun updateListReceiptTotal(receiptTotal: Double?) {
        viewModelScope.launch {
            val current = uiState.value.list ?: return@launch
            listRepository.updateStoreAndBudget(
                id = listId,
                storeName = current.storeName,
                storeAddress = current.storeAddress,
                lat = current.storeLatitude,
                lng = current.storeLongitude,
                budget = current.budget,
                receiptTotal = receiptTotal,
                discount = current.discountAmount,
                extraFees = current.extraFeesAmount
            )
        }
    }

    fun updateItemPrice(itemId: String, newPrice: Double?) {
        viewModelScope.launch {
            val item = uiState.value.items.find { it.id == itemId } ?: return@launch
            val updated = item.copy(estimatedUnitPrice = newPrice)
            listRepository.updateItem(updated)
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            listRepository.deleteItem(itemId)
        }
    }

    fun shareList(context: android.content.Context) {
        val list = uiState.value.list ?: return
        val items = uiState.value.items
        val shareText = formatShareUseCase(list, items, isRomanian = true)
        ShareIntentHelper.shareText(context, shareText, "Trimite: ${list.name}")
    }

    fun addOrUpdateProductFromBarcode(
        name: String,
        quantity: Double,
        unit: UnitOfMeasure,
        price: Double?,
        barcode: String,
        putInCart: Boolean = true
    ) {
        viewModelScope.launch {
            val existing = uiState.value.items.find {
                it.productNameSnapshot.equals(name, ignoreCase = true) || it.notes?.contains(barcode) == true
            }
            if (existing != null) {
                val updated = existing.copy(
                    actualUnitPrice = price ?: existing.actualUnitPrice,
                    estimatedUnitPrice = price ?: existing.estimatedUnitPrice,
                    isInCart = putInCart,
                    isPurchased = false,
                    notes = if (existing.notes.isNullOrBlank()) "Cod: $barcode" else existing.notes
                )
                listRepository.updateItem(updated)
            } else {
                val product = productRepository.findOrCreateProduct(
                    name = name,
                    defaultUnit = unit,
                    estimatedPrice = price
                ).copy(barcode = barcode)
                productRepository.saveProduct(product)

                val newItem = ShoppingListItem(
                    id = UUID.randomUUID().toString(),
                    shoppingListId = listId,
                    productId = product.id,
                    productNameSnapshot = product.name,
                    quantity = quantity,
                    unit = unit,
                    estimatedUnitPrice = price ?: product.lastKnownPrice,
                    actualUnitPrice = if (putInCart) price else null,
                    isPurchased = false,
                    isInCart = putInCart,
                    category = product.category,
                    notes = "Cod: $barcode"
                )
                listRepository.addItem(newItem)
            }
        }
    }

    fun applyDiscountPriceToItem(productName: String, discountPrice: Double) {
        viewModelScope.launch {
            val item = uiState.value.items.find {
                it.productNameSnapshot.contains(productName, ignoreCase = true) ||
                productName.contains(it.productNameSnapshot, ignoreCase = true)
            } ?: return@launch

            val updated = item.copy(
                estimatedUnitPrice = discountPrice,
                actualUnitPrice = if (item.isInCart || item.isPurchased) discountPrice else item.actualUnitPrice
            )
            listRepository.updateItem(updated)
        }
    }
}

data class ListDetailUiState(
    val list: ShoppingList? = null,
    val items: List<ShoppingListItem> = emptyList(),
    val summary: FinancialSummary = FinancialSummary(0.0, 0.0, 0.0, 0.0, 0, 0, 0, 0),
    val isLoading: Boolean = true
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListDetailScreen(
    viewModel: ListDetailViewModel,
    parser: SmartTextImportParser,
    onNavigateBack: () -> Unit,
    onNavigateToShoppingMode: () -> Unit,
    onNavigateToPrePayment: () -> Unit,
    activeSessionManager: ro.smartshopping.list.data.session.ActiveShoppingSessionManager? = null,
    onNavigateToWallet: (() -> Unit)? = null,
    onNavigateToAgent: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showImportDialog by remember { mutableStateOf(false) }
    var showStoreDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var editingPriceItem by remember { mutableStateOf<ShoppingListItem?>(null) }
    var matchingDeals by remember { mutableStateOf<List<StoreDeal>>(emptyList()) }

    LaunchedEffect(state.items) {
        val names = state.items.map { it.productNameSnapshot }
        matchingDeals = viewModel.storeDealsRepository?.getDealsMatchingList(names) ?: emptyList()
    }

    // Sync with ActiveShoppingSessionManager so all tabs know which list is active
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

    var newItemName by remember { mutableStateOf("") }
    var newItemQty by remember { mutableStateOf("1") }
    var newItemUnit by remember { mutableStateOf(UnitOfMeasure.PCS) }
    var newItemPrice by remember { mutableStateOf("") }
    var isExpandedUnitMenu by remember { mutableStateOf(false) }

    val currency = state.list?.currency ?: "RON"

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.list?.name ?: "Detalii Listă", fontWeight = FontWeight.Bold, maxLines = 1)
                        if (!state.list?.storeName.isNullOrBlank()) {
                            Text(
                                text = "📍 ${state.list?.storeName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                },
                actions = {
                    // Barcode Scanner Button (Shelf Scanner)
                    IconButton(onClick = { showBarcodeScanner = true }) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan la Raft",
                            tint = Color(0xFF00FF88)
                        )
                    }

                    // Smart Text Import Button
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Import Text", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Pre-payment Calculator Button
                    IconButton(onClick = onNavigateToPrePayment) {
                        Icon(Icons.Default.Calculate, contentDescription = "Calcul Înainte de Plată")
                    }

                    // Store Location Button
                    IconButton(onClick = { showStoreDialog = true }) {
                        Icon(Icons.Default.Store, contentDescription = "Magazin")
                    }

                    // Salvare locală fișier TXT în folderul dedicat telefonului
                    IconButton(onClick = {
                        val currentList = state.list
                        if (currentList != null) {
                            val file = ro.smartshopping.list.data.local.LocalBackupStorageManager.exportListToTxt(
                                context = context,
                                list = currentList,
                                items = state.items
                            )
                            android.widget.Toast.makeText(
                                context,
                                "Fișier TXT salvat în telefon:\n${file.name}\nFolder: Documents/SmartListCalculator",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }) {
                        Icon(Icons.Default.Save, contentDescription = "Salvează fișier TXT pe telefon")
                    }

                    // Share
                    IconButton(onClick = { viewModel.shareList(context) }) {
                        Icon(Icons.Default.Share, contentDescription = "Trimite")
                    }

                    // Shopping Mode Button
                    Button(
                        onClick = onNavigateToShoppingMode,
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Magazin")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Store Banner if present (with direct Google Maps action)
            state.list?.let { list ->
                if (!list.storeName.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = list.storeName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (!list.storeAddress.isNullOrBlank()) {
                                        Text(
                                            text = list.storeAddress,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    StoreLocationHelper.openStoreInGoogleMaps(context, list.storeName, list.storeAddress)
                                }) {
                                    Icon(Icons.Default.Map, contentDescription = "Deschide Maps", tint = MaterialTheme.colorScheme.primary)
                                }
                                TextButton(onClick = { showStoreDialog = true }) {
                                    Text("Schimbă")
                                }
                            }
                        }
                    }
                }
            }

            // Live Budget Countdown Card (Real-Time Subtraction + Exceed Alert)
            LiveBudgetCountdownCard(
                summary = state.summary,
                listName = state.list?.name ?: "Listă Cumpărături",
                onUpdateBudget = { newBudget ->
                    viewModel.updateListBudget(newBudget)
                },
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Official Store Deals Banner (Metro, Kaufland, Auchan, Lidl)
            if (matchingDeals.isNotEmpty()) {
                StoreDealsBanner(
                    matchingDeals = matchingDeals,
                    onApplyDealPrice = { name, discountPrice ->
                        viewModel.applyDiscountPriceToItem(name, discountPrice)
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Financial Summary Card with budget tracking & in-cart live values
            FinancialSummaryCard(summary = state.summary, modifier = Modifier.padding(bottom = 8.dp))

            // Action row: Pre-Payment Calculator & Import Text shortcuts
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onNavigateToPrePayment,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Calcul Înainte de Plată", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Import Text Rapid", fontSize = 12.sp)
                }
            }

            // Items List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.items, key = { it.id }) { item ->
                    ShoppingListItemRow(
                        item = item,
                        currency = currency,
                        onToggle = { viewModel.toggleItemPurchase(item) },
                        onToggleCart = { viewModel.toggleItemCart(item) },
                        onUpdateQuantity = { newQty -> viewModel.updateItemQuantity(item.id, newQty) },
                        onEditPrice = { editingPriceItem = item },
                        onDelete = { viewModel.deleteItem(item.id) }
                    )
                }
            }

            // Quick Add Product Bar with Stepper & Unit
            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = newItemName,
                            onValueChange = { newItemName = it },
                            placeholder = { Text("Produs nou (ex: Mere)") },
                            modifier = Modifier.weight(2f),
                            singleLine = true
                        )

                        // Stepper (-)
                        IconButton(
                            onClick = {
                                val current = newItemQty.toDoubleOrNull() ?: 1.0
                                val step = if (newItemUnit == UnitOfMeasure.KG || newItemUnit == UnitOfMeasure.L) 0.5 else 1.0
                                val next = (current - step).coerceAtLeast(0.1)
                                newItemQty = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                            },
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        OutlinedTextField(
                            value = newItemQty,
                            onValueChange = { newItemQty = it },
                            modifier = Modifier.width(55.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        // Stepper (+)
                        IconButton(
                            onClick = {
                                val current = newItemQty.toDoubleOrNull() ?: 1.0
                                val step = if (newItemUnit == UnitOfMeasure.KG || newItemUnit == UnitOfMeasure.L) 0.5 else 1.0
                                val next = current + step
                                newItemQty = if (next % 1.0 == 0.0) next.toInt().toString() else next.toString()
                            },
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        }

                        // Unit Menu
                        Box {
                            OutlinedButton(
                                onClick = { isExpandedUnitMenu = true },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Text(newItemUnit.getSymbol(true))
                            }
                            DropdownMenu(
                                expanded = isExpandedUnitMenu,
                                onDismissRequest = { isExpandedUnitMenu = false }
                            ) {
                                UnitOfMeasure.entries.forEach { u ->
                                    DropdownMenuItem(
                                        text = { Text(u.symbolRo) },
                                        onClick = {
                                            newItemUnit = u
                                            isExpandedUnitMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = newItemPrice,
                            onValueChange = { newItemPrice = it },
                            placeholder = { Text("Preț") },
                            modifier = Modifier.weight(1.1f),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        IconButton(
                            onClick = {
                                val qty = newItemQty.replace(',', '.').toDoubleOrNull() ?: 1.0
                                val price = newItemPrice.replace(',', '.').toDoubleOrNull()
                                viewModel.addItemToList(newItemName, qty, newItemUnit, price)
                                newItemName = ""
                                newItemQty = "1"
                                newItemPrice = ""
                            },
                            enabled = newItemName.isNotBlank()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Adaugă")
                        }
                    }
                }
            }
        }
    }

    // Smart Text Import Dialog
    if (showImportDialog) {
        TextImportDialog(
            parser = parser,
            onDismiss = { showImportDialog = false },
            onConfirmImport = { items ->
                viewModel.importParsedItems(items)
                showImportDialog = false
            }
        )
    }

    // Store Picker Dialog
    if (showStoreDialog) {
        StorePickerDialog(
            currentStoreName = state.list?.storeName,
            currentStoreAddress = state.list?.storeAddress,
            onDismiss = { showStoreDialog = false },
            onConfirm = { name, address ->
                viewModel.updateStore(name, address)
                showStoreDialog = false
            }
        )
    }

    // Price Edit Dialog
    editingPriceItem?.let { item ->
        var tempPrice by remember { mutableStateOf(item.estimatedUnitPrice?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { editingPriceItem = null },
            title = { Text("Modifică preț: ${item.productNameSnapshot}") },
            text = {
                OutlinedTextField(
                    value = tempPrice,
                    onValueChange = { tempPrice = it },
                    label = { Text("Preț unitar (RON)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateItemPrice(item.id, tempPrice.replace(',', '.').toDoubleOrNull())
                    editingPriceItem = null
                }) {
                    Text("Salvează")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingPriceItem = null }) {
                    Text("Anulează")
                }
            }
        )
    }

    // Shelf Product Barcode Scanner Dialog
    if (showBarcodeScanner) {
        ProductBarcodeScannerDialog(
            itemsInList = state.items,
            currency = currency,
            productRepository = viewModel.productRepository,
            onPutInCart = { itemId, price ->
                viewModel.updateItemPurchaseStage(itemId, ItemPurchaseStage.IN_CART, price)
            },
            onAddNewProductToCart = { name, quantity, unit, price, barcode ->
                viewModel.addOrUpdateProductFromBarcode(name, quantity, unit, price, barcode, putInCart = true)
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }
}

@Composable
fun ShoppingListItemRow(
    item: ShoppingListItem,
    currency: String,
    onToggle: () -> Unit,
    onToggleCart: () -> Unit,
    onUpdateQuantity: (Double) -> Unit,
    onEditPrice: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.purchaseStage) {
                ItemPurchaseStage.PURCHASED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ItemPurchaseStage.IN_CART -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ItemPurchaseStage.TO_BUY -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3-Stage Progress Indicator Target
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            when (item.purchaseStage) {
                                ItemPurchaseStage.PURCHASED -> SuccessGreenLight
                                ItemPurchaseStage.IN_CART -> MaterialTheme.colorScheme.primary
                                ItemPurchaseStage.TO_BUY -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)
                            }
                        )
                        .clickable { onToggle() },
                    contentAlignment = Alignment.Center
                ) {
                    when (item.purchaseStage) {
                        ItemPurchaseStage.PURCHASED -> Icon(Icons.Default.Check, contentDescription = "Plătit", tint = Color.White, modifier = Modifier.size(18.dp))
                        ItemPurchaseStage.IN_CART -> Icon(Icons.Default.ShoppingCart, contentDescription = "În coș", tint = Color.White, modifier = Modifier.size(18.dp))
                        ItemPurchaseStage.TO_BUY -> {}
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productNameSnapshot,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity} ${item.unit.getSymbol(true)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = " • ${item.category}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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

                // Price display
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(end = 4.dp)) {
                    val price = item.effectiveUnitPrice
                    if (price != null && price > 0.0) {
                        Text(
                            text = "${String.format(Locale.US, "%.2f", item.calculatedLineTotal)} $currency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", price)} $currency/u",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        TextButton(onClick = onEditPrice) {
                            Text("+ Preț", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Șterge", tint = MaterialTheme.colorScheme.error)
                }
            }

            // Quick Stepper & In-Cart Toggle Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
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
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(14.dp))
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity} ${item.unit.getSymbol(true)}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = {
                            val step = if (item.unit == UnitOfMeasure.KG || item.unit == UnitOfMeasure.L) 0.5 else 1.0
                            onUpdateQuantity(item.quantity + step)
                        },
                        modifier = Modifier.size(30.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }

                // Quick Toggle In-Cart Button
                if (!item.isPurchased) {
                    TextButton(onClick = onToggleCart) {
                        Icon(Icons.Default.ShoppingCart, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (item.isInCart) "Scoate din coș" else "Pune în coș", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
