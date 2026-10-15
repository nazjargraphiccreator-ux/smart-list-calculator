package ro.smartshopping.list.data.session

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class SavedWhatsAppContact(
    val label: String,
    val phoneNumber: String,
    val emoji: String = "👤"
)

object SavedWhatsAppContactsManager {

    private const val PREFS_NAME = "whatsapp_contacts_prefs"
    private const val KEY_CONTACTS_JSON = "contacts_json"
    private const val KEY_LAST_USED_NUMBER = "last_used_number"

    private val DEFAULT_PRESETS = listOf(
        SavedWhatsAppContact(label = "Soție / Soț", phoneNumber = "", emoji = "💍"),
        SavedWhatsAppContact(label = "Acasă", phoneNumber = "", emoji = "🏠"),
        SavedWhatsAppContact(label = "Partener", phoneNumber = "", emoji = "🤝"),
        SavedWhatsAppContact(label = "Familie", phoneNumber = "", emoji = "👨‍👩‍👧")
    )

    fun getSavedContacts(context: Context): List<SavedWhatsAppContact> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_CONTACTS_JSON, null)
        if (rawJson.isNullOrBlank()) {
            return DEFAULT_PRESETS
        }

        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<SavedWhatsAppContact>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    SavedWhatsAppContact(
                        label = obj.optString("label", "Contact"),
                        phoneNumber = obj.optString("phoneNumber", ""),
                        emoji = obj.optString("emoji", "👤")
                    )
                )
            }
            if (list.isEmpty()) DEFAULT_PRESETS else list
        } catch (e: Exception) {
            DEFAULT_PRESETS
        }
    }

    fun saveContact(context: Context, contact: SavedWhatsAppContact) {
        val current = getSavedContacts(context).filterNot {
            it.phoneNumber.isNotBlank() && it.phoneNumber == contact.phoneNumber
        }.toMutableList()

        current.add(0, contact)
        saveList(context, current.take(8))
    }

    fun getLastUsedNumber(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_USED_NUMBER, "") ?: ""
    }

    fun setLastUsedNumber(context: Context, number: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LAST_USED_NUMBER, number).apply()
    }

    private fun saveList(context: Context, contacts: List<SavedWhatsAppContact>) {
        val arr = JSONArray()
        for (c in contacts) {
            val obj = JSONObject()
            obj.put("label", c.label)
            obj.put("phoneNumber", c.phoneNumber)
            obj.put("emoji", c.emoji)
            arr.put(obj)
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CONTACTS_JSON, arr.toString()).apply()
    }
}
