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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Discount
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import ro.smartshopping.list.domain.model.ItemPurchaseStage
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.ui.screens.lists.ListDetailViewModel
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrePaymentCalculatorScreen(
    viewModel: ListDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showBudgetDialog by remember { mutableStateOf(false) }
    var showDiscountDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }

    val currency = state.list?.currency ?: "RON"
    val summary = state.summary

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Calculate, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Calcul înainte de plată",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = state.list?.name ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Înapoi")
                    }
                },
                actions = {
                    IconButton(onClick = { showReceiptDialog = true }) {
                        Icon(Icons.Default.Receipt, contentDescription = "Compară Bon Fiscal")
                    }
                }
            )
        },
        bottomBar = {
            // Sticky Summary Card at the bottom with quick checkout action
            Surface(
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 8.dp,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Estimativ În Coș",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", summary.inCartTotal)} $currency",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreenLight
                            )
                        }

                        Button(
                            onClick = { showReceiptDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Compară cu Bonul", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Mandatory Disclaimer Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Totalul este estimativ până când toate prețurile și reducerile sunt confirmate la casă.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // 2. Budget and Financial Real-time Calculation Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Buget & Cheltuieli", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            TextButton(onClick = { showBudgetDialog = true }) {
                                Text(if (summary.budget != null) "Modifică Buget" else "+ Setează Buget")
                            }
                        }

                        if (summary.budget != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Buget stabilit:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format(Locale.US, "%.2f", summary.budget)} $currency", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Diferență buget:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                val rem = summary.remainingBudget ?: 0.0
                                val remText = if (rem >= 0) "Rămas: +${String.format(Locale.US, "%.2f", rem)} $currency" else "Depășit: ${String.format(Locale.US, "%.2f", rem)} $currency"
                                Text(
                                    text = remText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (rem >= 0) SuccessGreenLight else MaterialTheme.colorScheme.error
                                )
                            }

                            if (summary.isOverBudget) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.errorContainer)
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Atenție: Depășești bugetul cu ${String.format(Locale.US, "%.2f", summary.overBudgetAmount)} $currency!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(10.dp))

                        // Real-time breakdown: Total listă | În coș | Rămas de luat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Valoare Listă", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format(Locale.US, "%.2f", summary.estimatedTotal)} $currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${summary.totalItemsCount} articole", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("În Coș Acum", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format(Locale.US, "%.2f", summary.inCartTotal)} $currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = SuccessGreenLight)
                                Text("${summary.inCartItemsCount} articole", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Rămas de Luat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${String.format(Locale.US, "%.2f", summary.remainingToBuyEstimated)} $currency", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("${summary.toBuyItemsCount} articole", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        // Discounts and fees button
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = { showDiscountDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Discount, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (summary.discountAmount > 0 || summary.extraFeesAmount > 0)
                                    "Reduceri: -${String.format(Locale.US, "%.2f", summary.discountAmount)} | Costuri: +${String.format(Locale.US, "%.2f", summary.extraFeesAmount)}"
                                else
                                    "Adaugă Reduceri / Garanție SGR / Sacoșe",
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // 3. Products list with shelf-price entry
            item {
                Text(
                    text = "Introdu prețurile văzute la raft:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            items(state.items, key = { it.id }) { item ->
                PrePaymentItemRow(
                    item = item,
                    currency = currency,
                    onUpdatePrice = { newPrice ->
                        viewModel.updateItemPrice(item.id, newPrice)
                    },
                    onUpdateQuantity = { newQty ->
                        viewModel.updateItemQuantity(item.id, newQty)
                    },
                    onToggleStage = {
                        val nextStage = when (item.purchaseStage) {
                            ItemPurchaseStage.TO_BUY -> ItemPurchaseStage.IN_CART
                            ItemPurchaseStage.IN_CART -> ItemPurchaseStage.PURCHASED
                            ItemPurchaseStage.PURCHASED -> ItemPurchaseStage.TO_BUY
                        }
                        viewModel.updateItemPurchaseStage(item.id, nextStage)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Dialog: Budget Setter
    if (showBudgetDialog) {
        var tempBudget by remember { mutableStateOf(state.list?.budget?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showBudgetDialog = false },
            title = { Text("Setează Limită de Buget") },
            text = {
                Column {
                    Text("Introdu suma maximă pe care dorești să o cheltuiești în această sesiune:")
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = tempBudget,
                        onValueChange = { tempBudget = it },
                        label = { Text("Buget ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateListBudget(tempBudget.replace(',', '.').toDoubleOrNull())
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

    // Dialog: Discounts & Extra Fees
    if (showDiscountDialog) {
        var tempDiscount by remember { mutableStateOf(state.list?.discountAmount?.toString() ?: "") }
        var tempFees by remember { mutableStateOf(state.list?.extraFeesAmount?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showDiscountDialog = false },
            title = { Text("Reduceri & Costuri Extra") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempDiscount,
                        onValueChange = { tempDiscount = it },
                        label = { Text("Reduceri / Cupoane ($currency)") },
                        placeholder = { Text("ex: 10.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempFees,
                        onValueChange = { tempFees = it },
                        label = { Text("Costuri Extra: Garanție SGR / Sacoșe ($currency)") },
                        placeholder = { Text("ex: 2.50") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateListDiscountsAndFees(
                        discount = tempDiscount.replace(',', '.').toDoubleOrNull(),
                        extraFees = tempFees.replace(',', '.').toDoubleOrNull()
                    )
                    showDiscountDialog = false
                }) {
                    Text("Aplică")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscountDialog = false }) {
                    Text("Anulează")
                }
            }
        )
    }

    // Dialog: Receipt Reconciliation & Comparison
    if (showReceiptDialog) {
        var receiptStr by remember { mutableStateOf(state.list?.receiptTotal?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { showReceiptDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Verificare Bon Fiscal")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Introdu suma de pe bonul primit de la casă pentru a o compara cu calculul din coș:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = receiptStr,
                        onValueChange = { receiptStr = it },
                        label = { Text("Total Bon Fiscal ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val enteredReceipt = receiptStr.replace(',', '.').toDoubleOrNull()
                    if (enteredReceipt != null) {
                        val estimatedRef = if (summary.inCartTotal > 0.0) summary.inCartTotal else summary.estimatedTotal
                        val diff = enteredReceipt - estimatedRef

                        Spacer(modifier = Modifier.height(4.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Estimare în Coș: ${String.format(Locale.US, "%.2f", estimatedRef)} $currency", style = MaterialTheme.typography.bodySmall)
                                Text("Total Bon Real: ${String.format(Locale.US, "%.2f", enteredReceipt)} $currency", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (diff < 0) {
                                    Text(
                                        text = "Economie realizată: ${String.format(Locale.US, "%.2f", -diff)} $currency! 🎉",
                                        color = SuccessGreenLight,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else if (diff > 0) {
                                    Text(
                                        text = "Diferență față de calcul: +${String.format(Locale.US, "%.2f", diff)} $currency",
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    Text("Potrivire exactă 100%! 👍", color = SuccessGreenLight, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val rTotal = receiptStr.replace(',', '.').toDoubleOrNull()
                    viewModel.updateListReceiptTotal(rTotal)
                    showReceiptDialog = false
                    scope.launch {
                        snackbarHostState.showSnackbar("Totalul de pe bon a fost salvat cu succes!")
                    }
                }) {
                    Text("Salvează Bonul")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReceiptDialog = false }) {
                    Text("Închide")
                }
            }
        )
    }
}

@Composable
fun PrePaymentItemRow(
    item: ShoppingListItem,
    currency: String,
    onUpdatePrice: (Double?) -> Unit,
    onUpdateQuantity: (Double) -> Unit,
    onToggleStage: () -> Unit
) {
    var priceText by remember(item.estimatedUnitPrice) {
        mutableStateOf(item.estimatedUnitPrice?.toString() ?: "")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (item.purchaseStage) {
                ItemPurchaseStage.PURCHASED -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                ItemPurchaseStage.IN_CART -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                ItemPurchaseStage.TO_BUY -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 3-Stage Quick Selector Icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when (item.purchaseStage) {
                                ItemPurchaseStage.PURCHASED -> SuccessGreenLight
                                ItemPurchaseStage.IN_CART -> MaterialTheme.colorScheme.primary
                                ItemPurchaseStage.TO_BUY -> MaterialTheme.colorScheme.outlineVariant
                            }
                        )
                        .clickable { onToggleStage() },
                    contentAlignment = Alignment.Center
                ) {
                    when (item.purchaseStage) {
                        ItemPurchaseStage.PURCHASED -> Icon(Icons.Default.Check, contentDescription = "Plătit", tint = Color.White, modifier = Modifier.size(20.dp))
                        ItemPurchaseStage.IN_CART -> Icon(Icons.Default.ShoppingCart, contentDescription = "În coș", tint = Color.White, modifier = Modifier.size(20.dp))
                        ItemPurchaseStage.TO_BUY -> Text("+", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.productNameSnapshot,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (item.isPurchased) TextDecoration.LineThrough else TextDecoration.None
                    )
                    Text(
                        text = "Stare: ${item.purchaseStage.labelRo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (item.purchaseStage) {
                            ItemPurchaseStage.PURCHASED -> SuccessGreenLight
                            ItemPurchaseStage.IN_CART -> MaterialTheme.colorScheme.primary
                            ItemPurchaseStage.TO_BUY -> MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Line Total
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%.2f", item.calculatedLineTotal)} $currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (item.isInCart || item.isPurchased) SuccessGreenLight else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Stepper and Shelf-Price Entry
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Stepper (-)
                IconButton(
                    onClick = {
                        val step = if (item.unit == UnitOfMeasure.KG || item.unit == UnitOfMeasure.L) 0.5 else 1.0
                        val newQty = (item.quantity - step).coerceAtLeast(0.1)
                        onUpdateQuantity(newQty)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Scade", modifier = Modifier.size(16.dp))
                }

                Text(
                    text = "${if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity} ${item.unit.getSymbol(true)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                // Stepper (+)
                IconButton(
                    onClick = {
                        val step = if (item.unit == UnitOfMeasure.KG || item.unit == UnitOfMeasure.L) 0.5 else 1.0
                        onUpdateQuantity(item.quantity + step)
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crește", modifier = Modifier.size(16.dp))
                }

                // Shelf Price Text Field
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { str ->
                        priceText = str
                        val p = str.replace(',', '.').toDoubleOrNull()
                        onUpdatePrice(p)
                    },
                    label = { Text("Preț la raft ($currency)") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
