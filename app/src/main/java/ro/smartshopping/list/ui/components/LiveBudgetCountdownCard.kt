package ro.smartshopping.list.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ro.smartshopping.list.domain.model.FinancialSummary
import ro.smartshopping.list.util.StoreNotificationHelper
import java.util.Locale

@Composable
fun LiveBudgetCountdownCard(
    summary: FinancialSummary,
    listName: String,
    onUpdateBudget: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showEditBudgetDialog by remember { mutableStateOf(false) }
    var budgetInput by remember(summary.budget) {
        mutableStateOf(summary.budget?.let { String.format(Locale.US, "%.2f", it) } ?: "")
    }

    val currency = summary.currency
    val budget = summary.budget
    val currentSpending = summary.inCartTotal + summary.actualSpent
    val remainingBudget = if (budget != null) budget - currentSpending else null
    val isOver = remainingBudget != null && remainingBudget < 0.0
    val overAmount = if (isOver) kotlin.math.abs(remainingBudget ?: 0.0) else 0.0

    // Vibrate & Notification trigger when exceeding budget before checkout
    var hasAlertedOverBudget by remember(summary.budget) { mutableStateOf(false) }

    LaunchedEffect(isOver, overAmount) {
        if (isOver && !hasAlertedOverBudget && budget != null) {
            hasAlertedOverBudget = true
            // Haptic vibration
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 400), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(longArrayOf(0, 300, 150, 400), -1)
                }
            } catch (e: Exception) {
                // Ignore vibration errors on emulators
            }

            // High Priority System Notification
            StoreNotificationHelper.postBudgetExceededNotification(
                context = context,
                listName = listName,
                currentSpent = currentSpending,
                budget = budget,
                currency = currency
            )
        } else if (!isOver) {
            hasAlertedOverBudget = false
        }
    }

    // Animation for pulsing crimson border when over budget
    val infiniteTransition = rememberInfiniteTransition(label = "budget_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val cardBorderColor by animateColorAsState(
        targetValue = when {
            isOver -> Color(0xFFFF3B30).copy(alpha = pulseAlpha)
            remainingBudget != null && remainingBudget <= (budget ?: 0.0) * 0.25 -> Color(0xFFFFB300)
            budget != null -> Color(0xFF00FF88).copy(alpha = 0.5f)
            else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        },
        label = "border_color"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOver) {
                Color(0xFF2A0D0D).copy(alpha = 0.85f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
            }
        ),
        border = BorderStroke(1.5.dp, cardBorderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row: Title & Edit Budget Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isOver) Color(0xFFFF3B30).copy(alpha = 0.2f)
                                else Color(0xFF00FF88).copy(alpha = 0.15f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isOver) Icons.Default.Warning else Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = if (isOver) Color(0xFFFF3B30) else Color(0xFF00FF88),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Monitorizare Buget Live 2026",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (budget != null) "Scădere automată din produse în coș" else "Setează un buget pentru control la raft",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }

                TextButton(
                    onClick = { showEditBudgetDialog = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (budget != null) Icons.Default.Edit else Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (budget != null) "Schimbă" else "Setează",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (budget == null) {
                // No budget set prompt
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Niciun buget setat",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = "Activează avertizările înainte de plată",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showEditBudgetDialog = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Setează Buget", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Budget metrics row: Buget Inițial | În Coș (Scăzut) | Buget Rămas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Buget Setat",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${String.format(Locale.US, "%.2f", budget)} $currency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "În Coș (Scădere)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "-${String.format(Locale.US, "%.2f", currentSpending)} $currency",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (isOver) "Depășire Buget" else "Buget Rămas",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isOver) Color(0xFFFF3B30) else Color(0xFF00FF88),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isOver) "+${String.format(Locale.US, "%.2f", overAmount)} $currency"
                            else "${String.format(Locale.US, "%.2f", remainingBudget ?: 0.0)} $currency",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isOver) Color(0xFFFF3B30) else Color(0xFF00FF88)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar with dynamic color
                val progressFraction = if (budget > 0.0) {
                    (currentSpending / budget).toFloat().coerceIn(0f, 1f)
                } else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = progressFraction,
                    animationSpec = tween(500),
                    label = "budget_progress"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        isOver -> Color(0xFFFF3B30)
                        progressFraction > 0.75f -> Color(0xFFFFB300)
                        else -> Color(0xFF00FF88)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Over budget alert banner
                AnimatedVisibility(
                    visible = isOver,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFFF3B30).copy(alpha = 0.2f))
                                .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = Color(0xFFFF3B30),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "⚠️ ATENȚIE: BUGET DEPĂȘIT ÎNAINTE DE PLATĂ!",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 11.sp,
                                        color = Color(0xFFFF3B30)
                                    )
                                    Text(
                                        text = "Ai depășit bugetul cu +${String.format(Locale.US, "%.2f", overAmount)} $currency. Verifică produsele înainte de casa de marcat!",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 10.sp,
                                        color = Color(0xFFFFCDD2)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for setting / editing budget
    if (showEditBudgetDialog) {
        AlertDialog(
            onDismissRequest = { showEditBudgetDialog = false },
            title = {
                Text(
                    text = if (budget != null) "Ajustează Bugetul" else "Setează Buget pentru $listName",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "Introdu suma maximă pe care vrei să o cheltuiești. Produsele puse în coș vor scădea din acest buget în timp real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = budgetInput,
                        onValueChange = { budgetInput = it.replace(',', '.') },
                        label = { Text("Buget Maxim ($currency)") },
                        placeholder = { Text("ex: 150.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsed = budgetInput.toDoubleOrNull()
                        onUpdateBudget(parsed)
                        showEditBudgetDialog = false
                    }
                ) {
                    Text("Salvează")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (budget != null) {
                            // Option to clear budget
                            onUpdateBudget(null)
                        }
                        showEditBudgetDialog = false
                    }
                ) {
                    Text(if (budget != null) "Șterge Buget" else "Anulează")
                }
            }
        )
    }
}
