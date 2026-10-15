package ro.smartshopping.list.util

object ProductEmojiHelper {

    /**
     * Returnează o iconiță/emoji potrivită pentru un produs bazată pe nume și categorie.
     */
    fun getEmojiForProduct(productName: String, category: String? = null): String {
        val p = productName.lowercase()
        val c = category?.lowercase() ?: ""

        return when {
            // Lactate & Ouă
            p.contains("branza") || p.contains("brânză") || p.contains("telemea") || p.contains("cascaval") || p.contains("cașcaval") || p.contains("mozzarella") || p.contains("gouda") || p.contains("parmezan") || p.contains("cheddar") -> "🧀"
            p.contains("unt") || (p.contains("pilos") && p.contains("82")) -> "🧈"
            p.contains("oua") || p.contains("ouă") || p.contains("toneli") -> "🥚"
            p.contains("iaurt") || p.contains("cremosso") || p.contains("danone") || p.contains("sana") || p.contains("kefir") || p.contains("smantana") || p.contains("smântână") -> "🥣"
            p.contains("lapte") || p.contains("zuzu") || p.contains("napolact") || p.contains("albalact") || p.contains("uht") -> "🥛"

            // Brutărie & Panificație
            p.contains("paine") || p.contains("pâine") || p.contains("toast") || p.contains("franzela") || p.contains("franzelă") || p.contains("ciabatta") || p.contains("bagheta") || p.contains("baghetă") -> "🥖"
            p.contains("corn") || p.contains("croissant") -> "🥐"
            p.contains("covrig") || p.contains("covrigi") -> "🥨"
            p.contains("chifla") || p.contains("chiflă") || p.contains("burger") -> "🍔"

            // Fructe
            p.contains("mar") || p.contains("mere") || p.contains("idared") || p.contains("golden") -> "🍎"
            p.contains("banana") || p.contains("banane") -> "🍌"
            p.contains("portocala") || p.contains("portocale") || p.contains("mandarina") || p.contains("mandarine") || p.contains("clementina") || p.contains("citrice") -> "🍊"
            p.contains("lamaie") || p.contains("lămâie") || p.contains("lamai") -> "🍋"
            p.contains("struguri") || p.contains("strugure") -> "🍇"
            p.contains("capsuni") || p.contains("căpșuni") || p.contains("zmeura") || p.contains("afine") -> "🍓"
            p.contains("pepene") -> "🍉"
            p.contains("avocado") -> "🥑"

            // Legume & Ciuperci
            p.contains("cartof") || p.contains("cartofi") -> "🥔"
            p.contains("ceapa") || p.contains("ceapă") -> "🧅"
            p.contains("usturoi") -> "🧄"
            p.contains("rosie") || p.contains("roșie") || p.contains("rosii") || p.contains("roșii") || p.contains("tomate") -> "🍅"
            p.contains("castravete") || p.contains("castraveti") || p.contains("castraveți") -> "🥒"
            p.contains("morcov") || p.contains("morcovi") -> "🥕"
            p.contains("ciuperci") || p.contains("champignon") || p.contains("pleurotus") -> "🍄"
            p.contains("salata") || p.contains("salată") || p.contains("spanac") || p.contains("varza") || p.contains("varză") -> "🥬"
            p.contains("ardei") || p.contains("kapia") || p.contains("gogosar") -> "🫑"
            p.contains("fasole") || p.contains("mazare") || p.contains("linte") -> "🫘"
            p.contains("porumb") -> "🌽"

            // Carne & Pește
            p.contains("pui") || p.contains("fragedo") || p.contains("coco") || p.contains("aripi") || p.contains("pulpe") -> "🍗"
            p.contains("porc") || p.contains("vita") || p.contains("vită") || p.contains("cotlet") || p.contains("ceafa") || p.contains("ceafă") || p.contains("muschi") || p.contains("mușchi") || p.contains("carne") -> "🥩"
            p.contains("salam") || p.contains("sunca") || p.contains("șuncă") || p.contains("carnati") || p.contains("cârnați") || p.contains("crenvursti") || p.contains("crenvurști") || p.contains("bacon") || p.contains("pikok") -> "🥓"
            p.contains("peste") || p.contains("pește") || p.contains("pastrav") || p.contains("păstrăv") || p.contains("somon") || p.contains("ton") || p.contains("dorada") || p.contains("crap") -> "🐟"
            p.contains("creveti") || p.contains("creveți") || p.contains("fructe de mare") -> "🦐"

            // Băuturi & Cafea
            p.contains("cafea") || p.contains("lavazza") || p.contains("jacobs") || p.contains("nescafe") || p.contains("espresso") || p.contains("tchibo") -> "☕"
            p.contains("ceai") -> "🫖"
            p.contains("apa") || p.contains("apă") || p.contains("borsec") || p.contains("dorna") || p.contains("bucovina") || p.contains("plata") || p.contains("minerala") -> "💧"
            p.contains("suc") || p.contains("cola") || p.contains("pepsi") || p.contains("fanta") || p.contains("sprite") || p.contains("santorini") || p.contains("nectar") || p.contains("fresh") -> "🧃"
            p.contains("bere") || p.contains("heineken") || p.contains("tuborg") || p.contains("ursus") || p.contains("timisoreana") -> "🍺"
            p.contains("vin") || p.contains("prosecco") || p.contains("sampanie") -> "🍷"

            // Băcănie & Alimente de bază
            p.contains("ulei") || p.contains("monini") || p.contains("flori") || p.contains("masline") || p.contains("măsline") -> "🫒"
            p.contains("orez") || p.contains("deroni") -> "🍚"
            p.contains("paste") || p.contains("spaghete") || p.contains("macaroane") || p.contains("barilla") -> "🍝"
            p.contains("faina") || p.contains("făină") || p.contains("malai") || p.contains("mălai") -> "🌾"
            p.contains("zahar") || p.contains("zahăr") || p.contains("sare") || p.contains("piper") || p.contains("condiment") -> "🧂"
            p.contains("bulion") || p.contains("sultan") || p.contains("rosii decojite") || p.contains("conserva") || p.contains("conservă") -> "🥫"

            // Dulciuri & Gustări
            p.contains("ciocolata") || p.contains("ciocolată") || p.contains("milka") || p.contains("poiana") || p.contains("nutella") -> "🍫"
            p.contains("biscuit") || p.contains("biscuiți") || p.contains("oreo") || p.contains("croco") -> "🍪"
            p.contains("inghetata") || p.contains("înghețată") -> "🍦"
            p.contains("chips") || p.contains("lays") || p.contains("chito") || p.contains("pufuleti") || p.contains("pufuleți") -> "🍿"
            p.contains("prajitura") || p.contains("prăjitură") || p.contains("tort") -> "🍰"

            // Casă, Curățenie & Igienă
            p.contains("detergent") || p.contains("ariel") || p.contains("persil") || p.contains("tide") || p.contains("dero") -> "🫧"
            p.contains("balsam") || p.contains("lenor") || p.contains("silan") -> "🌸"
            p.contains("sapun") || p.contains("săpun") || p.contains("dove") || p.contains("gel de dus") || p.contains("gel de duș") -> "🧼"
            p.contains("sampon") || p.contains("șampon") || p.contains("deodorant") || p.contains("crema") || p.contains("cremă") -> "🧴"
            p.contains("pasta de dinti") || p.contains("pastă de dinți") || p.contains("colgate") || p.contains("oral-b") -> "🪥"
            p.contains("hartie") || p.contains("hârtie") || p.contains("igienica") || p.contains("igienică") || p.contains("servetele") || p.contains("șervețele") || p.contains("zeva") -> "🧻"
            p.contains("sac") || p.contains("saci") || p.contains("burete") || p.contains("laveta") || p.contains("lavetă") -> "🧹"

            // Animale
            p.contains("caine") || p.contains("câine") || p.contains("pisica") || p.contains("pisică") || p.contains("purina") || p.contains("whiskas") || p.contains("pedigree") -> "🐾"

            // Fallback în funcție de categorie
            c.contains("lactat") -> "🥛"
            c.contains("carne") || c.contains("mezel") -> "🥩"
            c.contains("peste") || c.contains("pește") -> "🐟"
            c.contains("fruct") -> "🍎"
            c.contains("legum") -> "🥦"
            c.contains("brutar") || c.contains("panific") -> "🥖"
            c.contains("bautur") || c.contains("băutur") -> "🧃"
            c.contains("dulc") -> "🍫"
            c.contains("curat") || c.contains("curăț") || c.contains("igien") -> "🧼"
            c.contains("bacani") || c.contains("băcăni") -> "🥫"

            else -> "🛒"
        }
    }

    /**
     * Returnează o iconiță/emoji pentru un titlu de categorie.
     */
    fun getCategoryEmoji(category: String?): String {
        val c = category?.lowercase() ?: ""
        return when {
            c.contains("lactat") -> "🥛"
            c.contains("carne") || c.contains("mezel") -> "🥩"
            c.contains("peste") || c.contains("pește") -> "🐟"
            c.contains("fruct") -> "🍎"
            c.contains("legum") -> "🥦"
            c.contains("brutar") || c.contains("panific") || c.contains("patiser") -> "🥖"
            c.contains("bautur") || c.contains("băutur") || c.contains("cafea") -> "☕"
            c.contains("dulc") || c.contains("snack") -> "🍫"
            c.contains("curat") || c.contains("curăț") || c.contains("cosmetic") || c.contains("igien") -> "🧼"
            c.contains("bacani") || c.contains("băcăni") || c.contains("conserve") -> "🥫"
            c.contains("congelat") -> "🧊"
            c.contains("animal") -> "🐾"
            else -> "📦"
        }
    }
}
