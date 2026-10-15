package ro.smartshopping.list.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Merge
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
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
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextImportDialog(
    parser: SmartTextImportParser,
    onDismiss: () -> Unit,
    onConfirmImport: (List<ParsedShoppingItem>) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    val sampleWhatsAppText = """
Lapte 2 bucăți
Pâine 3 buc
Ouă 1 cofraj
Mere 2 kg
Detergent 1 bucată
Apă minerală 6 sticle
    """.trimIndent()

    var rawText by remember { mutableStateOf("") }
    var parsedItems by remember { mutableStateOf<List<ParsedShoppingItem>>(emptyList()) }
    var hasAnalyzed by remember { mutableStateOf(false) }

    // If dialog opens and user clicks Analyze
    fun runAnalysis(textToParse: String) {
        val result = parser.parse(textToParse)
        parsedItems = result.items
        hasAnalyzed = true
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxHeight()
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                                text = "Import Inteligent Text",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "WhatsApp, SMS, Notițe, E-mail",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Închide")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!hasAnalyzed || parsedItems.isEmpty()) {
                    // Input View
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = rawText,
                            onValueChange = { rawText = it },
                            placeholder = {
                                Text(
                                    "Lipește aici textul listei:\n\nExemplu:\nLapte 2 bucăți\nPâine 3 buc\nOuă 1 cofraj\nMere 2 kg\nApă minerală 6 sticle"
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

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
                                Text("Lipește din Clipboard")
                            }

                            OutlinedButton(
                                onClick = {
                                    rawText = sampleWhatsAppText
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Exemplu Rapid")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

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
                        Text("Analizează și Previzualizează")
                    }
                } else {
                    // Editable Preview View
                    val duplicatesCount = parsedItems.groupBy { it.name.lowercase().trim() }.count { it.value.size > 1 }
                    val totalEstimated = parsedItems.sumOf { it.estimatedLineTotal }

                    // Duplicate warning banner if any
                    if (duplicatesCount > 0) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
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
                                        text = "Detectate $duplicatesCount produse duplicate în text.",
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
                                    Icon(Icons.Default.Merge, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Combină", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${parsedItems.size} produse identificate (editabile)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        TextButton(onClick = { hasAnalyzed = false }) {
                            Text("Modifică textul sursă")
                        }
                    }

                    // Editable Items List
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(parsedItems, key = { _, item -> item.id }) { index, item ->
                            EditableParsedItemRow(
                                item = item,
                                onUpdate = { updated ->
                                    val newList = parsedItems.toMutableList()
                                    newList[index] = updated
                                    parsedItems = newList
                                },
                                onDelete = {
                                    val newList = parsedItems.toMutableList()
                                    newList.removeAt(index)
                                    parsedItems = newList
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(10.dp))

                    // Summary Bar
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

                        Button(
                            onClick = { onConfirmImport(parsedItems) },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text("Confirmă și Importă (${parsedItems.size})", fontWeight = FontWeight.Bold)
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
            // Row 1: Name and Category badge and Delete button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = item.name,
                    onValueChange = { newName ->
                        onUpdate(item.copy(name = newName))
                    },
                    label = { Text("Denumire produs") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Șterge",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Row 2: Quantity Stepper, Unit Dropdown, Unit Price, Line Total
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

            // Row 3: Category badge & total calculated
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
