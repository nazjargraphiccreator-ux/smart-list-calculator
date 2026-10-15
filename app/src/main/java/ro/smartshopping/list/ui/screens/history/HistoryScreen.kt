package ro.smartshopping.list.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ro.smartshopping.list.data.repository.ShoppingListRepository
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListStatus
import ro.smartshopping.list.domain.usecase.CarryForwardUnpurchasedUseCase
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryViewModel(
    private val repository: ShoppingListRepository,
    private val carryForwardUseCase: CarryForwardUnpurchasedUseCase
) : ViewModel() {

    val completedListsState: StateFlow<List<ShoppingList>> = repository.getListsByStatusFlow(ShoppingListStatus.COMPLETED)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun carryForwardUnpurchased(sourceList: ShoppingList, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val sourceItems = repository.getItemsForList(sourceList.id)
            val newListName = "Restanțe: ${sourceList.name}"
            val newListId = repository.createList(name = newListName, currency = sourceList.currency)

            val carriedItems = carryForwardUseCase(
                sourceItems = sourceItems,
                targetShoppingListId = newListId
            )
            repository.addItems(carriedItems)
            onCreated(newListId)
        }
    }

    fun repeatList(sourceList: ShoppingList, onCreated: (String) -> Unit) {
        viewModelScope.launch {
            val newListId = repository.duplicateList(
                sourceListId = sourceList.id,
                newName = "Repetă: ${sourceList.name}",
                preservePrices = true
            )
            onCreated(newListId)
        }
    }

    fun reopenList(listId: String) {
        viewModelScope.launch {
            repository.updateListStatus(listId, ShoppingListStatus.ACTIVE)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateToListDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val completedLists by viewModel.completedListsState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Istoric Cumpărături & Reutilizare", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            if (completedLists.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nu ai nicio sesiune finalizată în istoric.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Când finalizezi o listă activă, o poți reutiliza aici.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(completedLists, key = { it.id }) { list ->
                        HistoryListCard(
                            list = list,
                            onCarryForward = {
                                viewModel.carryForwardUnpurchased(list) { newId ->
                                    onNavigateToListDetail(newId)
                                }
                            },
                            onRepeat = {
                                viewModel.repeatList(list) { newId ->
                                    onNavigateToListDetail(newId)
                                }
                            },
                            onReopen = { viewModel.reopenList(list.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryListCard(
    list: ShoppingList,
    onCarryForward: () -> Unit,
    onRepeat: () -> Unit,
    onReopen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ro", "RO"))
    val dateText = list.completedAt?.let { dateFormat.format(Date(it)) } ?: "Finalizată"

    Card(
        modifier = modifier.fillMaxWidth(),
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
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "${String.format(Locale.US, "%.2f", list.actualTotal)} ${list.currency}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!list.storeName.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Store,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = list.storeName + (list.storeAddress?.let { " • $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Produse achiziționate: ${list.purchasedItemsCount} din ${list.totalItemsCount} (${list.completionPercentage}%)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (list.receiptTotal != null || list.budget != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    list.receiptTotal?.let { receipt ->
                        val diff = receipt - list.actualTotal
                        val diffSign = if (diff >= 0) "+" else ""
                        Text(
                            text = "Bon: ${String.format(Locale.US, "%.2f", receipt)} ${list.currency} ($diffSign${String.format(Locale.US, "%.2f", diff)})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    list.budget?.let { b ->
                        val totalPaid = list.receiptTotal ?: list.actualTotal
                        val remaining = b - totalPaid
                        val isOver = remaining < 0
                        Text(
                            text = "Buget: ${String.format(Locale.US, "%.2f", b)} (${if (isOver) "Depășit cu ${String.format(Locale.US, "%.2f", -remaining)}" else "Rămas ${String.format(Locale.US, "%.2f", remaining)}"})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            val unpurchasedCount = list.totalItemsCount - list.purchasedItemsCount

            Spacer(modifier = Modifier.height(14.dp))

            // Reuse Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (unpurchasedCount > 0) {
                    Button(
                        onClick = onCarryForward,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Transferă ($unpurchasedCount)", style = MaterialTheme.typography.bodySmall)
                    }
                }

                OutlinedButton(
                    onClick = onRepeat,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Repetă tot", style = MaterialTheme.typography.bodySmall)
                }

                IconButton(onClick = onReopen) {
                    Icon(Icons.Default.Replay, contentDescription = "Redeschide")
                }
            }
        }
    }
}
