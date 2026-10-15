package ro.smartshopping.list.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

data class PresetStore(
    val id: String,
    val name: String,
    val defaultAddress: String,
    val colorHex: Long = 0xFF2E7D32
)

object StoreLocationHelper {

    val popularRomanianStores = listOf(
        PresetStore("kaufland_militari", "Kaufland Militari", "Str. Valea Cascadelor 3, Militari, București", 0xFFE53935),
        PresetStore("auchan_militari", "Auchan Militari", "Centrul Comercial Militari, Bd. Iuliu Maniu 560A", 0xFFC2185B),
        PresetStore("kaufland", "Kaufland", "Hipermarket", 0xFFE53935),
        PresetStore("auchan", "Auchan", "Hipermarket", 0xFFC2185B),
        PresetStore("lidl", "Lidl", "Supermarket", 0xFF0050AA),
        PresetStore("carrefour", "Carrefour", "Hipermarket / Market", 0xFF1976D2),
        PresetStore("mega_image", "Mega Image", "Supermarket / Shop&Go", 0xFFD32F2F),
        PresetStore("penny", "Penny", "Discounter", 0xFFD84315),
        PresetStore("profi", "Profi", "Supermarket", 0xFF388E3C),
        PresetStore("metro", "Metro Cash & Carry", "Grosist", 0xFF0D47A1),
        PresetStore("selgros", "Selgros", "Cash & Carry", 0xFFC62828),
        PresetStore("dm", "dm drogerie markt", "Cosmetice & Casă", 0xFF512DA8)
    )

    fun openInGoogleMaps(context: Context, query: String) {
        openStoreInGoogleMaps(context, query, null)
    }

    /**
     * Opens Google Maps targeting a store or search query.
     * Guaranteed safe: if Google Maps is not installed, opens web browser fallback.
     */
    fun openStoreInGoogleMaps(context: Context, storeName: String, address: String? = null) {
        val query = if (!address.isNullOrBlank()) "$storeName, $address" else storeName
        val geoUri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
            setPackage("com.google.android.apps.maps")
        }

        try {
            if (mapIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(mapIntent)
            } else {
                // Browser fallback
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
                val browserIntent = Intent(Intent.ACTION_VIEW, webUri)
                context.startActivity(browserIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Nu s-a putut deschide Google Maps: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
