package ro.smartshopping.list.ui.screens.agent

import android.content.Context
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.smartshopping.list.R
import ro.smartshopping.list.data.repository.ShoppingListRepository
import ro.smartshopping.list.data.repository.StoreDealsRepository
import ro.smartshopping.list.domain.model.AgentMatchResult
import ro.smartshopping.list.domain.model.BasketComparisonSummary
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.StoreDeal
import ro.smartshopping.list.domain.model.StoreDealNotification
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.usecase.SmartDiscountAgentUseCase
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import ro.smartshopping.list.util.StoreLocationHelper
import ro.smartshopping.list.util.StoreNotificationHelper
import java.util.Locale
import java.util.UUID

class SmartAgentViewModel(
    private val listRepository: ShoppingListRepository,
    private val dealsRepository: StoreDealsRepository,
    private val agentUseCase: SmartDiscountAgentUseCase
) : ViewModel() {

    private val _selectedListId = MutableStateFlow<String?>(null)
    val selectedListId = _selectedListId.asStateFlow()

    private val _agentSummary = MutableStateFlow<BasketComparisonSummary?>(null)
    val agentSummary = _agentSummary.asStateFlow()

    private val _selectedStoreFilter = MutableStateFlow<String>("Toate")
    val selectedStoreFilter = _selectedStoreFilter.asStateFlow()

    val activeLists: StateFlow<List<ShoppingList>> = listRepository.getAllListsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDeals: StateFlow<List<StoreDeal>> = dealsRepository.getAllDealsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<StoreDealNotification>> = dealsRepository.getNotificationsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            activeLists.collect { lists ->
                if (_selectedListId.value == null && lists.isNotEmpty()) {
                    selectList(lists.first().id)
                }
            }
        }
    }

    fun selectList(listId: String) {
        _selectedListId.value = listId
        refreshAgentAnalysis(listId)
    }

    fun setStoreFilter(filter: String) {
        _selectedStoreFilter.value = filter
    }

    fun refreshAgentAnalysis(listId: String? = _selectedListId.value) {
        if (listId == null) return
        viewModelScope.launch {
            val items = listRepository.getItemsForList(listId)
            val summary = agentUseCase(items)
            _agentSummary.value = summary
        }
    }

    fun applyDiscountsToList(context: Context, onApplied: () -> Unit) {
        val listId = _selectedListId.value ?: return
        val summary = _agentSummary.value ?: return

        viewModelScope.launch {
            for (match in summary.matches) {
                val deal = match.matchedDeal
                if (deal != null) {
                    val updated = match.item.copy(
                        estimatedUnitPrice = deal.discountPrice,
                        storeName = deal.storeName
                    )
                    listRepository.updateItem(updated)
                }
            }
            refreshAgentAnalysis(listId)
            onApplied()
        }
    }

    fun addDealToCurrentList(deal: StoreDeal) {
        val listId = _selectedListId.value ?: return
        viewModelScope.launch {
            val newItem = ShoppingListItem(
                id = UUID.randomUUID().toString(),
                shoppingListId = listId,
                productId = UUID.randomUUID().toString(),
                productNameSnapshot = deal.productName,
                quantity = 1.0,
                unit = deal.unit,
                packageCount = if (deal.unit in listOf(UnitOfMeasure.PCS, UnitOfMeasure.BOTTLE, UnitOfMeasure.CARTON, UnitOfMeasure.PACK)) 1 else null,
                estimatedUnitPrice = deal.discountPrice,
                actualUnitPrice = null,
                isPurchased = false,
                isInCart = false,
                category = deal.category,
                storeName = deal.storeName
            )
            listRepository.addItems(listOf(newItem))
            refreshAgentAnalysis(listId)
        }
    }

    fun triggerAutomatedNotification(context: Context, storeName: String) {
        viewModelScope.launch {
            val isKaufland = storeName.contains("Kaufland", ignoreCase = true)
            val title = if (isKaufland) {
                "Alertă Kaufland Militari: Reduceri Noi!"
            } else {
                "Alertă Auchan Militari: Prețuri Tăiate!"
            }
            val highlight = if (isKaufland) "-33% Lactate & Cafea Valea Cascadelor" else "-40% Fructe & Detergenți Bd. Iuliu Maniu"
            val message = if (isKaufland) {
                "Au fost detectate 4 produse din lista ta cu preț promoțional la Kaufland Militari."
            } else {
                "Super ofertă flash la Auchan Militari pentru coșul tău curent."
            }

            val notif = StoreDealNotification(
                id = UUID.randomUUID().toString(),
                storeName = storeName,
                title = title,
                message = message,
                discountHighlight = highlight,
                timestamp = System.currentTimeMillis()
            )
            dealsRepository.addNotification(notif)

            // Post native system notification
            StoreNotificationHelper.postStoreDealNotification(
                context = context,
                notificationId = (100..999).random(),
                storeName = storeName,
                title = title,
                message = message,
                discountHighlight = highlight
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAgentScreen(
    viewModel: SmartAgentViewModel,
    onNavigateToListDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activeLists by viewModel.activeLists.collectAsState()
    val selectedListId by viewModel.selectedListId.collectAsState()
    val summary by viewModel.agentSummary.collectAsState()
    val allDeals by viewModel.allDeals.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val storeFilter by viewModel.selectedStoreFilter.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Coș & Comparare", "Găsește pe Raft", "Catalog Reduceri", "Alerte Magazine (${notifications.size})")

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = {
                        val targetId = selectedListId ?: activeLists.firstOrNull()?.id
                        if (targetId != null) {
                            onNavigateToListDetail(targetId)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Înapoi la Listă"
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.app_brand_logo),
                            contentDescription = "Smart-list-Calculator",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Smart-list-Calculator", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Militari Residence & Chiajna • Oferte Reale", fontSize = 11.sp, color = SuccessGreenLight)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshAgentAnalysis() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reîmprospătează")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Active List Fast-Return Banner
            val currentSelectedList = activeLists.find { it.id == selectedListId } ?: activeLists.firstOrNull()
            if (currentSelectedList != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clickable { onNavigateToListDetail(currentSelectedList.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = Color(0xFF00FF88), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Listă Curentă: ${currentSelectedList.name}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = "Înapoi la Listă ➜",
                            color = Color(0xFF00FF88),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.5.sp
                        )
                    }
                }
            }
            // List Selector Chips if multiple lists exist
            if (activeLists.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(activeLists) { list ->
                        FilterChip(
                            selected = list.id == selectedListId,
                            onClick = { viewModel.selectList(list.id) },
                            label = { Text(list.name, maxLines = 1) },
                            leadingIcon = if (list.id == selectedListId) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
            }

            // Tab Navigation
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTab) {
                0 -> BasketComparisonTab(
                    summary = summary,
                    context = context,
                    onApplyDiscounts = {
                        viewModel.applyDiscountsToList(context) {}
                    }
                )
                1 -> AisleLocatorTab(
                    matches = summary?.matches ?: emptyList(),
                    context = context
                )
                2 -> DealsCatalogTab(
                    deals = allDeals,
                    currentFilter = storeFilter,
                    onFilterChange = { viewModel.setStoreFilter(it) },
                    onAddDeal = { deal -> viewModel.addDealToCurrentList(deal) }
                )
                3 -> NotificationsTab(
                    notifications = notifications,
                    onTriggerTest = { store -> viewModel.triggerAutomatedNotification(context, store) }
                )
            }
        }
    }
}

@Composable
fun BasketComparisonTab(
    summary: BasketComparisonSummary?,
    context: Context,
    onApplyDiscounts: () -> Unit
) {
    if (summary == null || summary.matches.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Nu există produse în lista selectată. Adaugă produse pentru ca Agentul să analizeze prețurile la Kaufland și Auchan Militari.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Holographic AI Insight Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF1B5E20).copy(alpha = 0.25f),
                                    Color(0xFF004D40).copy(alpha = 0.25f)
                                )
                            )
                        )
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = SuccessGreenLight,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Recomandare Agent Inteligent",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${summary.matchedItemsCount}/${summary.totalItemsCount} găsite",
                            style = MaterialTheme.typography.labelSmall,
                            color = SuccessGreenLight,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = summary.recommendedStrategy,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Store vs Store Comparison Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Kaufland Militari
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Kaufland Militari", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Valea Cascadelor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "${String.format(Locale.US, "%.2f", summary.kauflandTotal)} RON",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (summary.kauflandSavings > 0) {
                                    Text(
                                        "Economie: -${String.format(Locale.US, "%.2f", summary.kauflandSavings)} lei",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreenLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        // Auchan Militari
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Auchan Militari", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                                Text("Bd. Iuliu Maniu 560A", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    "${String.format(Locale.US, "%.2f", summary.auchanTotal)} RON",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (summary.auchanSavings > 0) {
                                    Text(
                                        "Economie: -${String.format(Locale.US, "%.2f", summary.auchanSavings)} lei",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SuccessGreenLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onApplyDiscounts,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Aplică reducerile în listă", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val target = if (summary.kauflandTotal <= summary.auchanTotal) {
                                    "Kaufland Valea Cascadelor Militari Bucuresti"
                                } else {
                                    "Auchan Militari Centrul Comercial Bucuresti"
                                }
                                StoreLocationHelper.openInGoogleMaps(context, target)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Harta Maps", fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Product Breakdown
        item {
            Text(
                "Comparare Produse Lista Ta (Kaufland vs Auchan):",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleSmall
            )
        }

        items(summary.matches, key = { it.item.id }) { match ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${match.item.productNameSnapshot} (${match.item.quantity} ${match.item.unit.symbolRo})",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )

                        if (match.matchedDeal != null) {
                            Text(
                                text = "-${match.matchedDeal.discountPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = SuccessGreenLight,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .background(SuccessGreenLight.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Kaufland deal info
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Kaufland:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (match.kauflandDeal != null) {
                                Text(
                                    "${String.format(Locale.US, "%.2f", match.kauflandDeal.discountPrice)} lei",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (match.bestStore?.contains("Kaufland") == true) SuccessGreenLight else MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text("Preț standard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Auchan deal info
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Auchan:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (match.auchanDeal != null) {
                                Text(
                                    "${String.format(Locale.US, "%.2f", match.auchanDeal.discountPrice)} lei",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (match.bestStore?.contains("Auchan") == true) SuccessGreenLight else MaterialTheme.colorScheme.onSurface
                                )
                            } else {
                                Text("Preț standard", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        // Recommendation
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("Recomandat:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                match.bestStore ?: "Oricare",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (match.aisleLocation != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = match.aisleLocation,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AisleLocatorTab(
    matches: List<AgentMatchResult>,
    context: Context
) {
    if (matches.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Nu există produse în lista selectată pentru localizare pe rafturi.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Storefront, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Găsire Rapidă la Raft în Militari", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        Text("Localizarea exactă a fiecărui produs pe raioane în Kaufland & Auchan Militari", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        items(matches, key = { it.item.id }) { match ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            match.item.productNameSnapshot,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp), tint = SuccessGreenLight)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                match.aisleLocation ?: "Raion General",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        match.bestStore?.let { store ->
                            Text(
                                text = "Magazin recomandat: $store",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    match.matchedDeal?.let { deal ->
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${String.format(Locale.US, "%.2f", deal.discountPrice)} lei",
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreenLight
                            )
                            Text(
                                "-${deal.discountPercent}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DealsCatalogTab(
    deals: List<StoreDeal>,
    currentFilter: String,
    onFilterChange: (String) -> Unit,
    onAddDeal: (StoreDeal) -> Unit
) {
    val context = LocalContext.current
    val storeOptions = listOf(
        "Toate",
        "Kaufland Rezervelor",
        "Lidl Tineretului",
        "Auchan Militari",
        "Penny Chiajna",
        "Metro Militari",
        "Mega Image"
    )

    val filteredDeals = deals.filter { deal ->
        when (currentFilter) {
            "Toate" -> true
            "Kaufland Rezervelor" -> deal.storeName.contains("Kaufland", ignoreCase = true)
            "Lidl Tineretului" -> deal.storeName.contains("Lidl", ignoreCase = true)
            "Auchan Militari" -> deal.storeName.contains("Auchan", ignoreCase = true)
            "Penny Chiajna" -> deal.storeName.contains("Penny", ignoreCase = true)
            "Metro Militari" -> deal.storeName.contains("Metro", ignoreCase = true)
            "Mega Image" -> deal.storeName.contains("Mega", ignoreCase = true)
            else -> deal.storeName.contains(currentFilter, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(storeOptions) { option ->
                    FilterChip(
                        selected = currentFilter == option,
                        onClick = { onFilterChange(option) },
                        label = { Text(option, fontSize = 12.sp) },
                        leadingIcon = if (currentFilter == option) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp)) }
                        } else null
                    )
                }
            }
        }

        items(filteredDeals, key = { it.id }) { deal ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    deal.productName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "-${deal.discountPercent}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreenLight,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .background(SuccessGreenLight.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${deal.storeName} • ${deal.storeBranch}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Proximity Badge
                            Text(
                                text = deal.proximityText,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF00F0FF),
                                fontWeight = FontWeight.Bold
                            )

                            // Shelf Location
                            Text(
                                text = "🧭 ${deal.aisleLocation}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${String.format(Locale.US, "%.2f", deal.discountPrice)} lei",
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                                color = SuccessGreenLight
                            )
                            Text(
                                "${String.format(Locale.US, "%.2f", deal.originalPrice)} lei",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons: GPS Navigation + Add to List
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                StoreLocationHelper.openStoreInGoogleMaps(context, deal.storeName, deal.storeBranch)
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(15.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hartă GPS", fontSize = 11.5.sp)
                        }

                        Button(
                            onClick = { onAddDeal(deal) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pune în Listă", fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsTab(
    notifications: List<StoreDealNotification>,
    onTriggerTest: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sistem Notificări Automate Magazine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Notificările automate te anunță direct în bara de stare când Kaufland sau Auchan Militari introduc promoții pentru produsele din lista ta.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onTriggerTest("Kaufland Militari") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Notificare Kaufland", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onTriggerTest("Auchan Militari") },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Test Notificare Auchan", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Text("Istoric Alerte Primite din Militari:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
        }

        items(notifications, key = { it.id }) { notif ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalOffer, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(notif.storeName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            notif.discountHighlight,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreenLight
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(notif.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(notif.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
