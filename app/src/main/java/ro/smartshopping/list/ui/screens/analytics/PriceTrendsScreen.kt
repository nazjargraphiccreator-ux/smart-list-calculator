package ro.smartshopping.list.ui.screens.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import ro.smartshopping.list.data.repository.PriceTrackingRepository
import ro.smartshopping.list.data.repository.ProductRepository
import ro.smartshopping.list.domain.model.ProductPriceHistory
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import java.util.Locale

class PriceTrendsViewModel(
    private val productRepository: ProductRepository,
    private val priceRepository: PriceTrackingRepository
) : ViewModel() {

    val priceTrendsState: StateFlow<List<ProductPriceHistory>> = productRepository.getAllProductsFlow()
        .map { products ->
            products.mapNotNull { p ->
                val history = priceRepository.getProductPriceHistory(p.id)
                if (history.records.isNotEmpty()) history else null
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceTrendsScreen(
    viewModel: PriceTrendsViewModel,
    modifier: Modifier = Modifier
) {
    val histories by viewModel.priceTrendsState.collectAsState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Evoluție & Istoric Prețuri", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = "Monitorizare prețuri reale înregistrate la cumpărături",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (histories.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Nu există înregistrări de preț suficiente.",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Bifează produse ca și cumpărate pentru a începe monitorizarea.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(histories, key = { it.productId }) { history ->
                        PriceHistoryCard(history = history)
                    }
                }
            }
        }
    }
}

@Composable
fun PriceHistoryCard(
    history: ProductPriceHistory,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = history.productName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                history.latestPrice?.let { latest ->
                    Text(
                        text = "${String.format(Locale.US, "%.2f", latest)} ${history.currency}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Historical breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Preț Minim:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%.2f", history.lowestPrice ?: 0.0)} ${history.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreenLight
                    )
                }

                Column {
                    Text("Preț Maxim:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%.2f", history.highestPrice ?: 0.0)} ${history.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Column {
                    Text("Preț Mediu:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "${String.format(Locale.US, "%.2f", history.averagePrice ?: 0.0)} ${history.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (history.priceChangePercentage != null) {
                Spacer(modifier = Modifier.height(10.dp))
                val isIncrease = history.priceChangePercentage > 0
                val prefix = if (isIncrease) "+" else ""
                val trendColor = if (isIncrease) MaterialTheme.colorScheme.error else SuccessGreenLight

                Text(
                    text = "Variație față de ultima cumpărare: $prefix${String.format(Locale.US, "%.1f", history.priceChangePercentage)}%",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = trendColor
                )
            }

            if (history.storeComparisons.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Prețuri înregistrate pe magazine:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                val globalMinPrice = history.lowestPrice ?: 0.0

                history.storeComparisons.forEach { storeComp ->
                    val isCheapest = storeComp.lowestPrice <= globalMinPrice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "• ${storeComp.storeName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isCheapest) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCheapest) SuccessGreenLight else MaterialTheme.colorScheme.onSurface
                            )
                            if (isCheapest) {
                                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "★ cel mai ieftin",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SuccessGreenLight,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Text(
                            text = "${String.format(Locale.US, "%.2f", storeComp.latestPrice)} ${storeComp.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isCheapest) SuccessGreenLight else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
