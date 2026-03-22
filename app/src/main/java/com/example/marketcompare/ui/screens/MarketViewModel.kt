package com.example.marketcompare.ui.screens

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.marketcompare.data.auth.AuthRepository
import com.example.marketcompare.data.auth.UserSession
import com.example.marketcompare.data.model.DailySavingsPoint
import com.example.marketcompare.data.model.ProductComparison
import com.example.marketcompare.data.model.ProductOffer
import com.example.marketcompare.data.model.ProductSearchCount
import com.example.marketcompare.data.model.ProductSearchHourPoint
import com.example.marketcompare.data.model.ProductSearchTrendPoint
import com.example.marketcompare.data.repo.MarketRepository
import com.example.marketcompare.data.sync.CloudSyncRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

enum class OfferSortOption(val label: String) {
    RELEVANCE("Relevance"),
    PRICE_LOW_HIGH("Price: low to high"),
    PRICE_HIGH_LOW("Price: high to low"),
    NAME_A_Z("Name: A to Z"),
    NAME_Z_A("Name: Z to A"),
    MARKET_A_Z("Market: A to Z")
}

data class ActiveFilterTag(
    val id: String,
    val label: String
)

data class ProductResultItem(
    val name: String,
    val category: String,
    val bestPrice: Double,
    val supermarkets: Int,
    val sampleOffer: ProductOffer,
    val matchLevel: String
)

@OptIn(ExperimentalCoroutinesApi::class)
class MarketViewModel(
    private val repository: MarketRepository,
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {
    val maxVisibleSearchResults = 140
    private val resultsPerPage = 10
    val supermarkets: List<String> = repository.availableSupermarkets()
    val analyticsCatalogProducts: List<String> = repository.analyticsCatalogProducts()
    val selectedMarkets = mutableStateListOf<String>()
    val selectedOfferKeys = mutableStateListOf<String>()
    private val adminCreatedOffers = mutableStateListOf<ProductOffer>()

    var query by mutableStateOf("")
    var searchOffers by mutableStateOf<List<ProductOffer>>(emptyList())
    var comparisonResults by mutableStateOf<List<ProductComparison>>(emptyList())
    var hasSearched by mutableStateOf(false)
    var isSearching by mutableStateOf(false)
    var isComparing by mutableStateOf(false)
    var searchSuggestions by mutableStateOf<List<String>>(emptyList())
    var selectedProductDetailsName by mutableStateOf<String?>(null)
    var selectedProductDetailsOffers by mutableStateOf<List<ProductOffer>>(emptyList())
    var selectedCategory by mutableStateOf<String?>(null)
    var selectedSubcategory by mutableStateOf<String?>(null)
    var selectedSpecialType by mutableStateOf<String?>(null)
    var minPriceInput by mutableStateOf("")
    var maxPriceInput by mutableStateOf("")
    var selectedSort by mutableStateOf(OfferSortOption.RELEVANCE)
    var selectedMatchLevel by mutableStateOf<String?>(null)
    var username by mutableStateOf("")
    var password by mutableStateOf("")
    var authError by mutableStateOf<String?>(null)
    var syncMessage by mutableStateOf<String?>(null)
    var selectedAnalyticsProduct by mutableStateOf("")
    var adminToolMessage by mutableStateOf<String?>(null)

    private val selectedProductFlow = MutableStateFlow("")
    private var suggestionJob: Job? = null
    private var searchJob: Job? = null
    private var searchWatchdogJob: Job? = null
    private var latestSearchToken = 0
    private var activeSearchCount = 0
    var currentResultsPage by mutableStateOf(0)
        private set

    val savingsPerDay: StateFlow<List<DailySavingsPoint>> = repository
        .observeAverageSavingsPerDay()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val topSearchedProducts: StateFlow<List<ProductSearchCount>> = repository
        .observeTopSearchedProducts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedProductTrend: StateFlow<List<ProductSearchTrendPoint>> = selectedProductFlow
        .flatMapLatest { product ->
            repository.observeProductSearchTrend(product)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedProductHours: StateFlow<List<ProductSearchHourPoint>> = selectedProductFlow
        .flatMapLatest { product ->
            repository.observeProductSearchHours(product)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recentQueries: StateFlow<List<String>> = repository
        .observeRecentQueries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userSession: StateFlow<UserSession?> = authRepository
        .observeSession()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun toggleMarket(market: String) {
        if (market in selectedMarkets) {
            selectedMarkets.remove(market)
        } else {
            selectedMarkets.add(market)
        }
    }

    fun search() {
        val currentQuery = query.trim()
        if (currentQuery.isBlank()) {
            isSearching = false
            searchOffers = emptyList()
            currentResultsPage = 0
            return
        }
        currentResultsPage = 0
        runSearch(currentQuery)
    }

    private fun runSearch(searchQuery: String) {
        val currentQuery = searchQuery.trim()
        if (currentQuery.isBlank()) {
            isSearching = false
            searchOffers = emptyList()
            return
        }
        searchJob?.cancel()
        searchWatchdogJob?.cancel()
        val searchToken = ++latestSearchToken
        searchWatchdogJob = viewModelScope.launch {
            delay(20_000)
            if (searchToken == latestSearchToken && isSearching) {
                // Fail-safe so UI never remains locked forever.
                activeSearchCount = 0
                isSearching = false
            }
        }
        searchJob = viewModelScope.launch {
            activeSearchCount += 1
            isSearching = true
            try {
                val result = repository.searchOffers(currentQuery, emptySet())
                val adminMatches = searchAdminCreatedOffers(currentQuery)
                if (searchToken == latestSearchToken) {
                    searchOffers = (result + adminMatches)
                        .distinctBy { offerKey(it) }
                    comparisonResults = emptyList()
                    selectedOfferKeys.clear()
                    hasSearched = true
                }
            } finally {
                activeSearchCount = (activeSearchCount - 1).coerceAtLeast(0)
                isSearching = activeSearchCount > 0
                searchWatchdogJob?.cancel()
            }
        }
    }

    fun onQueryChange(newValue: String) {
        query = newValue
        currentResultsPage = 0
        suggestionJob?.cancel()
        if (newValue.isBlank()) {
            searchSuggestions = emptyList()
            // Keep results visible when user clears/edits input to avoid abrupt UI jump.
            // A new explicit search run controls when results should refresh.
            hasSearched = searchOffers.isNotEmpty()
            return
        }
        if (!hasSearched && searchOffers.isNotEmpty()) {
            hasSearched = true
        }
        suggestionJob = viewModelScope.launch {
            delay(120)
            val catalogSuggestions = repository.suggestProducts(newValue, limit = 12)
            val adminSuggestions = adminCreatedOffers
                .map { it.name }
                .distinct()
                .filter { it.startsWith(newValue, ignoreCase = true) || it.contains(newValue, ignoreCase = true) }
                .take(6)
            val historySuggestions = recentQueries.value
                .filter { it.startsWith(newValue, ignoreCase = true) }
                .take(2)
            searchSuggestions = (adminSuggestions + catalogSuggestions + historySuggestions).distinct().take(12)
        }
    }

    fun addAdminItemSmart(
        name: String,
        brand: String,
        category: String,
        subcategory: String,
        packageAmount: String,
        basePriceInput: String,
        selectedSupermarkets: Set<String>,
        specialType: String,
        cloneCount: Int
    ): Boolean {
        val safeName = name.trim()
        val safeCategory = category.trim()
        val safeSubcategory = subcategory.trim()
        val safePackage = packageAmount.trim().lowercase()
        val parsedPrice = basePriceInput.trim().replace(",", ".").toDoubleOrNull()
        if (safeName.length < 3) {
            adminToolMessage = "Name too short."
            return false
        }
        if (parsedPrice == null || parsedPrice <= 0.0) {
            adminToolMessage = "Price must be a positive number."
            return false
        }
        if (safeCategory.isBlank() || safeSubcategory.isBlank()) {
            adminToolMessage = "Category and subcategory are required."
            return false
        }
        if (safePackage.isBlank()) {
            adminToolMessage = "Package amount is required (example: 500g, 1l, 6stk)."
            return false
        }

        val markets = if (selectedSupermarkets.isEmpty()) {
            supermarkets.take(3)
        } else {
            selectedSupermarkets.toList()
        }
        val safeCloneCount = cloneCount.coerceIn(0, 3)
        val cloneLabels = listOf("Daily", "Classic", "Select")
        val type = if (specialType.isBlank()) "Regular" else specialType

        val generated = mutableListOf<ProductOffer>()
        markets.forEachIndexed { marketIndex, market ->
            val marketFactor = 1.0 + ((market.hashCode().absoluteValue % 7) - 3) * 0.01
            val baseKeywords = buildAdminKeywords(safeName, safeCategory, safeSubcategory, type)
            val baseOffer = ProductOffer(
                supermarket = market,
                name = safeName,
                price = ((parsedPrice * marketFactor) * 100.0).toInt() / 100.0,
                category = safeCategory,
                keywords = baseKeywords,
                brand = brand.ifBlank { "CraftLine" },
                specialType = type,
                packageAmount = safePackage,
                weeklySales = 70 + ((safeName.hashCode() + market.hashCode()).absoluteValue % 140)
            )
            generated += baseOffer

            repeat(safeCloneCount) { idx ->
                val cloneIndex = idx + 1
                val cloneFactor = when (cloneIndex) {
                    1 -> 0.95
                    2 -> 1.05
                    else -> 1.12
                }
                val cloneName = "${cloneLabels[idx]} $safeName"
                generated += baseOffer.copy(
                    name = cloneName,
                    price = ((baseOffer.price * cloneFactor) * 100.0).toInt() / 100.0,
                    brand = if (baseOffer.brand == "CraftLine") "CraftLine ${cloneLabels[idx]}" else "${baseOffer.brand} ${cloneLabels[idx]}",
                    keywords = buildAdminKeywords(cloneName, safeCategory, safeSubcategory, type),
                    weeklySales = (baseOffer.weeklySales - 8 + (marketIndex * 2) + cloneIndex * 3).coerceAtLeast(20)
                )
            }
        }

        adminCreatedOffers.removeAll { it.name.equals(safeName, ignoreCase = true) || it.name.endsWith(" $safeName", ignoreCase = true) }
        adminCreatedOffers.addAll(generated.distinctBy { offerKey(it) })
        adminToolMessage = "Added ${generated.size} offers across ${markets.size} market(s)."
        return true
    }

    fun useSuggestionAndSearch(suggestion: String) {
        suggestionJob?.cancel()
        query = suggestion
        searchSuggestions = emptyList()
        search()
    }

    fun offerKey(offer: ProductOffer): String {
        return "${offer.supermarket}|${offer.name}|${offer.price}"
    }

    fun toggleSelectedOffer(offer: ProductOffer) {
        val key = offerKey(offer)
        if (key in selectedOfferKeys) {
            selectedOfferKeys.remove(key)
        } else {
            selectedOfferKeys.add(key)
        }
    }

    fun compareSelectedProducts() {
        viewModelScope.launch {
            isComparing = true
            try {
                comparisonResults = repository.compareSelectedOffers(selectedOffers())
            } finally {
                isComparing = false
            }
        }
    }

    fun selectedOffers(): List<ProductOffer> {
        return searchOffers
            .filter { offerKey(it) in selectedOfferKeys }
            .sortedWith(compareBy<ProductOffer> { it.name }.thenBy { it.price })
    }

    fun openProductDetails(productName: String) {
        selectedProductDetailsName = productName
        selectedProductDetailsOffers = searchOffers
            .filter { it.name == productName }
            .sortedBy { it.price }
    }

    fun closeProductDetails() {
        selectedProductDetailsName = null
        selectedProductDetailsOffers = emptyList()
    }

    fun availableCategories(): List<String> {
        return filteredByMarket(searchOffers)
            .map { it.category }
            .distinct()
            .sorted()
    }

    fun availableSubcategories(): List<String> {
        val scoped = filteredByCategory(filteredByMarket(searchOffers))
        return scoped
            .mapNotNull { offer -> offer.keywords.firstOrNull { it.startsWith("subcategory:") }?.removePrefix("subcategory:") }
            .distinct()
            .sorted()
    }

    fun availableSpecialTypes(): List<String> {
        return filteredByCategory(filteredByMarket(searchOffers))
            .map { it.specialType }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
    }

    fun filteredOffers(): List<ProductOffer> {
        val sorted = filteredOffersUncapped()
        return if (sorted.size > maxVisibleSearchResults) {
            sorted.take(maxVisibleSearchResults)
        } else {
            sorted
        }
    }

    fun productResults(): List<ProductResultItem> {
        val grouped = filteredOffersUncapped().groupBy { it.name }
        return grouped.mapNotNull { (name, offers) ->
            val best = offers.minByOrNull { it.price } ?: return@mapNotNull null
            ProductResultItem(
                name = name,
                category = best.category,
                bestPrice = best.price,
                supermarkets = offers.map { it.supermarket }.distinct().size,
                sampleOffer = best,
                matchLevel = highestMatchLevel(offers)
            )
        }.let { items ->
            when (selectedSort) {
                OfferSortOption.RELEVANCE -> items.sortedWith(
                    compareByDescending<ProductResultItem> { matchLevelRank(it.matchLevel) }
                        .thenBy { it.bestPrice }
                        .thenBy { it.name.lowercase() }
                )
                OfferSortOption.PRICE_LOW_HIGH -> items.sortedBy { it.bestPrice }
                OfferSortOption.PRICE_HIGH_LOW -> items.sortedByDescending { it.bestPrice }
                OfferSortOption.NAME_A_Z -> items.sortedBy { it.name.lowercase() }
                OfferSortOption.NAME_Z_A -> items.sortedByDescending { it.name.lowercase() }
                OfferSortOption.MARKET_A_Z -> items.sortedWith(
                    compareByDescending<ProductResultItem> { it.supermarkets }
                        .thenBy { it.name.lowercase() }
                )
            }
        }
    }

    private fun filteredOffersUncapped(): List<ProductOffer> {
        var offers = searchOffers
        offers = filteredByMarket(offers)
        offers = filteredByCategory(offers)
        offers = filteredBySubcategory(offers)
        offers = filteredByPrice(offers)
        offers = filteredBySpecialType(offers)
        offers = filteredByMatchLevel(offers)

        return when (selectedSort) {
            OfferSortOption.RELEVANCE -> offers
            OfferSortOption.PRICE_LOW_HIGH -> offers.sortedBy { it.price }
            OfferSortOption.PRICE_HIGH_LOW -> offers.sortedByDescending { it.price }
            OfferSortOption.NAME_A_Z -> offers.sortedBy { it.name.lowercase() }
            OfferSortOption.NAME_Z_A -> offers.sortedByDescending { it.name.lowercase() }
            OfferSortOption.MARKET_A_Z -> offers.sortedBy { it.supermarket.lowercase() }
        }
    }

    fun activeFilterTags(): List<ActiveFilterTag> {
        val tags = mutableListOf<ActiveFilterTag>()
        selectedMarkets.forEach { market ->
            tags.add(ActiveFilterTag(id = "market:$market", label = "Market: $market"))
        }
        selectedCategory?.let { category ->
            tags.add(ActiveFilterTag(id = "category", label = "Category: $category"))
        }
        selectedSubcategory?.let { subcategory ->
            tags.add(ActiveFilterTag(id = "subcategory", label = "Subcategory: $subcategory"))
        }
        selectedSpecialType?.let { specialType ->
            tags.add(ActiveFilterTag(id = "specialType", label = "Type: $specialType"))
        }
        minPriceInput.toDoubleOrNull()?.let { min ->
            tags.add(ActiveFilterTag(id = "minPrice", label = "Min: €${"%.2f".format(min)}"))
        }
        maxPriceInput.toDoubleOrNull()?.let { max ->
            tags.add(ActiveFilterTag(id = "maxPrice", label = "Max: €${"%.2f".format(max)}"))
        }
        if (selectedSort != OfferSortOption.RELEVANCE) {
            tags.add(ActiveFilterTag(id = "sort", label = selectedSort.label))
        }
        selectedMatchLevel?.let { level ->
            tags.add(ActiveFilterTag(id = "matchLevel", label = "Match: $level"))
        }
        return tags
    }

    fun removeFilter(tagId: String) {
        when {
            tagId.startsWith("market:") -> {
                val market = tagId.removePrefix("market:")
                selectedMarkets.remove(market)
            }
            tagId == "category" -> {
                selectedCategory = null
                selectedSubcategory = null
            }
            tagId == "subcategory" -> selectedSubcategory = null
            tagId == "specialType" -> selectedSpecialType = null
            tagId == "minPrice" -> minPriceInput = ""
            tagId == "maxPrice" -> maxPriceInput = ""
            tagId == "sort" -> selectedSort = OfferSortOption.RELEVANCE
            tagId == "matchLevel" -> selectedMatchLevel = null
        }
    }

    fun clearAllFilters() {
        selectedMarkets.clear()
        selectedCategory = null
        selectedSubcategory = null
        selectedSpecialType = null
        minPriceInput = ""
        maxPriceInput = ""
        selectedSort = OfferSortOption.RELEVANCE
        selectedMatchLevel = null
        currentResultsPage = 0
    }

    fun totalResultPages(results: List<ProductResultItem>): Int {
        if (results.isEmpty()) return 1
        return ((results.size - 1) / resultsPerPage) + 1
    }

    fun currentPageIndex(results: List<ProductResultItem>): Int {
        val lastIndex = totalResultPages(results) - 1
        return currentResultsPage.coerceIn(0, lastIndex.coerceAtLeast(0))
    }

    fun pagedProductResults(results: List<ProductResultItem>): List<ProductResultItem> {
        if (results.isEmpty()) return emptyList()
        val safePage = currentPageIndex(results)
        val start = safePage * resultsPerPage
        return results.drop(start).take(resultsPerPage)
    }

    fun goToNextPage(results: List<ProductResultItem>) {
        val lastIndex = totalResultPages(results) - 1
        currentResultsPage = (currentResultsPage + 1).coerceAtMost(lastIndex.coerceAtLeast(0))
    }

    fun goToPreviousPage() {
        currentResultsPage = (currentResultsPage - 1).coerceAtLeast(0)
    }

    fun goToPage(pageIndex: Int, results: List<ProductResultItem>) {
        val lastIndex = totalResultPages(results) - 1
        currentResultsPage = pageIndex.coerceIn(0, lastIndex.coerceAtLeast(0))
    }

    fun selectAnalyticsProduct(product: String) {
        selectedAnalyticsProduct = product
        selectedProductFlow.value = product
    }

    fun signIn() {
        viewModelScope.launch {
            val success = authRepository.signIn(username, password)
            authError = if (success) {
                null
            } else {
                "Invalid credentials. Use user/passwort or admin/passwort."
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            syncMessage = null
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            val session = userSession.value
            if (session == null) {
                syncMessage = "Sign in to enable cloud sync."
                return@launch
            }
            val success = cloudSyncRepository.syncPendingData(session.token)
            syncMessage = if (success) "Cloud sync complete." else "Sync failed. Will retry in background."
        }
    }

    private fun filteredByMarket(offers: List<ProductOffer>): List<ProductOffer> {
        if (selectedMarkets.isEmpty()) return offers
        return offers.filter { it.supermarket in selectedMarkets }
    }

    private fun filteredByCategory(offers: List<ProductOffer>): List<ProductOffer> {
        val category = selectedCategory ?: return offers
        return offers.filter { it.category == category }
    }

    private fun filteredBySubcategory(offers: List<ProductOffer>): List<ProductOffer> {
        val subcategory = selectedSubcategory ?: return offers
        return offers.filter { offer ->
            offer.keywords.any { keyword ->
                keyword.startsWith("subcategory:") && keyword.removePrefix("subcategory:") == subcategory
            }
        }
    }

    private fun filteredByPrice(offers: List<ProductOffer>): List<ProductOffer> {
        val min = minPriceInput.toDoubleOrNull()
        val max = maxPriceInput.toDoubleOrNull()
        return offers.filter { offer ->
            (min == null || offer.price >= min) && (max == null || offer.price <= max)
        }
    }

    private fun filteredByMatchLevel(offers: List<ProductOffer>): List<ProductOffer> {
        val selected = selectedMatchLevel ?: return offers
        return offers.filter { offer -> offerMatchLevel(offer) == selected }
    }

    private fun filteredBySpecialType(offers: List<ProductOffer>): List<ProductOffer> {
        val selected = selectedSpecialType ?: return offers
        return offers.filter { offer -> offer.specialType == selected }
    }

    private fun offerMatchLevel(offer: ProductOffer): String {
        val keys = offer.keywords.map { key -> key.lowercase() }.toSet()
        return when {
            "match_level_exact" in keys -> "EXACT"
            "match_level_close" in keys -> "CLOSE"
            "match_level_similar" in keys -> "SIMILAR"
            else -> "SIMILAR"
        }
    }

    private fun highestMatchLevel(offers: List<ProductOffer>): String {
        val keywords = offers.flatMap { it.keywords.map(String::lowercase) }.toSet()
        return when {
            "match_level_exact" in keywords -> "EXACT"
            "match_level_close" in keywords -> "CLOSE"
            "match_level_similar" in keywords -> "SIMILAR"
            else -> "SIMILAR"
        }
    }

    private fun matchLevelRank(level: String): Int {
        return when (level.uppercase()) {
            "EXACT" -> 3
            "CLOSE" -> 2
            "SIMILAR" -> 1
            else -> 0
        }
    }

    private fun buildAdminKeywords(
        name: String,
        category: String,
        subcategory: String,
        specialType: String
    ): List<String> {
        val nameTokens = name.lowercase()
            .replace(Regex("[^a-z0-9äöüß ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.length > 1 }
        return (nameTokens + listOf(
            category.lowercase(),
            "subcategory:${subcategory.lowercase()}",
            specialType.lowercase(),
            "match_level_exact"
        )).distinct()
    }

    private fun searchAdminCreatedOffers(query: String): List<ProductOffer> {
        val q = query.trim().lowercase()
        if (q.length < 2) return emptyList()
        return adminCreatedOffers
            .asSequence()
            .mapNotNull { offer ->
                val score = adminSearchScore(offer, q)
                if (score <= 0) null else offer to score
            }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(240)
            .toList()
    }

    private fun adminSearchScore(offer: ProductOffer, queryLower: String): Int {
        val name = offer.name.lowercase()
        val brand = offer.brand.lowercase()
        val keywordHit = offer.keywords.any { it.contains(queryLower) }
        return when {
            name == queryLower -> 300
            name.startsWith(queryLower) -> 220
            name.contains(queryLower) -> 150
            brand.contains(queryLower) -> 120
            keywordHit -> 100
            else -> 0
        }
    }

    companion object {
        fun factory(
            repository: MarketRepository,
            authRepository: AuthRepository,
            cloudSyncRepository: CloudSyncRepository
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return MarketViewModel(repository, authRepository, cloudSyncRepository) as T
                }
            }
        }
    }
}
