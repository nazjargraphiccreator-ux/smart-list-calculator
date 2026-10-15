package ro.smartshopping.list.ui.screens.lists

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import ro.smartshopping.list.data.repository.ProductRepository
import ro.smartshopping.list.data.repository.ShoppingListRepository
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.ShoppingListStatus
import ro.smartshopping.list.domain.parser.ParsedShoppingItem
import ro.smartshopping.list.domain.parser.SmartTextImportParser
import ro.smartshopping.list.domain.usecase.FormatListForSharingUseCase
import ro.smartshopping.list.ui.components.CompletionProgressBar
import ro.smartshopping.list.ui.components.TextImportDialog
import ro.smartshopping.list.util.ShareIntentHelper
import java.util.Locale
import java.util.UUID

class ListsViewModel(
    private val repository: ShoppingListRepository,
    private val formatShareUseCase: FormatListForSharingUseCase
) : ViewModel() {

    private val searchQuery = MutableStateFlow("")

    val uiState: StateFlow<ListsUiState> = combine(
        repository.getListsByStatusFlow(ShoppingListStatus.ACTIVE),
        searchQuery
    ) { lists, query ->
        val filtered = if (query.isBlank()) {
            lists
        } else {
            lists.filter { it.name.contains(query, ignoreCase = true) || (it.notes?.contains(query, ignoreCase = true) == true) }
        }
        ListsUiState(lists = filtered, query = query, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListsUiState())

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
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

    fun shareList(context: android.content.Context, list: ShoppingList) {
        viewModelScope.launch {
            val items = repository.getItemsForList(list.id)
            val shareText = formatShareUseCase(list, items, isRomanian = true)
            ShareIntentHelper.shareText(context, shareText, "Trimite: ${list.name}")
        }
    }
}

data class ListsUiState(
    val lists: List<ShoppingList> = emptyList(),
    val query: String = "",
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
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Liste Cumpărături 2026", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showImportDialog = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Importă Text WhatsApp", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Creează Listă")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Search Bar & Import Text Shortcut Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Caută în listele active...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                OutlinedButton(
                    onClick = { showImportDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Import", fontSize = 12.sp)
                }
            }

            if (state.lists.isEmpty() && !state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (state.query.isNotBlank()) "Nicio listă găsită" else "Nu ai nicio listă activă",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { showCreateDialog = true }) {
                                Text("+ Creează Listă")
                            }
                            OutlinedButton(onClick = { showImportDialog = true }) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Lipește din WhatsApp")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
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
    }

    if (showCreateDialog) {
        CreateListDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, notes ->
                viewModel.createNewList(name, notes)
                showCreateDialog = false
            }
        )
    }

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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = list.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (!list.storeName.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = list.storeName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    if (!list.notes.isNullOrBlank()) {
                        Text(
                            text = list.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
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

            Spacer(modifier = Modifier.height(10.dp))

            // Completion Progress
            CompletionProgressBar(
                purchasedCount = list.purchasedItemsCount,
                totalCount = list.totalItemsCount,
                completionPercentage = list.completionPercentage
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Totals and Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Total Estimat: ${String.format(Locale.US, "%.2f", list.estimatedTotal)} ${list.currency}",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (list.actualTotal > 0.0) {
                        Text(
                            text = "Cheltuit: ${String.format(Locale.US, "%.2f", list.actualTotal)} ${list.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Trimite")
                    }
                    Button(
                        onClick = onShoppingMode,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Magazin")
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
                    label = { Text("Denumire listă (ex: Cumpărături weekend)") },
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
