package com.example.marketcompare.data

import com.example.marketcompare.model.Product
import com.example.marketcompare.model.ProductTaxonomy
import com.example.marketcompare.service.PricingService
import com.example.marketcompare.utils.ProductNormalizer
import kotlin.math.absoluteValue

/**
 * Realistic MVP fixture data.
 *
 * - 100+ products generated from curated food seeds
 * - same products across multiple supermarkets
 * - similar alternatives with slightly different names/brands/quantities/prices
 */
object SampleProducts {
    private val pricingService = PricingService()
    private const val targetBaseItemCount = 200
    private const val maxModifiedClonesPerItem = 3

    private val supermarkets = listOf("NovaMart", "Fresho", "Markthaus", "GreenPlaza", "PreisPlanet", "DailyDrop")
    private val marketFactor = mapOf(
        "NovaMart" to 0.95,
        "Fresho" to 0.96,
        "Markthaus" to 1.05,
        "GreenPlaza" to 1.06,
        "PreisPlanet" to 0.98,
        "DailyDrop" to 0.93
    )

    private data class Seed(
        val name: String,
        val brand: String?,
        val category: String,
        val subcategory: String,
        val quantity: Double,
        val unit: String,
        val basePrice: Double,
        val isBio: Boolean = false,
        val isVegan: Boolean = false,
        val isVegetarian: Boolean = true,
        val isLactoseFree: Boolean = false,
        val isGlutenFree: Boolean = false,
        val tags: List<String>,
        val synonyms: List<String>,
        val imageUrl: String? = null
    )

    private val coreSeeds = listOf(
        Seed("Vollmilch 1L", "MoonDairy", "Milchprodukte", "Milch", 1.0, "l", 1.19, tags = listOf("milch", "kuhmilch"), synonyms = listOf("frische milch", "trinkmilch")),
        Seed("Bio Vollmilch 1L", "PureLeaf", "Milchprodukte", "Milch", 1.0, "l", 1.59, isBio = true, tags = listOf("bio", "milch"), synonyms = listOf("bio milch")),
        Seed("Griechischer Joghurt 500g", "AlpenHof", "Milchprodukte", "Joghurt", 500.0, "g", 1.89, tags = listOf("joghurt", "protein"), synonyms = listOf("greek yogurt")),
        Seed("Gouda gerieben 200g", "HomeJoy", "Milchprodukte", "Käse", 200.0, "g", 1.69, tags = listOf("käse", "gouda"), synonyms = listOf("geriebener gouda")),

        Seed("Äpfel rot 1kg", null, "Obst", "Frischobst", 1.0, "kg", 2.49, isVegan = true, isLactoseFree = true, isGlutenFree = true, tags = listOf("obst", "apfel"), synonyms = listOf("rote äpfel")),
        Seed("Bananen", null, "Obst", "Exotisch", 1.0, "kg", 1.79, isVegan = true, isLactoseFree = true, isGlutenFree = true, tags = listOf("obst", "banane"), synonyms = listOf("banane gelb")),
        Seed("Heidelbeeren 300g", "Naturgut", "Obst", "Beeren", 300.0, "g", 2.99, isVegan = true, isLactoseFree = true, isGlutenFree = true, tags = listOf("beeren"), synonyms = listOf("blueberries")),

        Seed("Tomaten 500g", null, "Gemüse", "Frischgemüse", 500.0, "g", 1.49, isVegan = true, isLactoseFree = true, isGlutenFree = true, tags = listOf("gemüse", "salat"), synonyms = listOf("strauchtomaten")),
        Seed("Paprika Mix 500g", null, "Gemüse", "Frischgemüse", 500.0, "g", 2.29, isVegan = true, isLactoseFree = true, isGlutenFree = true, tags = listOf("gemüse", "paprika"), synonyms = listOf("paprikamix")),
        Seed("Kichererbsen 400g", "KitchenStar", "Gemüse", "Hülsenfrüchte", 400.0, "g", 1.09, isVegan = true, isLactoseFree = true, tags = listOf("huelsenfrucht", "protein"), synonyms = listOf("chickpeas")),

        Seed("Hähnchenbrustfilet 400g", "GutBio", "Fleisch & Fisch", "Fleisch", 400.0, "g", 4.99, tags = listOf("fleisch", "protein"), synonyms = listOf("chicken breast")),
        Seed("Rinderhack 500g", "Landjunker", "Fleisch & Fisch", "Fleisch", 500.0, "g", 5.49, tags = listOf("rind", "hackfleisch"), synonyms = listOf("ground beef")),
        Seed("Lachsfilet 250g", "Fishline", "Fleisch & Fisch", "Fisch", 250.0, "g", 6.29, isGlutenFree = true, tags = listOf("fisch", "omega3"), synonyms = listOf("salmon fillet")),
        Seed("Putenaufschnitt 150g", "Nordwurst", "Fleisch & Fisch", "Wurst", 150.0, "g", 2.29, tags = listOf("wurst", "aufschnitt"), synonyms = listOf("putenbrust aufschnitt")),

        Seed("Haferdrink Barista 1L", "Oatoria", "Vegane Alternativen", "Pflanzliche Milch", 1.0, "l", 2.19, isVegan = true, isLactoseFree = true, tags = listOf("hafer", "drink"), synonyms = listOf("hafermilch", "oat milk")),
        Seed("Sojadrink Ungesüßt 1L", "SoyVale", "Vegane Alternativen", "Pflanzliche Milch", 1.0, "l", 1.99, isVegan = true, isLactoseFree = true, tags = listOf("soja", "drink"), synonyms = listOf("sojamilch")),
        Seed("Vegane Burger Patties 180g", "PlantPeak", "Vegane Alternativen", "Fleischersatz", 180.0, "g", 2.99, isVegan = true, isLactoseFree = true, tags = listOf("vegan", "burger"), synonyms = listOf("veggie patty")),

        Seed("Mineralwasser Classic 1.5L", "ClearSpring", "Getränke", "Wasser", 1.5, "l", 0.49, isVegan = true, isLactoseFree = true, isGlutenFree = true, tags = listOf("wasser"), synonyms = listOf("sprudel")),
        Seed("Orangensaft 1L", "SunDrop", "Getränke", "Säfte", 1.0, "l", 2.29, isVegan = true, tags = listOf("saft", "orange"), synonyms = listOf("orangensaft")),
        Seed("Kaffee gemahlen 500g", "BeanForge", "Getränke", "Kaffee", 500.0, "g", 6.99, isVegan = true, tags = listOf("kaffee"), synonyms = listOf("ground coffee")),
        Seed("Kamillentee 40g", "TeaMoss", "Getränke", "Tee", 40.0, "g", 1.89, isVegan = true, tags = listOf("tee"), synonyms = listOf("chamomile tea")),

        Seed("Spaghetti 500g", "PastaMondo", "Trockenwaren", "Pasta", 500.0, "g", 1.79, isVegan = true, tags = listOf("pasta"), synonyms = listOf("nudeln", "spagetti")),
        Seed("Penne Rigate 500g", "GranoVia", "Trockenwaren", "Pasta", 500.0, "g", 1.99, isVegan = true, tags = listOf("pasta", "penne"), synonyms = listOf("penne")),
        Seed("Basmati Reis 1kg", "RiceField", "Trockenwaren", "Reis", 1.0, "kg", 3.49, isVegan = true, isGlutenFree = true, tags = listOf("reis"), synonyms = listOf("basmatireis")),
        Seed("Haferflocken Feinblatt 500g", "MorningMill", "Trockenwaren", "Getreide", 500.0, "g", 1.49, isVegan = true, tags = listOf("haferflocken"), synonyms = listOf("oats")),

        Seed("Kartoffelchips Paprika 150g", "CrunchMates", "Snacks", "Chips", 150.0, "g", 1.89, isVegan = true, tags = listOf("chips", "paprika"), synonyms = listOf("paprika chips")),
        Seed("Nussmix geröstet 200g", "NutHaven", "Snacks", "Nüsse", 200.0, "g", 3.49, isVegan = true, tags = listOf("nuesse"), synonyms = listOf("studentenfutter")),
        Seed("Vollmilchschokolade 100g", "CocoaJoy", "Snacks", "Süßigkeiten", 100.0, "g", 1.29, tags = listOf("schokolade"), synonyms = listOf("chocolate")),

        Seed("Roggenbrot 500g", "BakerLane", "Backwaren", "Brot", 500.0, "g", 1.99, isVegan = true, tags = listOf("brot"), synonyms = listOf("roggen vollkornbrot")),
        Seed("Mehrkornbrötchen 6 Stk", "GrainyDay", "Backwaren", "Brötchen", 6.0, "stk", 1.49, isVegan = true, tags = listOf("broetchen"), synonyms = listOf("mehrkorn broetchen")),
        Seed("Buttercroissant 80g", "FlourNest", "Backwaren", "Gebäck", 80.0, "g", 0.79, tags = listOf("croissant", "gebaeck"), synonyms = listOf("franzoesisches croissant")),

        Seed("Tiefkühlpizza Margherita 350g", "FrostBite", "Tiefkühlprodukte", "Pizza", 350.0, "g", 3.29, isVegetarian = true, tags = listOf("pizza"), synonyms = listOf("frozen pizza")),
        Seed("Tiefkühl Erbsen 450g", "IceHarvest", "Tiefkühlprodukte", "Gemüse", 450.0, "g", 1.99, isVegan = true, tags = listOf("erbsen", "gemuese"), synonyms = listOf("frozen peas")),
        Seed("Vanilleeis 900ml", "CreamCloud", "Tiefkühlprodukte", "Eis", 900.0, "ml", 4.49, tags = listOf("eis"), synonyms = listOf("vanilla ice cream")),

        Seed("Tomatensauce Basilikum 400g", "RedGarden", "Konserven", "Gläser", 400.0, "g", 1.89, isVegan = true, tags = listOf("tomatensauce"), synonyms = listOf("tomatensosse", "pasta sauce")),
        Seed("Mais 300g", "SunnyKernel", "Konserven", "Dosen", 300.0, "g", 1.29, isVegan = true, tags = listOf("mais"), synonyms = listOf("sweet corn")),
        Seed("Kidneybohnen 400g", "HomeJoy", "Konserven", "Dosen", 400.0, "g", 0.99, isVegan = true, isGlutenFree = true, tags = listOf("bohnen"), synonyms = listOf("kidney beans"))
    )

    private val seeds: List<Seed> by lazy {
        buildExpandedSeeds(coreSeeds, targetBaseItemCount, maxModifiedClonesPerItem)
    }

    val products: List<Product> by lazy {
        val output = mutableListOf<Product>()
        var idCounter = 1

        seeds.forEach { seed ->
            require(ProductTaxonomy.isValid(seed.category, seed.subcategory)) {
                "Invalid taxonomy assignment: ${seed.category} -> ${seed.subcategory}"
            }
            supermarkets.forEach { market ->
                val adjustedPrice = adjustPrice(seed.basePrice, market, seed.name)
                val normalizedName = ProductNormalizer.normalizeProductName(seed.name, seed.brand)
                val product = Product(
                    id = "p${idCounter++}",
                    name = marketSpecificName(seed.name, market),
                    brand = marketSpecificBrand(seed.brand, market),
                    category = seed.category,
                    subcategory = seed.subcategory,
                    quantity = seed.quantity,
                    unit = seed.unit,
                    price = adjustedPrice,
                    currency = "EUR",
                    supermarket = market,
                    country = "DE",
                    isBio = seed.isBio,
                    isVegan = seed.isVegan,
                    isVegetarian = seed.isVegetarian,
                    isLactoseFree = seed.isLactoseFree,
                    isGlutenFree = seed.isGlutenFree,
                    tags = seed.tags,
                    synonyms = seed.synonyms,
                    normalizedName = normalizedName,
                    pricePerUnit = pricingService.calculatePricePerUnit(adjustedPrice, normalizeQuantityForUnit(seed.quantity, seed.unit)),
                    imageUrl = seed.imageUrl,
                    inStock = deterministicStock(seed.name, market)
                )
                output += product
            }
        }
        output
    }

    private fun buildExpandedSeeds(
        baseSeeds: List<Seed>,
        targetCount: Int,
        maxClones: Int
    ): List<Seed> {
        if (baseSeeds.isEmpty()) return emptyList()
        if (baseSeeds.size >= targetCount) return baseSeeds.take(targetCount)

        val expanded = baseSeeds.toMutableList()
        val cloneCountByBase = IntArray(baseSeeds.size)
        var cursor = 0

        while (expanded.size < targetCount) {
            val baseIndex = cursor % baseSeeds.size
            if (cloneCountByBase[baseIndex] >= maxClones) {
                cursor += 1
                continue
            }
            val cloneOrdinal = cloneCountByBase[baseIndex] + 1
            val clone = createModifiedClone(baseSeeds[baseIndex], cloneOrdinal, cursor)
            expanded += clone
            cloneCountByBase[baseIndex] = cloneOrdinal
            cursor += 1

            if (cloneCountByBase.all { it >= maxClones }) break
        }

        return expanded
            .distinctBy { "${it.name}|${it.brand}|${it.quantity}|${it.unit}|${it.category}|${it.subcategory}" }
            .take(targetCount)
    }

    private fun createModifiedClone(source: Seed, cloneOrdinal: Int, salt: Int): Seed {
        val namePrefix = when (cloneOrdinal) {
            1 -> "Daily"
            2 -> "Classic"
            else -> "Select"
        }
        val quantityFactor = when (cloneOrdinal) {
            1 -> 0.9
            2 -> 1.1
            else -> 1.25
        }
        val adjustedQuantity = when (source.unit.lowercase()) {
            "g", "ml" -> ((source.quantity * quantityFactor / 10.0).toInt() * 10).coerceAtLeast(50).toDouble()
            "kg", "l" -> ((source.quantity * quantityFactor * 100.0).toInt() / 100.0).coerceAtLeast(0.25)
            "stk" -> (source.quantity + cloneOrdinal).coerceAtLeast(1.0)
            else -> source.quantity
        }
        val priceFactor = when (cloneOrdinal) {
            1 -> 0.94
            2 -> 1.06
            else -> 1.12
        }
        val variantToken = ((source.name.hashCode() + salt).absoluteValue % 4) + 1
        val cloneBrand = when {
            source.brand.isNullOrBlank() -> generatedBrandForCategory(source.category, cloneOrdinal, variantToken)
            cloneOrdinal == 1 -> "${source.brand} Daily"
            cloneOrdinal == 2 -> "${source.brand} Classic"
            else -> "${source.brand} Select"
        }

        return source.copy(
            name = "$namePrefix ${source.name}",
            brand = cloneBrand,
            quantity = adjustedQuantity,
            basePrice = (source.basePrice * priceFactor).coerceAtLeast(0.49),
            tags = (source.tags + listOf(namePrefix.lowercase(), "variant", "type_${cloneOrdinal}")).distinct(),
            synonyms = (source.synonyms + listOf("${source.name.lowercase()} $namePrefix".lowercase())).distinct()
        )
    }

    private fun generatedBrandForCategory(category: String, cloneOrdinal: Int, variantToken: Int): String {
        val base = when (category) {
            "Obst", "Gemüse" -> "FieldBloom"
            "Milchprodukte" -> "CreamVale"
            "Fleisch & Fisch" -> "PrimeNest"
            "Getränke" -> "FlowSip"
            "Backwaren" -> "BakeHaven"
            "Tiefkühlprodukte" -> "ArcticTray"
            "Snacks" -> "CrunchVale"
            "Trockenwaren", "Konserven" -> "PantryForge"
            "Vegane Alternativen" -> "PlantMingle"
            else -> "HomeCraft"
        }
        val tier = when (cloneOrdinal) {
            1 -> "Daily"
            2 -> "Classic"
            else -> "Select"
        }
        return "$base $tier $variantToken"
    }

    private fun normalizeQuantityForUnit(quantity: Double, unit: String): Double {
        return when (unit.lowercase()) {
            "g", "ml" -> quantity / 1000.0
            else -> quantity
        }.coerceAtLeast(0.0001)
    }

    private fun adjustPrice(basePrice: Double, market: String, key: String): Double {
        val factor = marketFactor[market] ?: 1.0
        val jitter = (((market + key).hashCode().absoluteValue % 9) - 4) * 0.01
        return ((basePrice * factor + jitter) * 100.0).toInt() / 100.0
    }

    private fun deterministicStock(name: String, market: String): Boolean {
        return ((name + market).hashCode().absoluteValue % 10) != 0
    }

    private fun marketSpecificName(base: String, market: String): String {
        // Keep exact same products across markets for strong EXACT matches,
        // but inject a few slight naming variants for realistic CLOSE/SIMILAR matches.
        return when {
            market == "Markthaus" && base.contains("Tomatensauce", ignoreCase = true) -> "Tomatensosse Basilikum 400g"
            market == "GreenPlaza" && base.contains("Haferdrink", ignoreCase = true) -> "Hafermilch Barista 1L"
            market == "PreisPlanet" && base.contains("Mozzarella", ignoreCase = true) -> "Mozarella 125g"
            else -> base
        }
    }

    private fun marketSpecificBrand(base: String?, market: String): String? {
        if (base == null) return null
        return when (market) {
            "NovaMart" -> if (base == "MoonDairy") "MoonDairy" else base
            "Fresho" -> if (base == "MoonDairy") "AlpenHof" else base
            "DailyDrop" -> if (base == "HomeJoy") "NatureJoy" else base
            else -> base
        }
    }
}
