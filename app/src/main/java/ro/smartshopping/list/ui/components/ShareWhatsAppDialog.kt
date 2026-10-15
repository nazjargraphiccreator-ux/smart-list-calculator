package ro.smartshopping.list.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ro.smartshopping.list.data.session.SavedWhatsAppContact
import ro.smartshopping.list.data.session.SavedWhatsAppContactsManager
import ro.smartshopping.list.domain.model.ShoppingList
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.util.WhatsAppShareHelper

private val WhatsAppBrandColor = Color(0xFF25D366)
private val WhatsAppDarkGreen = Color(0xFF0F2618)
private val WhatsAppBubbleColor = Color(0xFF1B382B)

@Composable
fun ShareWhatsAppDialog(
    list: ShoppingList,
    items: List<ShoppingListItem>,
    onDismissRequest: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Contacte salvate & ultimul număr
    var savedContacts by remember {
        mutableStateOf(SavedWhatsAppContactsManager.getSavedContacts(context))
    }
    var phoneNumber by remember {
        mutableStateOf(SavedWhatsAppContactsManager.getLastUsedNumber(context))
    }

    var onlyRemaining by remember { mutableStateOf(true) }
    var showPreview by remember { mutableStateOf(true) }
    var saveContactLabel by remember { mutableStateOf("") }
    var showSaveLabelInput by remember { mutableStateOf(false) }

    // Generare text WhatsApp cu iconițe
    val messageText = remember(list, items, onlyRemaining) {
        WhatsAppShareHelper.formatRemainingListWithIcons(
            list = list,
            items = items,
            onlyRemaining = onlyRemaining
        )
    }

    val remainingItemsCount = remember(items) {
        items.count { !it.isPurchased && it.purchaseStage != ro.smartshopping.list.domain.model.ItemPurchaseStage.PURCHASED }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 720.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Antet cu Branding WhatsApp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(WhatsAppBrandColor.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("💬", fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Trimite pe WhatsApp",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ce a rămas de cumpărat • Cu iconițe",
                                style = MaterialTheme.typography.bodySmall,
                                color = WhatsAppBrandColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Default.Close, contentDescription = "Închide")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Selector: Doar ce a rămas vs Toată lista
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = onlyRemaining,
                        onClick = { onlyRemaining = true },
                        label = { Text("Rămase ($remainingItemsCount produse)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = WhatsAppBrandColor.copy(alpha = 0.2f),
                            selectedLabelColor = WhatsAppBrandColor
                        )
                    )
                    FilterChip(
                        selected = !onlyRemaining,
                        onClick = { onlyRemaining = false },
                        label = { Text("Toată lista (${items.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Câmp Număr Telefon
                Text(
                    text = "Număr de telefon destinatar:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ex: 0722 123 456 sau +407...") },
                    leadingIcon = {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = WhatsAppBrandColor)
                    },
                    trailingIcon = {
                        if (phoneNumber.isNotBlank()) {
                            IconButton(onClick = { phoneNumber = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Șterge număr")
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (phoneNumber.isNotBlank()) {
                                SavedWhatsAppContactsManager.setLastUsedNumber(context, phoneNumber)
                            }
                        }
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = WhatsAppBrandColor,
                        focusedLabelColor = WhatsAppBrandColor
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Preset Chips pentru contacte frecvente (Soție, Acasă, etc.)
                Text(
                    text = "Alege rapid destinatar:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(savedContacts) { contact ->
                        val isContactSaved = contact.phoneNumber.isNotBlank()
                        val isSelected = isContactSaved && WhatsAppShareHelper.cleanPhoneNumber(phoneNumber) == WhatsAppShareHelper.cleanPhoneNumber(contact.phoneNumber)

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) WhatsAppBrandColor else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    if (isContactSaved) {
                                        phoneNumber = contact.phoneNumber
                                    } else {
                                        // Cere salvarea numărului curent sub această etichetă
                                        if (phoneNumber.isNotBlank()) {
                                            SavedWhatsAppContactsManager.saveContact(
                                                context,
                                                contact.copy(phoneNumber = phoneNumber)
                                            )
                                            savedContacts = SavedWhatsAppContactsManager.getSavedContacts(context)
                                            Toast.makeText(context, "Număr salvat pentru: ${contact.label}", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Scrie mai întâi un număr în câmp!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            color = if (isSelected) WhatsAppBrandColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(contact.emoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = contact.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) WhatsAppBrandColor else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isContactSaved) {
                                        Text(
                                            text = contact.phoneNumber.takeLast(6).padStart(contact.phoneNumber.length, '•'),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "+ Adaugă nr",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Opțiune de a salva numărul curent cu un nume nou
                AnimatedVisibility(visible = showSaveLabelInput) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = saveContactLabel,
                                onValueChange = { saveContactLabel = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Nume contact (ex: Mama)") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (saveContactLabel.isNotBlank() && phoneNumber.isNotBlank()) {
                                        SavedWhatsAppContactsManager.saveContact(
                                            context,
                                            SavedWhatsAppContact(
                                                label = saveContactLabel.trim(),
                                                phoneNumber = phoneNumber.trim(),
                                                emoji = "👤"
                                            )
                                        )
                                        savedContacts = SavedWhatsAppContactsManager.getSavedContacts(context)
                                        showSaveLabelInput = false
                                        saveContactLabel = ""
                                        Toast.makeText(context, "Contact salvat!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Salvează")
                            }
                        }
                    }
                }

                if (!showSaveLabelInput && phoneNumber.isNotBlank()) {
                    TextButton(
                        onClick = { showSaveLabelInput = true },
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(Icons.Default.BookmarkBorder, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salvează acest număr în lista rapidă", fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Card Previzualizare Mesaj Formatat cu Iconițe
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPreview = !showPreview },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Previzualizare Mesaj WhatsApp:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = if (showPreview) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand/Collapse"
                    )
                }

                AnimatedVisibility(visible = showPreview) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = WhatsAppDarkGreen),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, WhatsAppBrandColor.copy(alpha = 0.4f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MESAJ WHATSAPP (CU ICONIȚE)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = WhatsAppBrandColor
                                )
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(messageText))
                                        Toast.makeText(context, "Mesajul cu iconițe a fost copiat!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copiază",
                                        tint = WhatsAppBrandColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = messageText,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(16.dp))

                // Buton Principal: Trimite pe WhatsApp la număr
                val hasValidNumber = WhatsAppShareHelper.isValidPhoneNumber(phoneNumber)
                Button(
                    onClick = {
                        if (phoneNumber.isNotBlank()) {
                            SavedWhatsAppContactsManager.setLastUsedNumber(context, phoneNumber)
                        }
                        WhatsAppShareHelper.sendWhatsAppDirect(
                            context = context,
                            phoneNumber = phoneNumber.takeIf { it.isNotBlank() },
                            messageText = messageText
                        )
                        onDismissRequest()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = WhatsAppBrandColor)
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = null,
                        tint = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (hasValidNumber) "Trimite pe WhatsApp la ${WhatsAppShareHelper.cleanPhoneNumber(phoneNumber)}" else "Trimite pe WhatsApp",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Buton Secundar: Deschide în WhatsApp fără număr (alege contact din WhatsApp)
                OutlinedButton(
                    onClick = {
                        WhatsAppShareHelper.sendWhatsAppDirect(
                            context = context,
                            phoneNumber = null,
                            messageText = messageText
                        )
                        onDismissRequest()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aleg contactul direct din WhatsApp", fontSize = 13.sp)
                }
            }
        }
    }
}
