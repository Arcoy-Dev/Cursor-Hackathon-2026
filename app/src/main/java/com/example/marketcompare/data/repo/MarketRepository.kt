package com.example.marketcompare.data.repo

import com.example.marketcompare.data.db.ComparisonRecordEntity
import com.example.marketcompare.data.db.HistoryDao
import com.example.marketcompare.data.db.SearchRecordEntity
import com.example.marketcompare.data.model.DailySavingsPoint
import com.example.marketcompare.data.model.ProductComparison
import com.example.marketcompare.data.model.ProductSearchCount
import com.example.marketcompare.data.model.ProductSearchHourPoint
import com.example.marketcompare.data.model.ProductSearchTrendPoint
import com.example.marketcompare.data.model.ProductOffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.util.Calendar
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class MarketRepository(
    private val historyDao: HistoryDao,
    private val localDataSource: InventoryDataSource,
    private val remoteDataSource: InventoryDataSource
) {
    private val syntheticStats by lazy {
        SyntheticStatsGenerator { localDataSource.catalogProductCategories() }
    }

    fun availableSupermarkets(): List<String> {
        return (localDataSource.supermarkets() + remoteDataSource.supermarkets()).toList().sorted()
    }

    fun analyticsCatalogProducts(): List<String> {
        val local = localDataSource.catalogProductCategories().keys
        val remote = remoteDataSource.catalogProductCategories().keys
        return (local + remote)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
            .sortedBy { it.lowercase() }
    }

    suspend fun searchOffers(query: String, selectedMarkets: Set<String>): List<ProductOffer> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return emptyList()

        val filtered = withContext(Dispatchers.Default) {
            fetchOffers(query, selectedMarkets)
                .sortedWith(compareBy<ProductOffer> { it.name }.thenBy { it.price })
        }

        withContext(Dispatchers.IO) {
            saveSearchRecord(query = query, supermarkets = selectedMarkets.size, offerCount = filtered.size)
        }
        return filtered
    }

    suspend fun suggestProducts(query: String, limit: Int = 8): List<String> {
        val normalizedQuery = query.trim().lowercase()
        if (normalizedQuery.isBlank()) return emptyList()
        return withContext(Dispatchers.Default) {
            localDataSource.suggestProducts(
                query = normalizedQuery,
                limit = limit.coerceIn(1, 12)
            )
        }
    }

    suspend fun compareProducts(offers: List<ProductOffer>, selectedProducts: Set<String>): List<ProductComparison> {
        val comparisons = withContext(Dispatchers.Default) {
            val filteredOffers = if (selectedProducts.isEmpty()) {
                offers
            } else {
                offers.filter { it.name in selectedProducts }
            }
            val grouped = filteredOffers.groupBy { it.name }
            grouped.map { (name, groupOffers) ->
                ProductComparison(
                    productName = name,
                    offers = groupOffers.sortedBy { it.price },
                    cheapest = groupOffers.minByOrNull { it.price },
                    mostExpensive = groupOffers.maxByOrNull { it.price }
                )
            }.sortedBy { it.productName }
        }

        withContext(Dispatchers.IO) { saveComparisons(comparisons) }
        return comparisons
    }

    suspend fun compareSelectedOffers(selectedOffers: List<ProductOffer>): List<ProductComparison> {
        val comparisons = withContext(Dispatchers.Default) {
            val grouped = selectedOffers.groupBy { it.name }
            grouped.map { (name, offers) ->
                ProductComparison(
                    productName = name,
                    offers = offers.sortedBy { it.price },
                    cheapest = offers.minByOrNull { it.price },
                    mostExpensive = offers.maxByOrNull { it.price }
                )
            }.sortedBy { it.productName }
        }

        withContext(Dispatchers.IO) { saveComparisons(comparisons) }
        return comparisons
    }

    suspend fun searchAndCompare(query: String, selectedMarkets: Set<String>): List<ProductComparison> {
        val offers = searchOffers(query, selectedMarkets)
        return compareProducts(offers, emptySet())
    }

    fun observeSearchesPerDay() = historyDao.observeSearchesPerDay()
    fun observeAverageSavingsPerDay(): Flow<List<DailySavingsPoint>> = historyDao.observeAverageSavingsPerDay()
    fun observeTopSearchedProducts(): Flow<List<ProductSearchCount>> {
        val syntheticFlow = flow {
            emit(syntheticStats.topProducts(limit = 120))
        }.flowOn(Dispatchers.Default)

        return combine(
            historyDao.observeTopSearchedProducts(limit = 120),
            syntheticFlow
        ) { real, synthetic ->
            mergeTopProducts(real, synthetic, limit = 12)
        }
    }

    fun observeProductSearchTrend(product: String): Flow<List<ProductSearchTrendPoint>> {
        val synthetic = if (product.isBlank()) emptyList() else syntheticStats.productTrend(product)
        return historyDao.observeProductSearchTrend(product).map { real ->
            mergeTrend(synthetic, real)
        }
    }

    fun observeProductSearchHours(product: String): Flow<List<ProductSearchHourPoint>> {
        val synthetic = if (product.isBlank()) emptyList() else syntheticStats.productHours(product)
        return historyDao.observeProductSearchHours(product).map { real ->
            mergeHours(synthetic, real)
        }
    }

    fun observeRecentQueries(): Flow<List<String>> = historyDao.observeRecentQueries()

    suspend fun getTotalSearches(): Int = historyDao.getSearchCount()

    private suspend fun saveSearchRecord(
        query: String,
        supermarkets: Int,
        offerCount: Int
    ) {
        val now = System.currentTimeMillis()
        historyDao.insertSearchRecord(
            SearchRecordEntity(
                query = query,
                timestamp = now,
                supermarkets = supermarkets,
                resultCount = offerCount
            )
        )
    }

    private suspend fun saveComparisons(comparisons: List<ProductComparison>) {
        val now = System.currentTimeMillis()
        val rows = comparisons.mapNotNull { comparison ->
            val cheapest = comparison.cheapest?.price ?: return@mapNotNull null
            val expensive = comparison.mostExpensive?.price ?: return@mapNotNull null
            ComparisonRecordEntity(
                productName = comparison.productName,
                cheapestPrice = cheapest,
                mostExpensivePrice = expensive,
                difference = expensive - cheapest,
                timestamp = now
            )
        }
        if (rows.isNotEmpty()) {
            historyDao.insertComparisonRecords(rows)
        }
    }

    private suspend fun fetchOffers(query: String, selectedMarkets: Set<String>): List<ProductOffer> {
        val localOffers = localDataSource.search(query)
        val remoteOffers = withTimeoutOrNull(1_500) {
            remoteDataSource.search(query)
        } ?: emptyList()
        val allOffers = (localOffers + remoteOffers)
            .distinctBy { "${it.supermarket}_${it.name}_${it.price}" }

        return allOffers.filter { offer ->
            selectedMarkets.isEmpty() || offer.supermarket in selectedMarkets
        }
    }

    private fun mergeTopProducts(
        real: List<ProductSearchCount>,
        synthetic: List<ProductSearchCount>,
        limit: Int
    ): List<ProductSearchCount> {
        data class ProductAgg(var displayName: String, var count: Int)
        val byProduct = linkedMapOf<String, ProductAgg>()
        synthetic.forEach { item ->
            val key = item.product.lowercase()
            byProduct[key] = ProductAgg(displayName = item.product, count = item.count)
        }
        real.forEach { item ->
            val key = item.product.lowercase()
            val existing = byProduct[key]
            if (existing == null) {
                byProduct[key] = ProductAgg(displayName = item.product, count = item.count)
            } else {
                existing.count += item.count
                // Prefer nicer display casing from real data when available.
                if (existing.displayName == existing.displayName.lowercase() && item.product != item.product.lowercase()) {
                    existing.displayName = item.product
                }
            }
        }
        return byProduct.entries
            .sortedWith(
                compareByDescending<Map.Entry<String, ProductAgg>> { it.value.count }
                    .thenBy { it.value.displayName.lowercase() }
            )
            .take(limit)
            .map { ProductSearchCount(product = it.value.displayName, count = it.value.count) }
    }

    private fun mergeTrend(
        synthetic: List<ProductSearchTrendPoint>,
        real: List<ProductSearchTrendPoint>
    ): List<ProductSearchTrendPoint> {
        val merged = linkedMapOf<String, Int>()
        synthetic.forEach { merged[it.day] = it.count }
        real.forEach { row -> merged[row.day] = (merged[row.day] ?: 0) + row.count }
        return merged.entries.map { ProductSearchTrendPoint(day = it.key, count = it.value) }
    }

    private fun mergeHours(
        synthetic: List<ProductSearchHourPoint>,
        real: List<ProductSearchHourPoint>
    ): List<ProductSearchHourPoint> {
        val merged = linkedMapOf<String, Int>()
        synthetic.forEach { merged[it.hour] = it.count }
        real.forEach { row -> merged[row.hour] = (merged[row.hour] ?: 0) + row.count }
        return merged.entries
            .sortedBy { it.key }
            .map { ProductSearchHourPoint(hour = it.key, count = it.value) }
    }

    private class SyntheticStatsGenerator(
        private val categoriesProvider: () -> Map<String, String>
    ) {
        private val productCategories by lazy(LazyThreadSafetyMode.NONE) {
            categoriesProvider()
        }
        private val lastThreeYearsDays = 365 * 3
        private val endDate = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        fun topProducts(limit: Int): List<ProductSearchCount> {
            return productCategories.entries
                .map { (name, category) ->
                    val base = popularityScore(name, category).coerceAtLeast(6).toDouble()
                    val demandTier = demandTierForProduct(name, category)
                    val categoryMix = when (category) {
                        "Fruits", "Vegetables" -> 1.16
                        "Dairy", "Bakery", "Pantry" -> 1.10
                        "Beverages", "Snacks", "Frozen", "Ready Meals" -> 1.03
                        "Organic", "Vegan", "Gluten Free" -> 0.95
                        "International", "Gourmet" -> 0.84
                        else -> 0.90
                    }
                    val seasonalityWeight = categorySeasonalityWeight(category)
                    val noise = 0.88 + (stableNoise(name, 91) * 0.28)
                    ProductSearchCount(
                        product = name,
                        count = (base * 410 * demandTier * categoryMix * seasonalityWeight * noise)
                            .roundToInt()
                            .coerceAtLeast(80)
                    )
                }
                .sortedWith(compareByDescending<ProductSearchCount> { it.count }.thenBy { it.product })
                .take(limit)
        }

        fun productTrend(product: String): List<ProductSearchTrendPoint> {
            val key = product.lowercase()
            val category = categoryForProduct(key)
            val base = popularityScore(key, category).coerceAtLeast(6).toDouble()
            val demandTier = demandTierForProduct(key, category)
            val output = ArrayList<ProductSearchTrendPoint>(lastThreeYearsDays)
            val cursor = endDate.clone() as Calendar
            cursor.add(Calendar.DAY_OF_YEAR, -(lastThreeYearsDays - 1))

            repeat(lastThreeYearsDays) {
                val dayKey = dayKey(cursor)
                val month = cursor.get(Calendar.MONTH) + 1
                val dayOfMonth = cursor.get(Calendar.DAY_OF_MONTH)
                val weekday = weekdayIndex(cursor)
                val timelineProgress = (it + 1).toDouble() / lastThreeYearsDays
                val seasonal = seasonalFactor(category, month)
                val weekly = weekdayFactor(category, weekday)
                val trend = 0.94 + (timelineProgress * 0.16)
                val payday = when (dayOfMonth) {
                    in 1..4 -> 1.06
                    in 24..28 -> 1.11
                    else -> 1.0
                }
                val holiday = holidayFactor(month, dayOfMonth)
                val promo = promoFactor(key, dayKey)
                val stockDip = stockDipFactor(key, dayKey)
                val weatherPulse = weatherPulseFactor(category, month, dayKey)
                val noise = 0.92 + (stableNoise("$key-$dayKey", 17) * 0.16)
                val value = base * 1.14 * demandTier * seasonal * weekly * trend * payday * holiday * promo * stockDip * weatherPulse * noise
                val count = value.roundToInt().coerceAtLeast(0)
                output += ProductSearchTrendPoint(day = dayKey, count = count)
                cursor.add(Calendar.DAY_OF_YEAR, 1)
            }
            return output
        }

        fun productHours(product: String): List<ProductSearchHourPoint> {
            val key = product.lowercase()
            val category = categoryForProduct(key)
            val base = popularityScore(key, category).coerceAtLeast(6).toDouble()
            val demandTier = demandTierForProduct(key, category)
            val profile = smoothHourProfile(buildCategoryHourProfile(category))
            return (0..23).map { hour ->
                val profileWeight = profile[hour]
                val itemTilt = itemHourTilt(key, hour)
                val noise = 0.97 + (stableNoise("$key-$hour", 43) * 0.06)
                val count = (base * demandTier * profileWeight * itemTilt * noise).roundToInt().coerceAtLeast(0)
                ProductSearchHourPoint(hour = "%02d:00".format(hour), count = count)
            }
        }

        private fun categoryForProduct(productLower: String): String {
            return productCategories.entries
                .firstOrNull { it.key.equals(productLower, ignoreCase = true) }
                ?.value
                ?: guessCategory(productLower)
        }

        private fun guessCategory(productLower: String): String {
            return when {
                productLower.contains("juice") || productLower.contains("water") || productLower.contains("cola") -> "Beverages"
                productLower.contains("pizza") || productLower.contains("frozen") -> "Frozen"
                productLower.contains("bread") || productLower.contains("bagel") -> "Bakery"
                productLower.contains("milk") || productLower.contains("yogurt") || productLower.contains("cheese") -> "Dairy"
                productLower.contains("chicken") || productLower.contains("salmon") || productLower.contains("beef") -> "Meat & Fish"
                productLower.contains("snack") || productLower.contains("chips") || productLower.contains("bar") -> "Snacks"
                else -> "Pantry"
            }
        }

        private fun seasonalFactor(category: String, month: Int): Double {
            return when (category) {
                "Beverages", "Frozen", "Fruits" -> when (month) {
                    in 6..8 -> 1.22
                    in 11..12 -> 1.06
                    else -> 1.0
                }
                "Ready Meals", "Pantry", "Bakery", "Dairy" -> when (month) {
                    in 11..2 -> 1.14
                    in 6..8 -> 0.95
                    else -> 1.0
                }
                "Organic", "Vegan", "Gluten Free" -> when (month) {
                    1 -> 1.14
                    in 4..6 -> 1.05
                    else -> 1.0
                }
                "Snacks", "Gourmet", "International" -> when (month) {
                    in 11..12 -> 1.20
                    in 5..8 -> 1.08
                    else -> 1.0
                }
                else -> 1.0
            }
        }

        private fun weekdayFactor(category: String, weekday: Int): Double {
            // Monday=0 ... Sunday=6
            val base = when (weekday) {
                5 -> 1.08
                6 -> 1.12
                4 -> 1.05
                else -> 0.96
            }
            val categoryTilt = when (category) {
                "Bakery", "Breakfast" -> if (weekday in 0..4) 1.06 else 0.98
                "Ready Meals", "Snacks" -> if (weekday >= 4) 1.08 else 0.96
                else -> 1.0
            }
            return base * categoryTilt
        }

        private fun holidayFactor(month: Int, day: Int): Double {
            return when {
                month == 12 && day in 20..31 -> 1.18
                month == 1 && day in 1..5 -> 1.10
                month == 11 && day >= 20 -> 1.08
                month in 7..8 && day in 1..10 -> 0.96
                else -> 1.0
            }
        }

        private fun promoFactor(productKey: String, dayKey: String): Double {
            val n = stableNoise("promo-$productKey-$dayKey", 301)
            return when {
                n > 0.995 -> 1.40
                n > 0.986 -> 1.22
                else -> 1.0
            }
        }

        private fun stockDipFactor(productKey: String, dayKey: String): Double {
            val n = stableNoise("stock-$productKey-$dayKey", 401)
            return if (n < 0.010) 0.70 else 1.0
        }

        private fun weatherPulseFactor(category: String, month: Int, dayKey: String): Double {
            val n = stableNoise("weather-$dayKey", 509)
            return when (category) {
                "Beverages", "Frozen" -> when {
                    month in 6..8 && n > 0.72 -> 1.06
                    month in 11..2 && n < 0.20 -> 0.97
                    else -> 1.0
                }
                "Ready Meals", "Bakery" -> when {
                    month in 10..2 && n > 0.74 -> 1.04
                    else -> 1.0
                }
                else -> 1.0
            }
        }

        private fun buildCategoryHourProfile(category: String): DoubleArray {
            // Base grocery traffic: low at night, moderate day, highest late afternoon/evening.
            val profile = DoubleArray(24) { hour ->
                when (hour) {
                    in 0..5 -> 0.18
                    in 6..8 -> 0.92
                    in 9..11 -> 0.98
                    in 12..14 -> 1.05
                    in 15..17 -> 1.18
                    in 18..20 -> 1.25
                    in 21..23 -> 0.72
                    else -> 1.0
                }
            }

            when (category) {
                "Breakfast", "Bakery", "Dairy" -> {
                    for (h in 6..10) profile[h] *= 1.24
                    for (h in 18..20) profile[h] *= 0.92
                }
                "Ready Meals", "Frozen" -> {
                    for (h in 17..21) profile[h] *= 1.30
                    for (h in 7..10) profile[h] *= 0.90
                }
                "Beverages" -> {
                    for (h in 11..20) profile[h] *= 1.16
                    for (h in 6..8) profile[h] *= 0.92
                }
                "Snacks" -> {
                    for (h in 15..22) profile[h] *= 1.22
                    for (h in 6..9) profile[h] *= 0.88
                }
                "Fruits", "Vegetables" -> {
                    for (h in 10..19) profile[h] *= 1.10
                }
            }
            return profile
        }

        private fun smoothHourProfile(profile: DoubleArray): DoubleArray {
            // 3-point smoothing to avoid unrealistic hour-to-hour jumps.
            return DoubleArray(profile.size) { idx ->
                val prev = if (idx == 0) profile[idx] else profile[idx - 1]
                val curr = profile[idx]
                val next = if (idx == profile.lastIndex) profile[idx] else profile[idx + 1]
                (prev * 0.24) + (curr * 0.52) + (next * 0.24)
            }
        }

        private fun itemHourTilt(productKey: String, hour: Int): Double {
            // Small per-item tilt so products do not share the exact same curve.
            val seed = stableNoise("tilt-$productKey", 611)
            return when {
                seed > 0.66 && hour in 6..11 -> 1.05
                seed < 0.33 && hour in 17..21 -> 1.06
                else -> 1.0
            }
        }

        private fun popularityScore(name: String, category: String): Int {
            val categoryBase = when (category) {
                "Fruits", "Vegetables" -> 22
                "Dairy", "Bakery", "Pantry" -> 20
                "Beverages", "Snacks", "Frozen", "Ready Meals" -> 18
                "Meat & Fish", "Breakfast" -> 16
                "Organic", "Vegan", "Gluten Free", "International", "Gourmet" -> 12
                "Household", "Personal Care" -> 10
                "Baby", "Pet" -> 8
                else -> 9
            }
            val boost = when {
                name.contains("premium", ignoreCase = true) -> 0
                name.contains("family", ignoreCase = true) -> 3
                name.contains("daily", ignoreCase = true) -> 2
                name.contains("classic", ignoreCase = true) -> 2
                else -> 1
            }
            val noise = (stableNoise(name, 7) * 10).roundToInt()
            return (categoryBase + boost + noise).coerceAtLeast(4)
        }

        private fun demandTierForProduct(product: String, category: String): Double {
            val name = product.lowercase()
            val stapleTerms = listOf("milk", "bread", "water", "banana", "apple", "rice", "egg", "pasta")
            val nicheTerms = listOf("premium", "gourmet", "limited", "artisan", "import", "keto")
            return when {
                stapleTerms.any { name.contains(it) } -> 1.30
                nicheTerms.any { name.contains(it) } -> 0.84
                category in setOf("Fruits", "Vegetables", "Dairy", "Bakery", "Pantry") -> 1.10
                category in setOf("International", "Gourmet", "Pet", "Baby") -> 0.88
                else -> 1.0
            }
        }

        private fun categorySeasonalityWeight(category: String): Double {
            return when (category) {
                "Beverages", "Frozen", "Fruits" -> 1.06
                "Bakery", "Pantry", "Dairy", "Vegetables" -> 1.00
                "International", "Gourmet", "Organic", "Vegan", "Gluten Free" -> 0.96
                else -> 0.98
            }
        }

        private fun weekdayIndex(calendar: Calendar): Int {
            return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 0
                Calendar.TUESDAY -> 1
                Calendar.WEDNESDAY -> 2
                Calendar.THURSDAY -> 3
                Calendar.FRIDAY -> 4
                Calendar.SATURDAY -> 5
                Calendar.SUNDAY -> 6
                else -> 0
            }
        }

        private fun dayKey(calendar: Calendar): String {
            val y = calendar.get(Calendar.YEAR)
            val m = calendar.get(Calendar.MONTH) + 1
            val d = calendar.get(Calendar.DAY_OF_MONTH)
            return "%04d-%02d-%02d".format(y, m, d)
        }

        private fun stableNoise(seed: String, salt: Int): Double {
            val value = (seed.hashCode() * 31 + salt).absoluteValue % 10_000
            return value / 10_000.0
        }
    }
}
