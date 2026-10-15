package ro.smartshopping.list.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.domain.parser.ParsedShoppingItem
import ro.smartshopping.list.domain.parser.SmartTextImportParser
import ro.smartshopping.list.domain.parser.TextImportResult
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextImportDialog(
    parser: SmartTextImportParser,
    onDismiss: () -> Unit,
    onConfirmImport: (List<ParsedShoppingItem>) -> Unit,
    onConfirmMultiStoreImport: ((itemsByStore: Map<String, List<ParsedShoppingItem>>, baseTitle: String) -> Unit)? = null
) {
    val clipboardManager = LocalClipboardManager.current

    val sampleMultiStoreText = """
Lista cumpărături 

Piață : 

1. Cartofi 10 kg
2. Fasole 2kg
3. Branza capra 


Metro:

1. Săpun - 6 buc
2. Bulion sultan - 4 buc
3. Samntana ( galetusa ) - 1 buc 
4. Salam săsesc- 1 buc 
5. Orez - 1 buc  
6. Scărița afumata 3kg 


Auchan : 

1. Cofrag - 30 oua 


Kaufland:

1. File de pui cu susan - 6 
2. Lapte dulce - 2 
3. Cascaval 
4. Iaurt cremosso - 4 buc ( profi )
    """.trimIndent()

    var rawText by remember { mutableStateOf("") }
    var parseResult by remember { mutableStateOf<TextImportResult?>(null) }
    var parsedItems by remember { mutableStateOf<List<ParsedShoppingItem>>(emptyList()) }
    var hasAnalyzed by remember { mutableStateOf(false) }
    var selectedStoreFilter by remember { mutableStateOf<String?>(null) }

    fun runAnalysis(textToParse: String) {
        val result = parser.parse(textToParse)
        parseResult = result
        parsedItems = result.items
        hasAnalyzed = true
        selectedStoreFilter = null
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.94f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxHeight()
            ) {
                // Header 2026
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Import Inteligent Multi-Magazin 2026",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Piață, Metro, Auchan, Kaufland • Auto-spațiere & note",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Închide")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (!hasAnalyzed || parsedItems.isEmpty()) {
                    // Input View
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            placeholder = {
                                Text(
                                    "Lipește aici textul cu magazine și produse:\n\nExemplu:\nPiață :\n1. Cartofi 10 kg\n2. Fasole 2kg\n\nMetro:\n1. Săpun - 6 buc\n2. Samntana ( galetusa ) - 1 buc\n\nKaufland:\n1. File de pui cu susan - 6\n2. Lapte dulce - 2"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text
                                    if (!clip.isNullOrBlank()) {
                                        rawText = clip
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Lipește Clipboard", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    rawText = sampleMultiStoreText
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Exemplu 4 Magazine", fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { runAnalysis(rawText) },
                        enabled = rawText.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analizează și Identifică Magazinele", fontWeight = FontWeight.Bold)
                    }
                } else {
                    // Preview & Edit View
                    val currentResult = parseResult
                    val stores = currentResult?.storesDetected ?: emptyList()
                    val spaceCorrectionsCount = parsedItems.count { it.spaceCorrectionNote != null }
                    val duplicatesCount = parsedItems.groupBy { "${it.storeName ?: ""}_${it.name.lowercase().trim()}" }.count { it.value.size > 1 }
                    val totalEstimated = parsedItems.sumOf { it.estimatedLineTotal }
                    val suggestedTitle = currentResult?.suggestedTitle ?: "Listă Cumpărături"

                    // Multi-Store Filter Chips
                    if (stores.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = selectedStoreFilter == null,
                                onClick = { selectedStoreFilter = null },
                                label = { Text("Toate (${parsedItems.size})", fontWeight = FontWeight.Bold) }
                            )
                            stores.forEach { store ->
                                val count = parsedItems.count { it.storeName == store }
                                FilterChip(
                                    selected = selectedStoreFilter == store,
                                    onClick = { selectedStoreFilter = if (selectedStoreFilter == store) null else store },
                                    label = { Text("$store ($count)") },
                                    leadingIcon = {
                                        Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                )
                            }
                        }
                    }

                    // Space correction intelligence banner
                    if (spaceCorrectionsCount > 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "⚡ $spaceCorrectionsCount corecții automate de spații aplicate (ex: 2kg ➔ 2 kg, 3kg ➔ 3 kg, săsesc- 1 ➔ săsesc - 1)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // Duplicate warning banner if any
                    if (duplicatesCount > 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$duplicatesCount produse duplicate detectate.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Button(
                                    onClick = {
                                        parsedItems = parser.mergeDuplicates(parsedItems)
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                                ) {
                                    Icon(Icons.Default.Merge, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Combină", fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val visibleItems = if (selectedStoreFilter == null) parsedItems else parsedItems.filter { it.storeName == selectedStoreFilter }
                        Text(
                            text = "${visibleItems.size} produse identificate (editabile)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { hasAnalyzed = false }) {
                            Text("Modifică textul sursă", fontSize = 12.sp)
                        }
                    }

                    // Editable Items List
                    val visibleItemsWithIndices = parsedItems.mapIndexed { index, item -> index to item }
                        .filter { selectedStoreFilter == null || it.second.storeName == selectedStoreFilter }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(visibleItemsWithIndices, key = { _, pair -> pair.second.id }) { _, (originalIndex, item) ->
                            EditableParsedItemRow(
                                item = item,
                                onUpdate = { updated ->
                                    val newList = parsedItems.toMutableList()
                                    newList[originalIndex] = updated
                                    parsedItems = newList
                                },
                                onDelete = {
                                    val newList = parsedItems.toMutableList()
                                    newList.removeAt(originalIndex)
                                    parsedItems = newList
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(8.dp))

                    // Summary & Multi-store Import Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Estimativ Previzualizat",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format(Locale.US, "%.2f", totalEstimated)} RON",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreenLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons
                    val itemsByStore = parsedItems.groupBy { it.storeName ?: "General" }
                    if (stores.size > 1 && onConfirmMultiStoreImport != null) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Button(
                                onClick = { onConfirmMultiStoreImport(itemsByStore, suggestedTitle) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                            ) {
                                Icon(Icons.Default.Storefront, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Creează ${stores.size} Liste Separate pe Magazine", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { onConfirmImport(parsedItems) },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("1 Singură Listă Grupată (${parsedItems.size} produse)", fontSize = 13.sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = { onConfirmImport(parsedItems) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Icon(Icons.Default.ShoppingBag, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirmă și Importă (${parsedItems.size} produse)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditableParsedItemRow(
    item: ParsedShoppingItem,
    onUpdate: (ParsedShoppingItem) -> Unit,
    onDelete: () -> Unit
) {
    var isExpandedUnitMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isDuplicate)
                MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Row 1: Store & Space correction Badges (if present)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!item.storeName.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "🏪 ${item.storeName}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (!item.spaceCorrectionNote.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "⚡ ${item.spaceCorrectionNote}",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Șterge",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Row 2: Name Input
            OutlinedTextField(
                value = item.name,
                onValueChange = { newName ->
                    onUpdate(item.copy(name = newName))
                },
                label = { Text("Denumire produs") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Optional Row: Notes (if present in original text like "( galetusa )" or "( profi )")
            if (!item.notes.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = item.notes,
                    onValueChange = { newNote ->
                        onUpdate(item.copy(notes = newNote.ifBlank { null }))
                    },
                    label = { Text("Notă / Ambalaj special") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 3: Quantity Stepper, Unit Dropdown, Unit Price, Line Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Stepper (-)
                IconButton(
                    onClick = {
                        val step = if (item.unit == UnitOfMeasure.KG || item.unit == UnitOfMeasure.L) 0.5 else 1.0
                        val newQty = (item.quantity - step).coerceAtLeast(0.1)
                        val newTotal = (item.unitPrice ?: 0.0) * newQty
                        onUpdate(item.copy(quantity = newQty, estimatedLineTotal = newTotal))
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Scade", modifier = Modifier.size(16.dp))
                }

                // Quantity Text
                OutlinedTextField(
                    value = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString(),
                    onValueChange = { str ->
                        val qty = str.replace(',', '.').toDoubleOrNull() ?: 1.0
                        val newTotal = (item.unitPrice ?: 0.0) * qty
                        onUpdate(item.copy(quantity = qty, estimatedLineTotal = newTotal))
                    },
                    label = { Text("Cant.") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(68.dp)
                )

                // Stepper (+)
                IconButton(
                    onClick = {
                        val step = if (item.unit == UnitOfMeasure.KG || item.unit == UnitOfMeasure.L) 0.5 else 1.0
                        val newQty = item.quantity + step
                        val newTotal = (item.unitPrice ?: 0.0) * newQty
                        onUpdate(item.copy(quantity = newQty, estimatedLineTotal = newTotal))
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Crește", modifier = Modifier.size(16.dp))
                }

                // Unit Selector Dropdown
                Box {
                    OutlinedButton(
                        onClick = { isExpandedUnitMenu = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Text(item.unit.getSymbol(true), fontWeight = FontWeight.Bold)
                    }

                    DropdownMenu(
                        expanded = isExpandedUnitMenu,
                        onDismissRequest = { isExpandedUnitMenu = false }
                    ) {
                        UnitOfMeasure.entries.forEach { u ->
                            DropdownMenuItem(
                                text = { Text("${u.symbolRo} (${u.name})") },
                                onClick = {
                                    onUpdate(item.copy(unit = u))
                                    isExpandedUnitMenu = false
                                }
                            )
                        }
                    }
                }

                // Price Input
                OutlinedTextField(
                    value = item.unitPrice?.toString() ?: "",
                    onValueChange = { priceStr ->
                        val p = priceStr.replace(',', '.').toDoubleOrNull()
                        val newTotal = (p ?: 0.0) * item.quantity
                        onUpdate(item.copy(unitPrice = p, estimatedLineTotal = newTotal))
                    },
                    placeholder = { Text("Preț") },
                    label = { Text("Preț (RON)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
            }

            // Row 4: Category badge & total calculated
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Categorie: ${item.category}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                    if (item.hasCorrection) {
                        Text(
                            text = " • Corectat automat",
                            style = MaterialTheme.typography.bodySmall,
                            color = SuccessGreenLight,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (item.estimatedLineTotal > 0.0) {
                    Text(
                        text = "Total: ${String.format(Locale.US, "%.2f", item.estimatedLineTotal)} RON",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
