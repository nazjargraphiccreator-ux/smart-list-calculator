package ro.smartshopping.list.ui.screens.wallet

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ro.smartshopping.list.domain.model.BarcodeType
import ro.smartshopping.list.domain.model.LoyaltyCard
import ro.smartshopping.list.ui.components.BarcodeCameraScanner
import ro.smartshopping.list.util.BarcodeHelper

data class StorePreset(
    val storeName: String,
    val programName: String,
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val defaultType: BarcodeType
)

val ROMANIAN_STORE_PRESETS = listOf(
    StorePreset("Kaufland Militari", "Kaufland Card", "#E11A22", "#7A0006", BarcodeType.CODE_128),
    StorePreset("Auchan Militari", "MyClub Auchan", "#D61821", "#0E7535", BarcodeType.CODE_128),
    StorePreset("Lidl", "Lidl Plus", "#0050AA", "#FFCC00", BarcodeType.QR_CODE),
    StorePreset("Carrefour", "Act For Good", "#004E98", "#E63946", BarcodeType.CODE_128),
    StorePreset("Mega Image", "Connect Card", "#E2001A", "#1A3B8B", BarcodeType.CODE_128),
    StorePreset("Penny", "Penny Card", "#E30613", "#FFCC00", BarcodeType.CODE_128),
    StorePreset("Catena", "Card Catena", "#008000", "#004D00", BarcodeType.CODE_128),
    StorePreset("Dr. Max", "Card Fidelitate", "#009640", "#E84E1B", BarcodeType.CODE_128)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoyaltyWalletScreen(
    viewModel: LoyaltyWalletViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayedCards = if (uiState.showOnlyFavorites) uiState.favoriteCards else uiState.cards

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Portofel Carduri Magazin",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Securizat",
                                tint = Color(0xFF00FF88),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "Carduri de fidelitate & scanare instantanee POS",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.openCameraScanner() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scanează Card Fizic",
                            tint = Color(0xFF00E5FF)
                        )
                    }
                    IconButton(
                        onClick = { viewModel.toggleFavoritesFilter() }
                    ) {
                        Icon(
                            imageVector = if (uiState.showOnlyFavorites) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Doar Favorite",
                            tint = if (uiState.showOnlyFavorites) Color(0xFFFFD600) else MaterialTheme.colorScheme.onSurface
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
                onClick = { viewModel.openAddDialog() },
                containerColor = Color(0xFF00E5FF),
                contentColor = Color.Black
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Adaugă Card")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Card Nou", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (displayedCards.isEmpty()) {
                EmptyWalletPlaceholder(
                    onAddClick = { viewModel.openAddDialog() },
                    onScanClick = { viewModel.openCameraScanner() }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Banner Securitate & Sfat la Casierie
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = Color(0xFF00E5FF),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Apasă pe orice card pentru codul de scanat",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Ecranul se va deschide mărit cu luminozitate optimizată pentru POS Kaufland / Auchan.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }

                    items(displayedCards, key = { it.id }) { card ->
                        LoyaltyCardItem(
                            card = card,
                            onCardClick = { viewModel.openCardDisplay(card) },
                            onToggleFavorite = { viewModel.toggleFavorite(card) },
                            onDelete = { viewModel.deleteCard(card.id) }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(72.dp))
                    }
                }
            }
        }
    }

    // Modal Afișare Cod de Bare Mărit pentru Casa de Marcat (POS Cashier Scanner Mode)
    uiState.selectedCardForDisplay?.let { card ->
        FullScreenBarcodeDialog(
            card = card,
            onClose = { viewModel.closeCardDisplay() }
        )
    }

    // Modal Adăugare Card Nou
    if (uiState.showAddDialog) {
        AddLoyaltyCardDialog(
            onDismiss = { viewModel.closeAddDialog() },
            onCardCreated = { store, prog, num, type, c1, c2, holder, notes, fav ->
                viewModel.addCard(store, prog, num, type, c1, c2, holder, notes, fav)
            },
            onScanRequested = {
                viewModel.openCameraScanner()
            }
        )
    }

    // Scaner Cameră Integrat
    if (uiState.showCameraScanner) {
        Dialog(
            onDismissRequest = { viewModel.closeCameraScanner() },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BarcodeCameraScanner(
                onBarcodeDetected = { code, type ->
                    viewModel.closeCameraScanner()
                    // Detectăm presetul de magazin pe baza formatului sau deschidem dialogul pre-completat
                    viewModel.openAddDialog()
                },
                onClose = { viewModel.closeCameraScanner() }
            )
        }
    }
}

@Composable
fun LoyaltyCardItem(
    card: LoyaltyCard,
    onCardClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryColor = try {
        Color(android.graphics.Color.parseColor(card.primaryColorHex))
    } catch (e: Exception) {
        Color(0xFFE11A22)
    }

    val secondaryColor = try {
        Color(android.graphics.Color.parseColor(card.secondaryColorHex))
    } catch (e: Exception) {
        Color(0xFF1E293B)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.586f) // Standard Credit Card Aspect Ratio
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onCardClick() },
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    )
                )
                .padding(20.dp)
        ) {
            // Chip auriu realist & Antet magazin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = card.storeName.uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = card.programName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (card.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Favorit",
                            tint = if (card.isFavorite) Color(0xFFFFD600) else Color.White.copy(alpha = 0.7f)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Șterge",
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // Chip SIM Simulat & Simbol Contactless
            Row(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Chip auriu
                Box(
                    modifier = Modifier
                        .size(42.dp, 32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFDF7A), Color(0xFFD4AF37), Color(0xFFAA8010))
                            )
                        )
                )

                Spacer(modifier = Modifier.width(16.dp))

                Icon(
                    imageVector = Icons.Default.QrCode,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(28.dp)
                )
            }

            // Număr card & Nume Posesor Jos
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = card.maskedNumber,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.cardHolderName ?: "CLIENT FIDEL",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "APASĂ PT SCANARE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FF88)
                    )
                }
            }
        }
    }
}

@Composable
fun FullScreenBarcodeDialog(
    card: LoyaltyCard,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val barcodeBitmap = remember(card.cardNumber, card.barcodeType) {
        BarcodeHelper.generateBarcodeBitmap(
            content = card.cardNumber,
            barcodeType = card.barcodeType,
            width = if (card.barcodeType == BarcodeType.QR_CODE) 600 else 900,
            height = 340
        )
    }

    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Antet
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = card.storeName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Text(
                            text = card.programName,
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF64748B)
                        )
                    }

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier
                            .background(Color(0xFFF1F5F9), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Închide",
                            tint = Color.Black
                        )
                    }
                }

                // Zona Centrală Cod de Bare (Pregătită pentru scaner optic)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp)
                ) {
                    if (barcodeBitmap != null) {
                        androidx.compose.foundation.Image(
                            bitmap = barcodeBitmap.asImageBitmap(),
                            contentDescription = "Cod de Bare Card",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (card.barcodeType == BarcodeType.QR_CODE) 260.dp else 180.dp)
                        )
                    } else {
                        Text(
                            text = "Eroare la generarea codului de bare",
                            color = Color.Red
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = card.cardNumber,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.Black,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buton Copiază Cod
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Cod Card", card.cardNumber)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Cod copiat în clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF1F5F9)),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Copiază numărul",
                            color = Color.Black,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Sfat casierie
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💡 Prezintă acest ecran scannerului optic de la casa Kaufland / Auchan sau casierului.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF475569)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddLoyaltyCardDialog(
    onDismiss: () -> Unit,
    onCardCreated: (
        storeName: String,
        programName: String,
        cardNumber: String,
        barcodeType: BarcodeType,
        primaryColorHex: String,
        secondaryColorHex: String,
        cardHolderName: String?,
        notes: String?,
        isFavorite: Boolean
    ) -> Unit,
    onScanRequested: () -> Unit
) {
    var storeName by remember { mutableStateOf("") }
    var programName by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var barcodeType by remember { mutableStateOf(BarcodeType.CODE_128) }
    var primaryColorHex by remember { mutableStateOf("#E11A22") }
    var secondaryColorHex by remember { mutableStateOf("#7A0006") }
    var cardHolderName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var isFavorite by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Adaugă Card Magazin",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Închide")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Presets Rapide
                Text(
                    text = "Alege un magazin rapid:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(ROMANIAN_STORE_PRESETS) { preset ->
                        val isSelected = storeName == preset.storeName
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.clickable {
                                storeName = preset.storeName
                                programName = preset.programName
                                primaryColorHex = preset.primaryColorHex
                                secondaryColorHex = preset.secondaryColorHex
                                barcodeType = preset.defaultType
                            }
                        ) {
                            Text(
                                text = preset.storeName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF00E5FF) else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Câmp Magazin
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    label = { Text("Nume Magazin (ex: Kaufland Militari)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Câmp Program / Tip Card
                OutlinedTextField(
                    value = programName,
                    onValueChange = { programName = it },
                    label = { Text("Program (ex: Kaufland Card)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Câmp Număr Card cu Buton Scanare Cameră
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { cardNumber = it },
                    label = { Text("Număr Card sau Cod Bare") },
                    trailingIcon = {
                        IconButton(onClick = onScanRequested) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Scanează cu Camera",
                                tint = Color(0xFF00E5FF)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Câmp Nume Posesor
                OutlinedTextField(
                    value = cardHolderName,
                    onValueChange = { cardHolderName = it },
                    label = { Text("Posesor Card (Opțional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        if (storeName.isNotBlank() && cardNumber.isNotBlank()) {
                            val prog = if (programName.isBlank()) "Card Fidelitate" else programName
                            onCardCreated(
                                storeName, prog, cardNumber, barcodeType,
                                primaryColorHex, secondaryColorHex, cardHolderName, notes, isFavorite
                            )
                        }
                    },
                    enabled = storeName.isNotBlank() && cardNumber.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Salvează în Portofelul Securizat", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmptyWalletPlaceholder(
    onAddClick: () -> Unit,
    onScanClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(Color(0xFF00E5FF).copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CreditCard,
                contentDescription = null,
                tint = Color(0xFF00E5FF),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Portofelul de Carduri este Gol",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Adaugă sau scanează cardurile magazinelor tale (Kaufland, Auchan, Lidl etc.) pentru a le prezenta direct la casă fără plastic.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
            ) {
                Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Scanează Card", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.outlinedButtonColors()
            ) {
                Text("Adaugă Manual")
            }
        }
    }
}
