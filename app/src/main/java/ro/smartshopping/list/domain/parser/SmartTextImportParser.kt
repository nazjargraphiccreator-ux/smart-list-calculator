package ro.smartshopping.list.domain.parser

import ro.smartshopping.list.domain.model.UnitOfMeasure
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

data class ParsedShoppingItem(
    val id: String = UUID.randomUUID().toString(),
    val rawText: String,
    val name: String,
    val originalNameCandidate: String,
    val quantity: Double,
    val unit: UnitOfMeasure,
    val packageCount: Int? = null,
    val unitPrice: Double? = null,
    val estimatedLineTotal: Double = (unitPrice ?: 0.0) * quantity,
    val category: String = "Alimente",
    val hasCorrection: Boolean = false,
    val isDuplicate: Boolean = false
)

data class TextImportResult(
    val items: List<ParsedShoppingItem>,
    val duplicatesCount: Int,
    val totalEstimatedPrice: Double,
    val rawLinesCount: Int
)

class SmartTextImportParser {

    /**
     * Dictionary of common supermarket items in Romania for typo correction,
     * normalization of Romanian diacritics, and accurate category assignment.
     */
    private val romanianDictionary = mapOf(
        // Lactate & Ouă
        "lapte" to ("Lapte" to "Lactate"),
        "lapte dulce" to ("Lapte" to "Lactate"),
        "lapte uht" to ("Lapte UHT" to "Lactate"),
        "unt" to ("Unt" to "Lactate"),
        "iaurt" to ("Iaurt" to "Lactate"),
        "iaurt grecesc" to ("Iaurt grecesc" to "Lactate"),
        "sana" to ("Sana" to "Lactate"),
        "kefir" to ("Kefir" to "Lactate"),
        "smantana" to ("Smântână" to "Lactate"),
        "smântână" to ("Smântână" to "Lactate"),
        "branza" to ("Brânză" to "Lactate"),
        "brânză" to ("Brânză" to "Lactate"),
        "telemea" to ("Telemea" to "Lactate"),
        "cascaval" to ("Cașcaval" to "Lactate"),
        "cașcaval" to ("Cașcaval" to "Lactate"),
        "mozzarella" to ("Mozzarella" to "Lactate"),
        "oua" to ("Ouă" to "Lactate & Ouă"),
        "ouă" to ("Ouă" to "Lactate & Ouă"),
        "ou" to ("Ouă" to "Lactate & Ouă"),

        // Brutărie & Panificație
        "paine" to ("Pâine" to "Brutărie"),
        "pâine" to ("Pâine" to "Brutărie"),
        "paine feliata" to ("Pâine feliată" to "Brutărie"),
        "chifle" to ("Chifle" to "Brutărie"),
        "covrigi" to ("Covrigi" to "Brutărie"),
        "bagheta" to ("Baghetă" to "Brutărie"),
        "toast" to ("Pâine Toast" to "Brutărie"),

        // Fructe & Legume
        "mere" to ("Mere" to "Fructe & Legume"),
        "mar" to ("Mere" to "Fructe & Legume"),
        "pere" to ("Pere" to "Fructe & Legume"),
        "banane" to ("Banane" to "Fructe & Legume"),
        "portocale" to ("Portocale" to "Fructe & Legume"),
        "mandarine" to ("Mandarine" to "Fructe & Legume"),
        "lamai" to ("Lămâi" to "Fructe & Legume"),
        "lămâi" to ("Lămâi" to "Fructe & Legume"),
        "cartofi" to ("Cartofi" to "Fructe & Legume"),
        "rosii" to ("Roșii" to "Fructe & Legume"),
        "roșii" to ("Roșii" to "Fructe & Legume"),
        "castraveti" to ("Castraveți" to "Fructe & Legume"),
        "castraveți" to ("Castraveți" to "Fructe & Legume"),
        "ceapa" to ("Ceapă" to "Fructe & Legume"),
        "ceapă" to ("Ceapă" to "Fructe & Legume"),
        "usturoi" to ("Usturoi" to "Fructe & Legume"),
        "morcovi" to ("Morcovi" to "Fructe & Legume"),
        "ardei" to ("Ardei" to "Fructe & Legume"),
        "ardei gras" to ("Ardei gras" to "Fructe & Legume"),
        "salata" to ("Salată" to "Fructe & Legume"),
        "salată" to ("Salată" to "Fructe & Legume"),
        "ciuperci" to ("Ciuperci" to "Fructe & Legume"),

        // Curățenie & Menaj
        "detergent" to ("Detergent" to "Curățenie & Casă"),
        "deterg" to ("Detergent" to "Curățenie & Casă"),
        "detergent vase" to ("Detergent de vase" to "Curățenie & Casă"),
        "balsam rufe" to ("Balsam de rufe" to "Curățenie & Casă"),
        "hartie igienica" to ("Hârtie igienică" to "Curățenie & Casă"),
        "hartie ig" to ("Hârtie igienică" to "Curățenie & Casă"),
        "servetele" to ("Șervețele" to "Curățenie & Casă"),
        "saci menajeri" to ("Saci menajeri" to "Curățenie & Casă"),
        "bureti vase" to ("Bureți de vase" to "Curățenie & Casă"),

        // Băuturi
        "apa minerala" to ("Apă minerală" to "Băuturi"),
        "apa plata" to ("Apă plată" to "Băuturi"),
        "apa min" to ("Apă minerală" to "Băuturi"),
        "apa" to ("Apă minerală" to "Băuturi"),
        "suc" to ("Suc" to "Băuturi"),
        "bere" to ("Bere" to "Băuturi"),
        "vin" to ("Vin" to "Băuturi"),
        "cola" to ("Coca-Cola" to "Băuturi"),
        "ceai" to ("Ceai" to "Băuturi"),
        "cafea" to ("Cafea" to "Băuturi calde"),
        "cafea boabe" to ("Cafea boabe" to "Băuturi calde"),
        "cafea macinata" to ("Cafea măcinată" to "Băuturi calde"),

        // Băcănie & Alimente de bază
        "ulei" to ("Ulei" to "Alimente de bază"),
        "ulei floarea soarelui" to ("Ulei de floarea-soarelui" to "Alimente de bază"),
        "ulei masline" to ("Ulei de măsline" to "Alimente de bază"),
        "zahar" to ("Zahăr" to "Alimente de bază"),
        "zahăr" to ("Zahăr" to "Alimente de bază"),
        "faina" to ("Făină" to "Alimente de bază"),
        "făină" to ("Făină" to "Alimente de bază"),
        "malai" to ("Mălai" to "Alimente de bază"),
        "orez" to ("Orez" to "Alimente de bază"),
        "paste" to ("Paste" to "Alimente de bază"),
        "spaghete" to ("Spaghete" to "Alimente de bază"),
        "sare" to ("Sare" to "Alimente de bază"),
        "otet" to ("Oțet" to "Alimente de bază"),
        "bulion" to ("Bulion" to "Alimente de bază"),
        "pasta tomate" to ("Pastă de tomate" to "Alimente de bază"),

        // Carne & Mezeluri
        "carne pui" to ("Carne de pui" to "Carne & Mezeluri"),
        "piept pui" to ("Piept de pui" to "Carne & Mezeluri"),
        "pulpe pui" to ("Pulpe de pui" to "Carne & Mezeluri"),
        "carne porc" to ("Carne de porc" to "Carne & Mezeluri"),
        "carne vita" to ("Carne de vită" to "Carne & Mezeluri"),
        "carne tocata" to ("Carne tocată" to "Carne & Mezeluri"),
        "salam" to ("Salam" to "Carne & Mezeluri"),
        "sunca" to ("Șuncă" to "Carne & Mezeluri"),
        "crenvursti" to ("Crenvurști" to "Carne & Mezeluri"),
        "peste" to ("Pește" to "Carne & Mezeluri"),

        // Îngrijire personală
        "sapun" to ("Săpun" to "Îngrijire personală"),
        "săpun" to ("Săpun" to "Îngrijire personală"),
        "sampon" to ("Șampon" to "Îngrijire personală"),
        "gel dus" to ("Gel de duș" to "Îngrijire personală"),
        "pasta dinti" to ("Pastă de dinți" to "Îngrijire personală"),
        "pasta de dinti" to ("Pastă de dinți" to "Îngrijire personală"),
        "deodorant" to ("Deodorant" to "Îngrijire personală")
    )

    // Regex for price formats: "7,50 lei", "7.50 RON", "7 lei", "la 5 lei", "5.50lei", "@ 10 RON", "12 ron/buc"
    private val pricePattern = Regex(
        """(?:la|@)?\s*(\d+(?:[.,]\d{1,2})?)\s*(?:lei|ron|lei/buc|ron/buc|lei/kg|ron/kg)\b""",
        RegexOption.IGNORE_CASE
    )

    // Regex for quantity and unit: e.g., "2 bucăți", "3 buc", "2 kg", "500 g", "1 litru", "6 sticle", "1 cofraj"
    private val qtyUnitPattern = Regex(
        """(\d+(?:[.,]\d+)?)\s*(buc(?:ati|ati|ata|ata)?|pcs|kg|kile|kilograme|kilogram|g|gr|grame|gram|l|litru|litri|liter|ml|mililitri|cl|sticle|sticla|btl|cofraj|cofraje|cutie|cutii|pachet|pachete|pack|borcan|borcane|doza|doze|conserva|conserve|punga|pungi)\b""",
        RegexOption.IGNORE_CASE
    )

    // Regex for leading bullet points or numbered lists: "- ", "* ", "• ", "1. ", "2) "
    private val leadingListPattern = Regex("""^[\s\-*•\d+.)\]]+""")

    /**
     * Parses multiline or comma-separated text into structured shopping items.
     */
    fun parse(rawInput: String): TextImportResult {
        if (rawInput.isBlank()) {
            return TextImportResult(emptyList(), 0, 0.0, 0)
        }

        // Split by lines or semicolons
        val lines = rawInput.split(Regex("[\r\n;]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val parsedItems = mutableListOf<ParsedShoppingItem>()

        for (line in lines) {
            val item = parseLine(line)
            if (item != null) {
                parsedItems.add(item)
            }
        }

        // Duplicate detection: group by normalized lowercase name
        val nameCounts = parsedItems.groupBy { it.name.lowercase().trim() }
        val duplicatesCount = nameCounts.values.count { it.size > 1 }

        val markedItems = parsedItems.map { item ->
            val isDup = (nameCounts[item.name.lowercase().trim()]?.size ?: 0) > 1
            item.copy(isDuplicate = isDup)
        }

        val totalEstimated = markedItems.sumOf { it.estimatedLineTotal }
        val roundedTotal = BigDecimal.valueOf(totalEstimated).setScale(2, RoundingMode.HALF_UP).toDouble()

        return TextImportResult(
            items = markedItems,
            duplicatesCount = duplicatesCount,
            totalEstimatedPrice = roundedTotal,
            rawLinesCount = lines.size
        )
    }

    /**
     * Parses a single text line into a structured ParsedShoppingItem.
     */
    fun parseLine(line: String): ParsedShoppingItem? {
        val cleanLine = line.replace(leadingListPattern, "").trim()
        if (cleanLine.isBlank()) return null

        var workingText = cleanLine
        var extractedPrice: Double? = null

        // 1. Extract Price if present
        val priceMatch = pricePattern.find(workingText)
        if (priceMatch != null) {
            val rawPriceNum = priceMatch.groupValues[1].replace(',', '.')
            extractedPrice = rawPriceNum.toDoubleOrNull()
            // Remove the price text from workingText
            workingText = workingText.removeRange(priceMatch.range).trim()
        }

        // 2. Extract Quantity and Unit
        var quantity = 1.0
        var unit = UnitOfMeasure.PCS
        var packageCount: Int? = null

        val qtyMatch = qtyUnitPattern.find(workingText)
        if (qtyMatch != null) {
            val rawQty = qtyMatch.groupValues[1].replace(',', '.')
            quantity = rawQty.toDoubleOrNull() ?: 1.0
            val rawUnitStr = qtyMatch.groupValues[2]
            unit = UnitOfMeasure.fromString(rawUnitStr)

            // If unit is packaging (BOTTLE, CARTON, PACK, CAN, JAR, BAG) or discrete pieces (PCS), store packageCount
            if (unit in listOf(UnitOfMeasure.PCS, UnitOfMeasure.BOTTLE, UnitOfMeasure.CARTON, UnitOfMeasure.PACK, UnitOfMeasure.CAN, UnitOfMeasure.JAR, UnitOfMeasure.BAG)) {
                packageCount = quantity.toInt()
            }

            // Remove qty + unit from workingText
            workingText = workingText.removeRange(qtyMatch.range).trim()
        } else {
            // Check if there is just a standalone trailing or leading number (e.g. "Lapte 2" or "3 Paine")
            val standaloneNumPattern = Regex("""\b(\d+(?:[.,]\d+)?)\b""")
            val standaloneMatch = standaloneNumPattern.find(workingText)
            if (standaloneMatch != null) {
                val num = standaloneMatch.groupValues[1].replace(',', '.').toDoubleOrNull()
                if (num != null && num > 0.0) {
                    quantity = num
                    if (unit == UnitOfMeasure.PCS) {
                        packageCount = num.toInt()
                    }
                    workingText = workingText.removeRange(standaloneMatch.range).trim()
                }
            }
        }

        // 3. Clean up the remaining name
        // Strip out prepositions like "de", "la", "x", "*", trailing punctuation
        var nameCandidate = workingText
            .replace(Regex("""^[x*×\-:\s]+"""), "")
            .replace(Regex("""[x*×\-:\s]+$"""), "")
            .trim()

        if (nameCandidate.isBlank()) {
            nameCandidate = cleanLine
        }

        // 4. Normalize and Correct Typos via Romanian Supermarket Dictionary
        val normalizedKey = nameCandidate.lowercase().trim()
        val strippedKey = removeDiacritics(normalizedKey)
        val dictEntry = romanianDictionary[normalizedKey]
            ?: romanianDictionary[strippedKey]
            ?: romanianDictionary.entries.firstOrNull { strippedKey.contains(removeDiacritics(it.key)) }?.value

        val finalName: String
        val category: String
        val hasCorrection: Boolean

        if (dictEntry != null) {
            finalName = dictEntry.first
            category = dictEntry.second
            hasCorrection = (finalName != nameCandidate)
        } else {
            // Capitalize first letter of each word
            finalName = nameCandidate.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            category = "Alimente"
            hasCorrection = false
        }

        val estimatedLineTotal = BigDecimal.valueOf((extractedPrice ?: 0.0) * quantity)
            .setScale(2, RoundingMode.HALF_UP).toDouble()

        return ParsedShoppingItem(
            rawText = cleanLine,
            name = finalName,
            originalNameCandidate = nameCandidate,
            quantity = quantity,
            unit = unit,
            packageCount = packageCount,
            unitPrice = extractedPrice,
            estimatedLineTotal = estimatedLineTotal,
            category = category,
            hasCorrection = hasCorrection
        )
    }

    /**
     * Merge duplicate items by summing their quantities and keeping the best known unit price.
     */
    fun mergeDuplicates(items: List<ParsedShoppingItem>): List<ParsedShoppingItem> {
        val merged = mutableMapOf<String, ParsedShoppingItem>()

        for (item in items) {
            val key = "${item.name.lowercase().trim()}_${item.unit.name}"
            val existing = merged[key]
            if (existing == null) {
                merged[key] = item.copy(isDuplicate = false)
            } else {
                val newQty = existing.quantity + item.quantity
                val price = existing.unitPrice ?: item.unitPrice
                val newTotal = BigDecimal.valueOf((price ?: 0.0) * newQty).setScale(2, RoundingMode.HALF_UP).toDouble()
                val mergedPackageCount = if (existing.packageCount != null || item.packageCount != null) {
                    (existing.packageCount ?: existing.quantity.toInt()) + (item.packageCount ?: item.quantity.toInt())
                } else null
                merged[key] = existing.copy(
                    quantity = newQty,
                    unitPrice = price,
                    estimatedLineTotal = newTotal,
                    packageCount = mergedPackageCount,
                    isDuplicate = false
                )
            }
        }

        return merged.values.toList()
    }

    private fun removeDiacritics(str: String): String {
        return str
            .replace('ă', 'a').replace('Ă', 'A')
            .replace('â', 'a').replace('Â', 'A')
            .replace('î', 'i').replace('Î', 'I')
            .replace('ș', 's').replace('Ș', 'S').replace('ş', 's').replace('Ş', 's')
            .replace('ț', 't').replace('Ț', 'T').replace('ţ', 't').replace('Ţ', 't')
    }
}
