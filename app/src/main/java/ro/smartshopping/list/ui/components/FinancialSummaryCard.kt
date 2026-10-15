package ro.smartshopping.list.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ro.smartshopping.list.domain.model.FinancialSummary
import ro.smartshopping.list.ui.theme.SuccessGreenLight
import java.util.Locale

@Composable
fun FinancialSummaryCard(
    summary: FinancialSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sumar Financiar & Buget",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FinancialMetric(
                    label = "Total Estimat",
                    amount = summary.estimatedTotal,
                    currency = summary.currency
                )
                FinancialMetric(
                    label = "Cheltuit Efectiv",
                    amount = summary.actualSpent,
                    currency = summary.currency,
                    isSuccess = true
                )
                FinancialMetric(
                    label = "Rămas de Plată",
                    amount = summary.remainingEstimated,
                    currency = summary.currency
                )
            }

            if (summary.variance != 0.0 && summary.purchasedItemsCount > 0) {
                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Diferență Real vs. Estimat:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val variancePrefix = if (summary.variance > 0) "+" else ""
                    Text(
                        text = "$variancePrefix${String.format(Locale.US, "%.2f", summary.variance)} ${summary.currency}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (summary.variance <= 0) SuccessGreenLight else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun FinancialMetric(
    label: String,
    amount: Double,
    currency: String,
    isSuccess: Boolean = false
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${String.format(Locale.US, "%.2f", amount)} $currency",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isSuccess && amount > 0) SuccessGreenLight else MaterialTheme.colorScheme.onSurface
        )
    }
}
