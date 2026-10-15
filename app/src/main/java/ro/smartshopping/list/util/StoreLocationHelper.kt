package ro.smartshopping.list.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

data class PresetStore(
    val id: String,
    val name: String,
    val defaultAddress: String,
    val colorHex: Long = 0xFF2E7D32,
    val proximityText: String = "Lângă Militari Residence / Chiajna"
)

object StoreLocationHelper {

    val popularRomanianStores = listOf(
        PresetStore("kaufland_rezervelor", "Kaufland Militari Residence", "Str. Rezervelor 64, Chiajna (lângă Militari Residence)", 0xFFE53935, "📍 200m de Militari Residence"),
        PresetStore("lidl_tineretului", "Lidl Militari Residence", "Str. Tineretului 4 / Str. Rezervelor, Chiajna", 0xFF0050AA, "📍 150m în Militari Residence"),
        PresetStore("auchan_militari", "Auchan Militari", "Centrul Comercial Militari, Bd. Iuliu Maniu 560A", 0xFFC2185B, "📍 900m la intrarea dinspre Chiajna"),
        PresetStore("penny_apeductului", "Penny Chiajna", "Str. Apeductului 62 / Str. Rezervelor, Chiajna", 0xFFD84315, "📍 400m de Militari Residence"),
        PresetStore("metro_militari", "Metro Cash & Carry Militari", "Bd. Iuliu Maniu 500, Militari", 0xFF0D47A1, "📍 1.8 km (3 min cu mașina)"),
        PresetStore("mega_tineretului", "Mega Image Militari Residence", "Str. Tineretului 2 / Ballroom Militari Residence", 0xFFD32F2F, "📍 100m lângă Ballroom"),
        PresetStore("carrefour_militari", "Carrefour Express Chiajna", "Str. Weiner Palada 22, Chiajna", 0xFF1976D2, "📍 600m în Chiajna"),
        PresetStore("profi_chiajna", "Profi City Chiajna", "Str. Tineretului 17, Chiajna", 0xFF388E3C, "📍 300m în Militari Residence")
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
            Toast.makeText(context, "Navigație spre $storeName", Toast.LENGTH_SHORT).show()
            try {
                val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode(query))
                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
            } catch (_: Exception) {}
        }
    }
}
