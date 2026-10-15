package ro.smartshopping.list.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import ro.smartshopping.list.data.repository.ProductRepository
import ro.smartshopping.list.domain.model.BarcodeType
import ro.smartshopping.list.domain.model.ItemPurchaseStage
import ro.smartshopping.list.domain.model.Product
import ro.smartshopping.list.domain.model.ShoppingListItem
import ro.smartshopping.list.domain.model.UnitOfMeasure
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductBarcodeScannerDialog(
    itemsInList: List<ShoppingListItem>,
    currency: String,
    productRepository: ProductRepository,
    onPutInCart: (itemId: String, price: Double?) -> Unit,
    onAddNewProductToCart: (name: String, quantity: Double, unit: UnitOfMeasure, price: Double?, barcode: String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var scannedCode by remember { mutableStateOf<String?>(null) }
    var scannedType by remember { mutableStateOf<BarcodeType?>(null) }
    var matchedItem by remember { mutableStateOf<ShoppingListItem?>(null) }
    var matchedCatalogProduct by remember { mutableStateOf<Product?>(null) }
    var isSearching by remember { mutableStateOf(false) }

    // Input fields for scanned product
    var productNameInput by remember { mutableStateOf("") }
    var productPriceInput by remember { mutableStateOf("") }
    var productQuantityInput by remember { mutableStateOf("1") }
    var actionSuccessMessage by remember { mutableStateOf<String?>(null) }

    fun vibrateSuccess() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val mgr = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                mgr?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vib = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vib?.vibrate(120)
            }
        } catch (e: Exception) {
            // Ignore on devices without vibrator
        }
    }

    fun handleBarcodeDetected(code: String, type: BarcodeType) {
        if (scannedCode != null) return // Already processing
        scannedCode = code
        scannedType = type
        isSearching = true
        vibrateSuccess()

        scope.launch {
            // 1. Search in existing product catalog
            val catalogProd = productRepository.findProductByBarcode(code)
            matchedCatalogProduct = catalogProd

            // 2. Check if an item in the active shopping list matches
            val itemInList = itemsInList.find { item ->
                if (catalogProd != null && item.productId == catalogProd.id) true
                else if (catalogProd != null && item.productNameSnapshot.equals(catalogProd.name, ignoreCase = true)) true
                else item.notes?.contains(code) == true
            }

            matchedItem = itemInList

            if (itemInList != null) {
                productNameInput = itemInList.productNameSnapshot
                productQuantityInput = if (itemInList.quantity % 1.0 == 0.0) itemInList.quantity.toInt().toString() else itemInList.quantity.toString()
                productPriceInput = (itemInList.actualUnitPrice ?: itemInList.estimatedUnitPrice)?.let {
                    String.format(Locale.US, "%.2f", it)
                } ?: (catalogProd?.lastKnownPrice?.let { String.format(Locale.US, "%.2f", it) } ?: "")
            } else if (catalogProd != null) {
                productNameInput = catalogProd.name
                productQuantityInput = "1"
                productPriceInput = catalogProd.lastKnownPrice?.let { String.format(Locale.US, "%.2f", it) } ?: ""
            } else {
                productNameInput = ""
                productQuantityInput = "1"
                productPriceInput = ""
            }

            isSearching = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            // Camera scanner view
            BarcodeCameraScanner(
                onBarcodeDetected = { code, type ->
                    handleBarcodeDetected(code, type)
                },
                onClose = onDismiss
            )

            // Header instructions over camera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color(0xFF00FF88),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Scanează Cod de Bare la Raft",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Închide", tint = Color.White)
                }
            }

            // Bottom Product Action Card (pops up when barcode is detected)
            AnimatedVisibility(
                visible = scannedCode != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        if (isSearching) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Identificare produs...", style = MaterialTheme.typography.bodyMedium)
                            }
                        } else if (actionSuccessMessage != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = SuccessGreenLight,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = actionSuccessMessage ?: "",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SuccessGreenLight
                                )
                                Spacer(modifier = Modifier.height(14.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    OutlinedButton(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Finalizat")
                                    }
                                    Button(
                                        onClick = {
                                            // Reset to scan next item
                                            scannedCode = null
                                            scannedType = null
                                            matchedItem = null
                                            matchedCatalogProduct = null
                                            actionSuccessMessage = null
                                            productNameInput = ""
                                            productPriceInput = ""
                                            productQuantityInput = "1"
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FF88))
                                    ) {
                                        Icon(Icons.Default.Replay, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Scanează Următorul", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            // Product details & confirmation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = if (matchedItem != null) "Găsit în Lista Ta!"
                                        else if (matchedCatalogProduct != null) "Identificat în Catalog"
                                        else "Produs Nou Scanat",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (matchedItem != null) SuccessGreenLight else MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Cod: ${scannedCode ?: ""}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        // Reset to rescan
                                        scannedCode = null
                                    }
                                ) {
                                    Icon(Icons.Default.Replay, contentDescription = "Rescanează")
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (matchedItem == null) {
                                OutlinedTextField(
                                    value = productNameInput,
                                    onValueChange = { productNameInput = it },
                                    label = { Text("Nume Produs") },
                                    placeholder = { Text("ex: Lapte Zuzu 1.5L") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                Text(
                                    text = matchedItem?.productNameSnapshot ?: "",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = productPriceInput,
                                    onValueChange = { productPriceInput = it.replace(',', '.') },
                                    label = { Text("Preț Raft ($currency)") },
                                    placeholder = { Text("ex: 6.99") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                OutlinedTextField(
                                    value = productQuantityInput,
                                    onValueChange = { productQuantityInput = it.replace(',', '.') },
                                    label = { Text("Cantitate") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    singleLine = true,
                                    modifier = Modifier.weight(0.7f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Button(
                                onClick = {
                                    val price = productPriceInput.toDoubleOrNull()
                                    val qty = productQuantityInput.toDoubleOrNull() ?: 1.0

                                    if (matchedItem != null) {
                                        onPutInCart(matchedItem!!.id, price)
                                        actionSuccessMessage = "Adăugat în coș: ${matchedItem!!.productNameSnapshot}"
                                    } else {
                                        val name = productNameInput.ifBlank { "Produs Scanat ($scannedCode)" }
                                        onAddNewProductToCart(
                                            name,
                                            qty,
                                            matchedCatalogProduct?.defaultUnit ?: UnitOfMeasure.PCS,
                                            price,
                                            scannedCode ?: ""
                                        )
                                        actionSuccessMessage = "Adăugat direct în coș: $name"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF00FF88)
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddShoppingCart,
                                    contentDescription = null,
                                    tint = Color.Black,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (matchedItem != null) "Confirmă & Pune în Coș" else "Adaugă Direct în Coș",
                                    color = Color.Black,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
