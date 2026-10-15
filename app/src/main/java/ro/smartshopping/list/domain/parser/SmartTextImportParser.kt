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
    val isDuplicate: Boolean = false,
    val storeName: String? = null,
    val notes: String? = null,
    val spaceCorrectionNote: String? = null
)

data class TextImportResult(
    val items: List<ParsedShoppingItem>,
    val duplicatesCount: Int,
    val totalEstimatedPrice: Double,
    val rawLinesCount: Int,
    val suggestedTitle: String = "Listă Cumpărături",
    val storesDetected: List<String> = emptyList(),
    val itemsByStore: Map<String, List<ParsedShoppingItem>> = emptyMap(),
    val spaceCorrectionsCount: Int = 0
)

class SmartTextImportParser {

    /**
     * Dictionary of common supermarket items in Romania for typo correction,
     * normalization of Romanian diacritics, and accurate category assignment.
     */
    private val romanianDictionary = mapOf(
        // Lactate & Ouă
        "lapte" to ("Lapte" to "Lactate"),
        "lapte dulce" to ("Lapte dulce" to "Lactate"),
        "lapte uht" to ("Lapte UHT" to "Lactate"),
        "unt" to ("Unt" to "Lactate"),
        "iaurt" to ("Iaurt" to "Lactate"),
        "iaurt grecesc" to ("Iaurt grecesc" to "Lactate"),
        "iaurt cremosso" to ("Iaurt Cremosso" to "Lactate"),
        "iaurt cremos" to ("Iaurt Cremosso" to "Lactate"),
        "sana" to ("Sana" to "Lactate"),
        "kefir" to ("Kefir" to "Lactate"),
        "smantana" to ("Smântână" to "Lactate"),
        "smântână" to ("Smântână" to "Lactate"),
        "samntana" to ("Smântână" to "Lactate"),
        "branza" to ("Brânză" to "Lactate"),
        "brânză" to ("Brânză" to "Lactate"),
        "branza capra" to ("Brânză de capră" to "Lactate"),
        "brânză capră" to ("Brânză de capră" to "Lactate"),
        "branza de capra" to ("Brânză de capră" to "Lactate"),
        "telemea" to ("Telemea" to "Lactate"),
        "cascaval" to ("Cașcaval" to "Lactate"),
        "cașcaval" to ("Cașcaval" to "Lactate"),
        "mozzarella" to ("Mozzarella" to "Lactate"),
        "oua" to ("Ouă" to "Lactate & Ouă"),
        "ouă" to ("Ouă" to "Lactate & Ouă"),
        "ou" to ("Ouă" to "Lactate & Ouă"),
        "cofrag oua" to ("Ouă" to "Lactate & Ouă"),
        "cofrag ouă" to ("Ouă" to "Lactate & Ouă"),
        "cofrag" to ("Ouă" to "Lactate & Ouă"),

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
        "fasole" to ("Fasole" to "Legume & Conserve"),
        "fasole boabe" to ("Fasole boabe" to "Legume & Conserve"),
        "fasole verde" to ("Fasole verde" to "Legume & Conserve"),

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
        "bulion sultan" to ("Bulion Sultan" to "Alimente de bază"),
        "pasta tomate" to ("Pastă de tomate" to "Alimente de bază"),

        // Carne & Mezeluri
        "carne pui" to ("Carne de pui" to "Carne & Mezeluri"),
        "piept pui" to ("Piept de pui" to "Carne & Mezeluri"),
        "pulpe pui" to ("Pulpe de pui" to "Carne & Mezeluri"),
        "carne porc" to ("Carne de porc" to "Carne & Mezeluri"),
        "carne vita" to ("Carne de vită" to "Carne & Mezeluri"),
        "carne tocata" to ("Carne tocată" to "Carne & Mezeluri"),
        "salam" to ("Salam" to "Carne & Mezeluri"),
        "salam sasesc" to ("Salam Săsesc" to "Carne & Mezeluri"),
        "salam săsesc" to ("Salam Săsesc" to "Carne & Mezeluri"),
        "scarita afumata" to ("Scăriță afumată" to "Carne & Mezeluri"),
        "scărița afumata" to ("Scăriță afumată" to "Carne & Mezeluri"),
        "scarița afumată" to ("Scăriță afumată" to "Carne & Mezeluri"),
        "scarita" to ("Scăriță afumată" to "Carne & Mezeluri"),
        "file de pui cu susan" to ("File de pui cu susan" to "Carne & Mezeluri"),
        "file pui susan" to ("File de pui cu susan" to "Carne & Mezeluri"),
        "file de pui" to ("File de pui" to "Carne & Mezeluri"),
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

    // Regex for quantity and unit: e.g., "2 bucăți", "3 buc", "2 kg", "500 g", "1 litru", "6 sticle", "1 cofraj", "30 oua"
    private val qtyUnitPattern = Regex(
        """(\d+(?:[.,]\d+)?)\s*(buc(?:ati|ati|ata|ata)?|pcs|kg|kile|kilograme|kilogram|g|gr|grame|gram|l|litru|litri|liter|ml|mililitri|cl|sticle|sticla|btl|cofraj|cofraje|cutie|cutii|pachet|pachete|pack|borcan|borcane|doza|doze|conserva|conserve|punga|pungi|oua|ouă)\b""",
        RegexOption.IGNORE_CASE
    )

    // Regex for trailing quantity with hyphen, colon, or multiplication symbol: e.g. "- 6", "- 2", ": 3", "x 4"
    private val trailingQtyPattern = Regex("""[-*x×:]\s*(\d+(?:[.,]\d+)?)\s*$""")

    // Regex for leading bullet points or numbered lists: "- ", "* ", "• ", "1. ", "2) "
    private val leadingListPattern = Regex("""^[\s\-*•\d+.)\]]+""")

    /**
     * Automatically identifies and fixes missing spaces in Romanian shopping lists:
     * - Number stuck to unit: "2kg" -> "2 kg", "3kg" -> "3 kg", "500g" -> "500 g", "6buc" -> "6 buc", "30oua" -> "30 oua"
     * - Hyphen separators without spaces: "săsesc- 1" -> "săsesc - 1", "Lapte-2" -> "Lapte - 2"
     * - Hyphens missing space after: "-1 buc" -> "- 1 buc"
     */
    fun normalizeMissingSpaces(rawText: String): Pair<String, String?> {
        var result = rawText
        val corrections = mutableListOf<String>()

        // 1. Missing space between number and unit (e.g. 2kg, 3kg, 10kg, 500g, 6buc, 30oua, 1l)
        val numUnitRegex = Regex(
            """(\d+(?:[.,]\d+)?)(kg|kile|kilograme|kilogram|g|gr|grame|gram|l|litru|litri|liter|ml|cl|buc|bucati|bucăți|bucata|bucată|pcs|sticle|sticla|btl|pack|pachete|cutii|cutie|oua|ouă)\b""",
            RegexOption.IGNORE_CASE
        )
        val numUnitMatches = numUnitRegex.findAll(result).toList()
        for (m in numUnitMatches.reversed()) {
            val original = m.value
            val num = m.groupValues[1]
            val unit = m.groupValues[2]
            val replacement = "$num $unit"
            corrections.add("$original ➔ $replacement")
            result = result.replaceRange(m.range, replacement)
        }

        // 2. Missing space around hyphens before numbers: e.g. "săsesc- 1" or "săsesc-1" -> "săsesc - 1"
        val hyphenBeforeRegex = Regex("""([a-zA-ZăâîșțĂÂÎȘȚ0-9])-\s*(?=\d+)""")
        val hyphenBeforeMatches = hyphenBeforeRegex.findAll(result).toList()
        for (m in hyphenBeforeMatches.reversed()) {
            val original = m.value
            val charBefore = m.groupValues[1]
            val replacement = "$charBefore - "
            if (original != "$charBefore - ") {
                corrections.add("$original ➔ $replacement")
            }
            result = result.replaceRange(m.range, replacement)
        }

        // 3. Hyphen followed immediately by a digit without space after: e.g. "Salam -1 buc" -> "Salam - 1 buc"
        val hyphenAfterRegex = Regex("""(?<=\s)-([0-9])""")
        val hyphenAfterMatches = hyphenAfterRegex.findAll(result).toList()
        for (m in hyphenAfterMatches.reversed()) {
            val original = m.value
            val digit = m.groupValues[1]
            val replacement = "- $digit"
            corrections.add("$original ➔ $replacement")
            result = result.replaceRange(m.range, replacement)
        }

        // Clean up any unintended double spaces
        result = result.replace(Regex("""[ \t]{2,}"""), " ").trim()

        val note = if (corrections.isNotEmpty()) {
            corrections.joinToString(", ")
        } else null

        return Pair(result, note)
    }

    /**
     * Detects if a line is an overall document title rather than a product.
     */
    fun isDocumentTitle(line: String): Boolean {
        val clean = line.trim()
        return clean.matches(
            Regex("""^(?:lista(?:\s+de)?\s+cump[aă]r[aă]turi|shopping\s*list|de\s+cump[aă]rat|cump[aă]r[aă]turi)[\s:]*$""", RegexOption.IGNORE_CASE)
        )
    }

    /**
     * Detects if a line represents a store section header (e.g. "Piață :", "Metro:", "Auchan :", "Kaufland:").
     */
    fun isStoreHeader(line: String): String? {
        val clean = line.trim()
        if (clean.isBlank() || isDocumentTitle(clean)) return null

        // 1. Colon section header, e.g. "Piață :", "Metro:", "Auchan :", "Kaufland:", "Lidl:"
        val colonRegex = Regex("""^([A-Za-zăâîșțĂÂÎȘȚ0-9\s&.\-]+?)\s*:\s*$""")
        val colonMatch = colonRegex.matchEntire(clean)
        if (colonMatch != null) {
            val candidate = colonMatch.groupValues[1].trim()
            if (candidate.length <= 35 && !candidate.contains(Regex("""\d+\s*(?:kg|g|l|buc|lei|ron)\b""", RegexOption.IGNORE_CASE))) {
                return normalizeStoreName(candidate)
            }
        }

        // 2. Known standalone store names: "Piață", "Metro", "Auchan", "Kaufland", etc.
        val stripped = clean.removePrefix("[").removeSuffix("]").removePrefix("#").removePrefix("*").removeSuffix("*").trim()
        val known = listOf("Piață", "Piata", "Metro", "Auchan", "Kaufland", "Lidl", "Carrefour", "Mega Image", "Penny", "Profi", "Selgros", "Cora", "Farmacie", "DM", "Brico Depot", "Dedeman", "Leroy Merlin")
        for (k in known) {
            if (stripped.equals(k, ignoreCase = true) || stripped.startsWith("$k ", ignoreCase = true)) {
                return normalizeStoreName(stripped)
            }
        }

        return null
    }

    fun normalizeStoreName(raw: String): String {
        val lower = raw.lowercase().trim()
        return when {
            lower.contains("piat") || lower.contains("piaț") -> "Piață"
            lower.contains("kaufland") -> "Kaufland"
            lower.contains("auchan") -> "Auchan"
            lower.contains("metro") -> "Metro"
            lower.contains("lidl") -> "Lidl"
            lower.contains("carrefour") -> "Carrefour"
            lower.contains("mega") -> "Mega Image"
            lower.contains("penny") -> "Penny"
            lower.contains("profi") -> "Profi"
            lower.contains("selgros") -> "Selgros"
            else -> raw.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
        }
    }

    /**
     * Parses multiline or comma-separated text into structured shopping items,
     * automatically segmenting by stores, resolving missing spaces, and assigning categories.
     */
    fun parse(rawInput: String): TextImportResult {
        if (rawInput.isBlank()) {
            return TextImportResult(emptyList(), 0, 0.0, 0)
        }

        val lines = rawInput.split(Regex("[\r\n;]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val parsedItems = mutableListOf<ParsedShoppingItem>()
        var currentStore: String? = null
        var suggestedTitle = "Listă Cumpărături"
        val storesDetected = mutableListOf<String>()

        for (line in lines) {
            if (isDocumentTitle(line)) {
                suggestedTitle = line.trim().removeSuffix(":").trim()
                continue
            }

            val storeHeader = isStoreHeader(line)
            if (storeHeader != null) {
                currentStore = storeHeader
                if (!storesDetected.contains(storeHeader)) {
                    storesDetected.add(storeHeader)
                }
                continue
            }

            val item = parseLine(line, currentStore)
            if (item != null) {
                parsedItems.add(item)
            }
        }

        // Duplicate detection: group by normalized lowercase name & store
        val nameCounts = parsedItems.groupBy { "${it.storeName ?: ""}_${it.name.lowercase().trim()}" }
        val duplicatesCount = nameCounts.values.count { it.size > 1 }

        val markedItems = parsedItems.map { item ->
            val key = "${item.storeName ?: ""}_${item.name.lowercase().trim()}"
            val isDup = (nameCounts[key]?.size ?: 0) > 1
            item.copy(isDuplicate = isDup)
        }

        val totalEstimated = markedItems.sumOf { it.estimatedLineTotal }
        val roundedTotal = BigDecimal.valueOf(totalEstimated).setScale(2, RoundingMode.HALF_UP).toDouble()
        val spaceCorrections = markedItems.count { it.spaceCorrectionNote != null }

        val itemsByStore = markedItems.groupBy { it.storeName ?: "General" }

        return TextImportResult(
            items = markedItems,
            duplicatesCount = duplicatesCount,
            totalEstimatedPrice = roundedTotal,
            rawLinesCount = lines.size,
            suggestedTitle = suggestedTitle,
            storesDetected = storesDetected,
            itemsByStore = itemsByStore,
            spaceCorrectionsCount = spaceCorrections
        )
    }

    /**
     * Parses a single text line into a structured ParsedShoppingItem.
     */
    fun parseLine(line: String, currentStore: String? = null): ParsedShoppingItem? {
        val rawWithoutBullets = line.replace(leadingListPattern, "").trim()
        if (rawWithoutBullets.isBlank()) return null

        // 1. Automatically identify and correct missing spaces
        val (normalizedLine, spaceCorrectionNote) = normalizeMissingSpaces(rawWithoutBullets)
        var workingText = normalizedLine

        // 2. Extract parenthetical or bracketed notes: e.g. "( galetusa )", "( profi )"
        var extractedNotes: String? = null
        val noteMatch = Regex("""\(([^)]+)\)|\[([^\]]+)\]""").find(workingText)
        if (noteMatch != null) {
            val candidate = (noteMatch.groups[1] ?: noteMatch.groups[2])?.value?.trim()
            if (!candidate.isNullOrBlank()) {
                extractedNotes = when (candidate.lowercase()) {
                    "galetusa", "găletușă", "galetușă" -> "Găletușă"
                    else -> candidate
                }
            }
            workingText = workingText.removeRange(noteMatch.range).trim()
        }

        var extractedPrice: Double? = null

        // 3. Extract Price if present
        val priceMatch = pricePattern.find(workingText)
        if (priceMatch != null) {
            val rawPriceNum = priceMatch.groupValues[1].replace(',', '.')
            extractedPrice = rawPriceNum.toDoubleOrNull()
            workingText = workingText.removeRange(priceMatch.range).trim()
        }

        // 4. Extract Quantity and Unit
        var quantity = 1.0
        var unit = UnitOfMeasure.PCS
        var packageCount: Int? = null

        val qtyMatch = qtyUnitPattern.find(workingText)
        if (qtyMatch != null) {
            val rawQty = qtyMatch.groupValues[1].replace(',', '.')
            quantity = rawQty.toDoubleOrNull() ?: 1.0
            val rawUnitStr = qtyMatch.groupValues[2].lowercase()

            if (rawUnitStr in listOf("oua", "ouă")) {
                unit = UnitOfMeasure.PCS
                packageCount = quantity.toInt()
            } else {
                unit = UnitOfMeasure.fromString(rawUnitStr)
                if (unit in listOf(UnitOfMeasure.PCS, UnitOfMeasure.BOTTLE, UnitOfMeasure.CARTON, UnitOfMeasure.PACK, UnitOfMeasure.CAN, UnitOfMeasure.JAR, UnitOfMeasure.BAG)) {
                    packageCount = quantity.toInt()
                }
            }

            workingText = workingText.removeRange(qtyMatch.range).trim()
        } else {
            // Check for trailing hyphen/colon numbers: e.g. "File de pui cu susan - 6", "Lapte dulce - 2"
            val trailingMatch = trailingQtyPattern.find(workingText)
            if (trailingMatch != null) {
                val num = trailingMatch.groupValues[1].replace(',', '.').toDoubleOrNull()
                if (num != null && num > 0.0) {
                    quantity = num
                    unit = UnitOfMeasure.PCS
                    packageCount = num.toInt()
                    workingText = workingText.removeRange(trailingMatch.range).trim()
                }
            } else {
                // Standalone number check: e.g. "Lapte 2" or "3 Paine"
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
        }

        // 5. Clean up remaining product name
        var nameCandidate = workingText
            .replace(Regex("""^[x*×\-:\s]+"""), "")
            .replace(Regex("""[x*×\-:\s]+$"""), "")
            .trim()

        if (nameCandidate.isBlank()) {
            nameCandidate = rawWithoutBullets
        }

        // Special handling for "Cofrag" when combined with eggs
        if (nameCandidate.equals("cofrag", ignoreCase = true) || nameCandidate.equals("cofrag oua", ignoreCase = true) || nameCandidate.equals("cofrag ouă", ignoreCase = true)) {
            nameCandidate = "Ouă"
            if (extractedNotes == null) {
                extractedNotes = "Cofraj ${quantity.toInt()} buc"
            }
        }

        // 6. Normalize and correct typos via Romanian Supermarket Dictionary
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
            hasCorrection = (finalName != nameCandidate || spaceCorrectionNote != null)
        } else {
            // Capitalize first letter of each word
            finalName = nameCandidate.split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { word ->
                    word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            category = "Alimente"
            hasCorrection = (spaceCorrectionNote != null)
        }

        val estimatedLineTotal = BigDecimal.valueOf((extractedPrice ?: 0.0) * quantity)
            .setScale(2, RoundingMode.HALF_UP).toDouble()

        return ParsedShoppingItem(
            rawText = rawWithoutBullets,
            name = finalName,
            originalNameCandidate = nameCandidate,
            quantity = quantity,
            unit = unit,
            packageCount = packageCount,
            unitPrice = extractedPrice,
            estimatedLineTotal = estimatedLineTotal,
            category = category,
            hasCorrection = hasCorrection,
            storeName = currentStore,
            notes = extractedNotes,
            spaceCorrectionNote = spaceCorrectionNote
        )
    }

    /**
     * Merge duplicate items by summing their quantities and keeping the best known unit price.
     */
    fun mergeDuplicates(items: List<ParsedShoppingItem>): List<ParsedShoppingItem> {
        val merged = mutableMapOf<String, ParsedShoppingItem>()

        for (item in items) {
            val key = "${item.storeName ?: ""}_${item.name.lowercase().trim()}_${item.unit.name}"
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
