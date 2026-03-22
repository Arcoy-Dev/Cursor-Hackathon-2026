package com.example.marketcompare.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.marketcompare.R
import com.example.marketcompare.data.model.ProductComparison
import com.example.marketcompare.data.model.ProductOffer
import com.example.marketcompare.data.model.ProductSearchHourPoint
import com.example.marketcompare.data.model.ProductSearchTrendPoint
import com.example.marketcompare.ui.theme.AccentBlue
import com.example.marketcompare.ui.theme.AccentPurple
import com.example.marketcompare.ui.theme.BackgroundSoft
import com.example.marketcompare.ui.theme.BorderSoft
import com.example.marketcompare.ui.theme.SuccessGreen
import com.example.marketcompare.ui.theme.TextPrimary
import com.example.marketcompare.ui.theme.TextSecondary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.vector.ImageVector

@Composable
fun MarketAppScreen(viewModel: MarketViewModel) {
    val session by viewModel.userSession.collectAsStateWithLifecycle()
    val activeSession = session
    val isAdmin = activeSession?.role == "admin"
    val tabs = if (isAdmin) {
        listOf("Search", "Compare", "Insights", "Account")
    } else {
        listOf("Search", "Compare", "Account")
    }
    var selectedTab by remember(isAdmin) { mutableIntStateOf(0) }
    var showAdminAddTool by remember(isAdmin) { mutableStateOf(false) }
    if (selectedTab > tabs.lastIndex) {
        selectedTab = tabs.lastIndex
    }
    val goToSearch: () -> Unit = {
        selectedTab = 0
        viewModel.closeProductDetails()
    }

    val detailName = viewModel.selectedProductDetailsName
    if (detailName != null) {
        ProductDetailsScreen(
            viewModel = viewModel,
            productName = detailName,
            offers = viewModel.selectedProductDetailsOffers,
            onClose = { viewModel.closeProductDetails() },
            onGoToSearch = goToSearch
        )
        return
    }

    if (activeSession == null) {
        AccountSyncScreen(viewModel = viewModel)
        return
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFF9FBFF), BackgroundSoft)
                        )
                    )
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                MarkioLogo(
                    onClick = goToSearch,
                    modifier = Modifier.height(72.dp)
                )
            }
        },
        bottomBar = {
            Box {
                NavigationBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .clip(RoundedCornerShape(26.dp)),
                    containerColor = Color.White.copy(alpha = 0.94f),
                    tonalElevation = 0.dp
                ) {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            modifier = Modifier.offset(x = tabHorizontalOffset(tab)),
                            icon = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcon(tab),
                                        contentDescription = tab,
                                        tint = if (selectedTab == index) AccentBlue else TextSecondary,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = tabShortLabel(tab),
                                        fontSize = 9.sp,
                                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                }
                            },
                            label = null,
                            alwaysShowLabel = false
                        )
                    }
                }
                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-12).dp)
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(AccentBlue, AccentPurple)
                                )
                            )
                            .clickable {
                                viewModel.adminToolMessage = null
                                showAdminAddTool = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    ) { padding ->
        when (tabs[selectedTab]) {
            "Search" -> SearchScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )

            "Compare" -> CompareScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )

            "Insights" -> StatsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )

            else -> AccountSyncScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (isAdmin && showAdminAddTool) {
        AdminAddItemToolDialog(
            viewModel = viewModel,
            onDismiss = { showAdminAddTool = false },
            onAdded = {
                showAdminAddTool = false
                selectedTab = 0
            }
        )
    }
}

private fun tabIcon(tab: String): ImageVector {
    return when (tab) {
        "Search" -> Icons.Filled.Search
        "Compare" -> Icons.Filled.ShoppingCart
        "Insights" -> Icons.Filled.Assessment
        else -> Icons.Filled.Person
    }
}

private fun tabShortLabel(tab: String): String {
    return when (tab) {
        "Insights" -> "Stats"
        "Account" -> "Profile"
        else -> tab
    }
}

private fun tabHorizontalOffset(tab: String): Dp {
    return when (tab) {
        "Compare" -> (-12).dp
        "Insights" -> 12.dp
        else -> 0.dp
    }
}

@Composable
private fun SearchScreen(viewModel: MarketViewModel, modifier: Modifier = Modifier) {
    val recentQueries by viewModel.recentQueries.collectAsStateWithLifecycle()
    var showFilters by remember { mutableStateOf(false) }
    val searchScrollState = rememberScrollState()
    val productResults = remember(
        viewModel.searchOffers,
        viewModel.selectedSort,
        viewModel.selectedCategory,
        viewModel.selectedSubcategory,
        viewModel.selectedSpecialType,
        viewModel.selectedMatchLevel,
        viewModel.minPriceInput,
        viewModel.maxPriceInput,
        viewModel.selectedMarkets.toList()
    ) { viewModel.productResults() }
    val totalResultPages = viewModel.totalResultPages(productResults)
    val currentPageIndex = viewModel.currentPageIndex(productResults)
    val pagedProductResults = viewModel.pagedProductResults(productResults)
    val activeFilters = viewModel.activeFilterTags()
    val categories = viewModel.availableCategories()
    val subcategories = viewModel.availableSubcategories()
    val specialTypes = viewModel.availableSpecialTypes()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFF), Color(0xFFEFF3FF))
                )
            )
            .verticalScroll(searchScrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Find products fast across supermarkets and prepare your comparison list.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Card(
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
            border = BorderStroke(1.dp, BorderSoft)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = viewModel.query,
                    onValueChange = { viewModel.onQueryChange(it) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Search product name") }
                )

                if (viewModel.searchSuggestions.isNotEmpty()) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FF))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Suggestions", color = TextSecondary, fontSize = 12.sp)
                            viewModel.searchSuggestions.forEach { suggestion ->
                                TextButton(
                                    onClick = { viewModel.useSuggestionAndSearch(suggestion) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(suggestion, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { showFilters = !showFilters }) {
                        Text(if (showFilters) "Filters ▲" else "Filters ▼")
                    }
                }

                Button(
                    onClick = { viewModel.search() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !viewModel.isSearching && viewModel.query.isNotBlank()
                ) {
                    Text(if (viewModel.isSearching) "Searching..." else "Search products")
                }
            }
        }

        if (showFilters) {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                border = BorderStroke(1.dp, BorderSoft)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Filter System",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Smart filtering and sorting for current search results.",
                        color = TextSecondary
                    )
                    Text("Sort", fontWeight = FontWeight.Medium, color = TextPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OfferSortOption.entries.forEach { option ->
                            FilterChip(
                                selected = option == viewModel.selectedSort,
                                onClick = { viewModel.selectedSort = option },
                                label = { Text(option.label) }
                            )
                        }
                    }

                    Text("Match level", fontWeight = FontWeight.Medium, color = TextPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("EXACT", "CLOSE", "SIMILAR").forEach { level ->
                            FilterChip(
                                selected = viewModel.selectedMatchLevel == level,
                                onClick = {
                                    viewModel.selectedMatchLevel = if (viewModel.selectedMatchLevel == level) {
                                        null
                                    } else {
                                        level
                                    }
                                },
                                label = { Text(level) }
                            )
                        }
                    }

                    Text("Supermarkets", fontWeight = FontWeight.Medium, color = TextPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        viewModel.supermarkets.forEach { market ->
                            FilterChip(
                                selected = market in viewModel.selectedMarkets,
                                onClick = { viewModel.toggleMarket(market) },
                                label = { Text(market) }
                            )
                        }
                    }

                    if (categories.isNotEmpty()) {
                        Text("Category", fontWeight = FontWeight.Medium, color = TextPrimary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categories.forEach { category ->
                                FilterChip(
                                    selected = viewModel.selectedCategory == category,
                                    onClick = {
                                        viewModel.selectedCategory = if (viewModel.selectedCategory == category) {
                                            null
                                        } else {
                                            category
                                        }
                                        viewModel.selectedSubcategory = null
                                    },
                                    label = { Text(category) }
                                )
                            }
                        }
                    }

                    if (subcategories.isNotEmpty()) {
                        Text("Subcategory", fontWeight = FontWeight.Medium, color = TextPrimary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subcategories.forEach { subcategory ->
                                FilterChip(
                                    selected = viewModel.selectedSubcategory == subcategory,
                                    onClick = {
                                        viewModel.selectedSubcategory = if (viewModel.selectedSubcategory == subcategory) {
                                            null
                                        } else {
                                            subcategory
                                        }
                                    },
                                    label = { Text(subcategory.replaceFirstChar { it.uppercase() }) }
                                )
                            }
                        }
                    }

                    if (specialTypes.isNotEmpty()) {
                        Text("Special type", fontWeight = FontWeight.Medium, color = TextPrimary)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            specialTypes.forEach { specialType ->
                                FilterChip(
                                    selected = viewModel.selectedSpecialType == specialType,
                                    onClick = {
                                        viewModel.selectedSpecialType = if (viewModel.selectedSpecialType == specialType) {
                                            null
                                        } else {
                                            specialType
                                        }
                                    },
                                    label = { Text(specialType) }
                                )
                            }
                        }
                    }

                    Text("Price range (€)", fontWeight = FontWeight.Medium, color = TextPrimary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = viewModel.minPriceInput,
                            onValueChange = { viewModel.minPriceInput = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Min") }
                        )
                        OutlinedTextField(
                            value = viewModel.maxPriceInput,
                            onValueChange = { viewModel.maxPriceInput = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            label = { Text("Max") }
                        )
                    }

                    if (activeFilters.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.clearAllFilters() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Clear all filters")
                        }
                    }
                }
            }
        }

        if (viewModel.hasSearched && recentQueries.isNotEmpty()) {
            Text("Recent searches", fontWeight = FontWeight.Medium, color = TextPrimary)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recentQueries.forEach { suggestion ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            viewModel.onQueryChange(suggestion)
                            viewModel.search()
                        },
                        label = { Text(suggestion) }
                    )
                }
            }
        }

        if (!viewModel.hasSearched) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Text(
                    text = "Type a product and tap Search. Only then we show supermarkets that have it.",
                    modifier = Modifier.padding(16.dp),
                    color = TextSecondary
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4F7FF))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Filters ->", color = TextSecondary, fontSize = 12.sp)
                    activeFilters.forEach { tag ->
                        FilterChip(
                            selected = true,
                            onClick = { viewModel.removeFilter(tag.id) },
                            label = { Text("${tag.label} x") }
                        )
                    }
                    if (activeFilters.isNotEmpty()) {
                        FilterChip(
                            selected = false,
                            onClick = { viewModel.clearAllFilters() },
                            label = { Text("Clear all x") }
                        )
                    }
                }
            }

            Text(
                text = "Matching products (${productResults.size})",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            if (productResults.isNotEmpty()) {
                Text(
                    text = "Page ${currentPageIndex + 1} of $totalResultPages",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (productResults.size >= viewModel.maxVisibleSearchResults) {
                Text(
                    text = "Showing first ${viewModel.maxVisibleSearchResults} products for faster browsing.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
            if (productResults.isEmpty()) {
                Card(shape = RoundedCornerShape(20.dp)) {
                    Text(
                        text = "No products match your filters for '${viewModel.query}'.",
                        modifier = Modifier.padding(16.dp),
                        color = TextSecondary
                    )
                }
            } else {
                pagedProductResults.forEach { result ->
                    ProductResultCard(
                        result = result,
                        onOpen = { viewModel.openProductDetails(result.name) }
                    )
                }
                if (totalResultPages > 1) {
                    Card(shape = RoundedCornerShape(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { viewModel.goToPage(0, productResults) },
                                enabled = currentPageIndex > 0
                            ) {
                                Text("<<")
                            }
                            TextButton(
                                onClick = { viewModel.goToPreviousPage() },
                                enabled = currentPageIndex > 0
                            ) {
                                Text("<")
                            }
                            (0 until totalResultPages).forEach { page ->
                                FilterChip(
                                    selected = page == currentPageIndex,
                                    onClick = { viewModel.goToPage(page, productResults) },
                                    label = { Text((page + 1).toString()) }
                                )
                            }
                            TextButton(
                                onClick = { viewModel.goToNextPage(productResults) },
                                enabled = currentPageIndex < totalResultPages - 1
                            ) {
                                Text(">")
                            }
                            TextButton(
                                onClick = { viewModel.goToPage(totalResultPages - 1, productResults) },
                                enabled = currentPageIndex < totalResultPages - 1
                            ) {
                                Text(">>")
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
private fun ProductResultCard(
    result: ProductResultItem,
    onOpen: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, BorderSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.clickable(onClick = onOpen)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = offerImageRes(result.sampleOffer)),
                contentDescription = "product image",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(result.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("${result.category} • ${result.supermarkets} supermarkets", color = TextSecondary)
            Text(
                text = "Match: ${result.matchLevel}",
                color = when (result.matchLevel) {
                    "EXACT" -> SuccessGreen
                    "CLOSE" -> AccentBlue
                    else -> TextSecondary
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text("Best price from ${formatCurrency(result.bestPrice)}", color = AccentBlue, fontWeight = FontWeight.Medium)
            Text("Tap to open details", color = TextSecondary, fontSize = 12.sp)
        }
    }
}

@Composable
private fun OfferCard(
    offer: ProductOffer,
    selected: Boolean,
    onToggle: () -> Unit,
    onDetails: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, BorderSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Image(
                painter = painterResource(id = offerImageRes(offer)),
                contentDescription = "product image",
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Text(offer.name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Text("Market: ${offer.supermarket}", color = TextSecondary)
            Text("Brand: ${offer.brand}", color = TextSecondary)
            Text("Type: ${offer.specialType}", color = AccentBlue, fontWeight = FontWeight.Medium)
            Text("Pack: ${offer.packageAmount}", color = TextSecondary)
            Text("Price: ${formatCurrency(offer.price)}", color = AccentBlue, fontWeight = FontWeight.SemiBold)
            Text("Price per kg: ${pricePerKgText(offer)}", color = TextSecondary, fontSize = 12.sp)
            productQuickDetail(offer)?.let {
                Text(it, color = TextSecondary, fontSize = 12.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDetails, modifier = Modifier.weight(1f)) {
                    Text("details")
                }
                Button(onClick = onToggle, modifier = Modifier.weight(1f)) {
                    Text(if (selected) "Remove" else "Add")
                }
            }
        }
    }
}

@Composable
private fun ProductDetailsScreen(
    viewModel: MarketViewModel,
    productName: String,
    offers: List<ProductOffer>,
    onClose: () -> Unit,
    onGoToSearch: () -> Unit
) {
    val cheapest = offers.minByOrNull { it.price }
    val expensive = offers.maxByOrNull { it.price }
    val average = if (offers.isNotEmpty()) offers.sumOf { it.price } / offers.size else 0.0
    val category = offers.firstOrNull()?.category ?: "Unknown"
    val keywords = offers.firstOrNull()?.keywords ?: emptyList()
    val details = buildProductDetailsMeta(
        productName = productName,
        category = category,
        keywords = keywords
    )
    val savings = if (cheapest != null && expensive != null) expensive.price - cheapest.price else 0.0
    val bestKgOffer = offers
        .filter { unitPricePerKgValue(it) != null }
        .minByOrNull { unitPricePerKgValue(it) ?: Double.MAX_VALUE }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFF), Color(0xFFEDEFFF))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onClose) {
                Text("←")
            }
            MarkioLogo(
                onClick = onGoToSearch,
                modifier = Modifier.height(48.dp)
            )
        }

        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                offers.firstOrNull()?.let { offer ->
                    Image(
                        painter = painterResource(id = offerImageRes(offer)),
                        contentDescription = "product image",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(productName, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text("${details.brand}  •  ${details.packSize}", color = TextSecondary)
                Text("$category  •  Origin: ${details.origin}", color = TextSecondary)
                Text("Type: ${offers.firstOrNull()?.specialType ?: "Regular"}", color = AccentBlue, fontWeight = FontWeight.Medium)
                offers.firstOrNull()?.let { lead ->
                    Text(typeExplanation(lead.specialType, lead.category), color = TextSecondary, fontSize = 12.sp)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("Best price", color = TextSecondary, fontSize = 12.sp)
                if (cheapest != null && expensive != null) {
                    Text(
                        formatCurrency(cheapest.price),
                        color = TextPrimary,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("at ${cheapest.supermarket}", color = SuccessGreen)
                    Text(
                        "Save up to ${formatCurrency(savings)} vs highest offer",
                        color = AccentBlue,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Average market price: ${formatCurrency(average)}",
                        color = TextSecondary
                    )
                    bestKgOffer?.let { kgOffer ->
                        Text(
                            "Best price per kg: ${pricePerKgText(kgOffer)} at ${kgOffer.supermarket}",
                            color = AccentBlue
                        )
                    }
                }
            }
        }

        if (keywords.isNotEmpty()) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Keywords", color = TextPrimary, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        keywords.take(10).forEach { keyword ->
                            FilterChip(
                                selected = false,
                                onClick = { },
                                label = { Text(keyword) }
                            )
                        }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Available supermarkets", color = TextPrimary, fontWeight = FontWeight.Medium)
                offers.forEach { offer ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFF))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(offer.supermarket, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("Brand: ${offer.brand}", color = TextSecondary, fontSize = 12.sp)
                                Text(
                                    "${offer.specialType} • ${offer.packageAmount}",
                                    color = AccentBlue,
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Stock signal: ${stockSignal(offer.weeklySales)}",
                                    color = SuccessGreen,
                                    fontSize = 12.sp
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = formatCurrency(offer.price),
                                    color = if (offer == cheapest) SuccessGreen else TextPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = pricePerKgText(offer),
                                    color = TextSecondary,
                                    fontSize = 11.sp
                                )
                                val selected = viewModel.offerKey(offer) in viewModel.selectedOfferKeys
                                TextButton(onClick = { viewModel.toggleSelectedOffer(offer) }) {
                                    Text(if (selected) "Remove" else "Add")
                                }
                            }
                        }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Product information", color = TextPrimary, fontWeight = FontWeight.Medium)
                Text("Ingredients: ${details.ingredients}", color = TextSecondary)
                Text("Storage: ${details.storage}", color = TextSecondary)
                Text("Allergen notes: ${details.allergens}", color = TextSecondary)
            }
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Nutrition (per 100g/ml)", color = TextPrimary, fontWeight = FontWeight.Medium)
                details.nutritionRows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(row.first, color = TextSecondary)
                        Text(row.second, color = TextPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun MarkioLogo(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = R.drawable.ic_markio_logo),
        contentDescription = "Markio logo",
        modifier = modifier.clickable(onClick = onClick),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun AdminAddItemToolDialog(
    viewModel: MarketViewModel,
    onDismiss: () -> Unit,
    onAdded: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Milchprodukte") }
    var subcategory by remember { mutableStateOf("Milch") }
    var packageAmount by remember { mutableStateOf("500g") }
    var basePrice by remember { mutableStateOf("1,99") }
    var cloneCount by remember { mutableIntStateOf(1) }
    var selectedSpecialType by remember { mutableStateOf("Regular") }
    val selectedMarkets = remember { mutableStateListOf<String>() }

    val categoryOptions = listOf(
        "Milchprodukte", "Obst", "Gemüse", "Fleisch & Fisch", "Vegane Alternativen",
        "Getränke", "Snacks", "Backwaren", "Tiefkühlprodukte", "Konserven", "Trockenwaren"
    )
    val subcategoryOptions = mapOf(
        "Milchprodukte" to listOf("Milch", "Joghurt", "Käse"),
        "Obst" to listOf("Frischobst", "Beeren", "Exotisch"),
        "Gemüse" to listOf("Frischgemüse", "Hülsenfrüchte"),
        "Fleisch & Fisch" to listOf("Fleisch", "Fisch", "Wurst"),
        "Vegane Alternativen" to listOf("Pflanzliche Milch", "Fleischersatz"),
        "Getränke" to listOf("Wasser", "Säfte", "Kaffee", "Tee"),
        "Snacks" to listOf("Chips", "Süßigkeiten", "Nüsse"),
        "Backwaren" to listOf("Brot", "Brötchen", "Gebäck"),
        "Tiefkühlprodukte" to listOf("Pizza", "Gemüse", "Eis"),
        "Konserven" to listOf("Dosen", "Gläser"),
        "Trockenwaren" to listOf("Pasta", "Reis", "Getreide")
    )
    val specialTypeOptions = listOf("Regular", "Bio", "Vegan", "Gluten Free", "Lactose Free", "High-Protein", "Low Carb")
    val activeSubcategories = subcategoryOptions[category].orEmpty()
    if (subcategory !in activeSubcategories && activeSubcategories.isNotEmpty()) {
        subcategory = activeSubcategories.first()
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Admin item tool", style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                Text(
                    "Add one item and auto-generate supermarket-ready offers with up to 3 modified clones.",
                    color = TextSecondary
                )
                HorizontalDivider()

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = brand,
                    onValueChange = { brand = it },
                    label = { Text("Brand (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = packageAmount,
                        onValueChange = { packageAmount = it },
                        label = { Text("Pack (500g, 1l, 6stk)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = basePrice,
                        onValueChange = { basePrice = it },
                        label = { Text("Base price €") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Category", color = TextPrimary, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryOptions.forEach { option ->
                        FilterChip(
                            selected = option == category,
                            onClick = { category = option },
                            label = { Text(option) }
                        )
                    }
                }

                if (activeSubcategories.isNotEmpty()) {
                    Text("Subcategory", color = TextPrimary, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeSubcategories.forEach { option ->
                            FilterChip(
                                selected = option == subcategory,
                                onClick = { subcategory = option },
                                label = { Text(option) }
                            )
                        }
                    }
                }

                Text("Special type", color = TextPrimary, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    specialTypeOptions.forEach { option ->
                        FilterChip(
                            selected = option == selectedSpecialType,
                            onClick = { selectedSpecialType = option },
                            label = { Text(option) }
                        )
                    }
                }

                Text("Clone variants (max 3)", color = TextPrimary, fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0..3).forEach { count ->
                        FilterChip(
                            selected = cloneCount == count,
                            onClick = { cloneCount = count },
                            label = { Text(count.toString()) }
                        )
                    }
                }

                Text("Supermarkets", color = TextPrimary, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    viewModel.supermarkets.forEach { market ->
                        FilterChip(
                            selected = market in selectedMarkets,
                            onClick = {
                                if (market in selectedMarkets) {
                                    selectedMarkets.remove(market)
                                } else {
                                    selectedMarkets.add(market)
                                }
                            },
                            label = { Text(market) }
                        )
                    }
                }
                Text(
                    "No selection = auto use 3 supermarkets.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                viewModel.adminToolMessage?.let { message ->
                    Text(message, color = AccentBlue, fontSize = 12.sp)
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = {
                            val added = viewModel.addAdminItemSmart(
                                name = name,
                                brand = brand,
                                category = category,
                                subcategory = subcategory,
                                packageAmount = packageAmount,
                                basePriceInput = basePrice,
                                selectedSupermarkets = selectedMarkets.toSet(),
                                specialType = selectedSpecialType,
                                cloneCount = cloneCount
                            )
                            if (added) onAdded()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add item")
                    }
                }
            }
        }
    }
}

private data class ProductDetailsMeta(
    val brand: String,
    val packSize: String,
    val origin: String,
    val ingredients: String,
    val storage: String,
    val allergens: String,
    val nutritionRows: List<Pair<String, String>>
)

private fun buildProductDetailsMeta(
    productName: String,
    category: String,
    keywords: List<String>
): ProductDetailsMeta {
    val brands = listOf("Fresh Choice", "Green Valley", "Urban Select", "Daily Good", "Value Home")
    val brand = brands[(productName.hashCode().absoluteValue) % brands.size]
    val packSize = Regex("(\\d+\\s?(g|kg|ml|l|pc))", RegexOption.IGNORE_CASE)
        .find(productName)
        ?.value
        ?.replace(" ", "")
        ?: "Standard pack"

    val origin = when (category) {
        "Fruits" -> "Spain / Italy"
        "Vegetables" -> "Germany / Netherlands"
        "Dairy" -> "Germany"
        "Meat & Fish" -> "EU farms / North Atlantic"
        "Bakery" -> "Regional bakery"
        "Pantry" -> "EU suppliers"
        "Frozen" -> "EU production"
        else -> "EU"
    }

    val ingredients = when (category) {
        "Fruits", "Vegetables" -> "100% natural produce"
        "Dairy" -> "Milk cultures and natural dairy ingredients"
        "Meat & Fish" -> "Selected protein cuts"
        "Bakery" -> "Wheat flour, yeast, water, salt"
        "Pantry" -> "Core pantry ingredients"
        "Frozen" -> "Flash-frozen quality ingredients"
        else -> "See package label"
    }

    val storage = when (category) {
        "Fruits", "Vegetables" -> "Store cool and dry, consume fresh."
        "Frozen" -> "Keep at -18 C or colder."
        "Dairy", "Meat & Fish" -> "Refrigerate between 2-7 C."
        else -> "Store in a cool, dry place."
    }

    val allergens = when {
        category == "Dairy" -> "Contains milk."
        category == "Bakery" -> "May contain gluten and sesame."
        keywords.any { it.contains("fish") || it.contains("shrimp") || it.contains("tuna") } -> "Contains fish/shellfish."
        else -> "No major allergens declared."
    }

    val nutrition = nutritionProfile(
        productName = productName,
        category = category,
        keywords = keywords
    )
    val nutritionRows = listOf(
        "Energy" to "${nutrition.energyKj} kJ",
        "Fat" to "${nutrition.fatG} g",
        "Carbohydrates" to "${nutrition.carbsG} g",
        "Protein" to "${nutrition.proteinG} g"
    )

    return ProductDetailsMeta(
        brand = brand,
        packSize = packSize,
        origin = origin,
        ingredients = ingredients,
        storage = storage,
        allergens = allergens,
        nutritionRows = nutritionRows
    )
}

private fun valueIndex(offer: ProductOffer): Double {
    val specialTypeWeight = when (offer.specialType) {
        "High-Protein" -> 2.6
        "Bio" -> 2.2
        "Gluten Free" -> 2.0
        "Low Carb" -> 2.1
        else -> 2.0
    }
    return specialTypeWeight / offer.price
}

private fun unitPriceText(offer: ProductOffer): String {
    val parsed = parsePackageAmount(offer.packageAmount) ?: return "n/a"
    val (baseAmount, type) = parsed
    if (baseAmount <= 0.0) return "n/a"
    return when (type) {
        "weight" -> "${formatCurrency(offer.price / baseAmount)}/kg"
        "volume" -> "${formatCurrency(offer.price / baseAmount)}/l"
        "count" -> "${formatCurrency(offer.price / baseAmount)}/pc"
        else -> "n/a"
    }
}

private fun pricePerKgText(offer: ProductOffer): String {
    val value = unitPricePerKgValue(offer) ?: return "n/a"
    return "${formatCurrency(value)}/kg"
}

private fun unitPricePerKgValue(offer: ProductOffer): Double? {
    val parsed = parsePackageAmount(offer.packageAmount) ?: return null
    val (amount, type) = parsed
    if (amount <= 0.0 || type != "weight") return null
    return offer.price / amount
}

private fun parsePackageAmount(text: String): Pair<Double, String>? {
    val match = Regex("(\\d+(?:\\.\\d+)?)\\s?(kg|g|l|ml|pc|pcs)", RegexOption.IGNORE_CASE).find(text)
        ?: return null
    val amount = match.groupValues[1].toDoubleOrNull() ?: return null
    return when (match.groupValues[2].lowercase()) {
        "kg" -> amount to "weight"
        "g" -> (amount / 1000.0) to "weight"
        "l" -> amount to "volume"
        "ml" -> (amount / 1000.0) to "volume"
        "pc", "pcs" -> amount to "count"
        else -> null
    }
}

@Composable
private fun CompareScreen(viewModel: MarketViewModel, modifier: Modifier = Modifier) {
    val selectedOffers = viewModel.selectedOffers()
    val productGroups = selectedOffers.groupBy { it.name }.toList().sortedBy { it.first }
    val marketCount = selectedOffers.map { it.supermarket }.distinct().size
    val bestPriceOffer = selectedOffers.minByOrNull { it.price }
    val bestValueOffer = selectedOffers.maxByOrNull { valueIndex(it) }
    val bestKgOffer = selectedOffers
        .filter { unitPricePerKgValue(it) != null }
        .minByOrNull { unitPricePerKgValue(it) ?: Double.MAX_VALUE }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFF), Color(0xFFEFF3FF))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Compare Selected Products",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text = "Human-friendly comparison: best deal, best fit, and clear trade-offs.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard("Selected offers", selectedOffers.size.toString(), Modifier.weight(1f))
            StatCard("Supermarkets", marketCount.toString(), Modifier.weight(1f))
        }

        if (bestPriceOffer != null || bestValueOffer != null) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF4FF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Highlights", fontWeight = FontWeight.Medium, color = TextPrimary)
                    bestPriceOffer?.let {
                        Text(
                            "Best price: ${it.name} at ${it.supermarket} (${formatCurrency(it.price)})",
                            color = SuccessGreen
                        )
                    }
                    bestValueOffer?.let {
                        Text(
                            "Best value: ${it.name} (${it.specialType}) at ${it.supermarket}",
                            color = AccentBlue
                        )
                    }
                    bestKgOffer?.let {
                        Text(
                            "Best €/kg: ${pricePerKgText(it)} at ${it.supermarket}",
                            color = AccentBlue
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.compareSelectedProducts() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !viewModel.isComparing && viewModel.selectedOfferKeys.isNotEmpty()
        ) {
            Text(if (viewModel.isComparing) "Comparing..." else "Run comparison")
        }

        Text(
            text = "Added for comparison (${viewModel.selectedOfferKeys.size})",
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary
        )

        if (viewModel.selectedOfferKeys.isEmpty()) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Text(
                    text = "No items added yet. Go to Search and add products to compare.",
                    modifier = Modifier.padding(16.dp),
                    color = TextSecondary
                )
            }
        } else {
            productGroups.forEach { (name, offers) ->
                val cheapestForProduct = offers.minByOrNull { it.price }
                val expensiveForProduct = offers.maxByOrNull { it.price }
                val nutritionReference = nutritionComparisonText(cheapestForProduct ?: offers.first())
                Card(shape = RoundedCornerShape(20.dp)) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                        Text(
                            text = humanComparisonSummary(offers),
                            color = TextSecondary
                        )
                        Text(
                            text = nutritionReference,
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        cheapestForProduct?.let {
                            Text(
                                "Best offer now: ${it.supermarket} • ${formatCurrency(it.price)}",
                                color = SuccessGreen
                            )
                        }
                        if (cheapestForProduct != null && expensiveForProduct != null) {
                            Text(
                                "Potential saving: ${formatCurrency(expensiveForProduct.price - cheapestForProduct.price)}",
                                color = AccentBlue,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        if (viewModel.comparisonResults.isEmpty()) {
            Card(shape = RoundedCornerShape(20.dp)) {
                Text(
                    text = "No comparison results yet. Search products first, select a few, then run comparison.",
                    modifier = Modifier.padding(16.dp),
                    color = TextSecondary
                )
            }
        } else {
            viewModel.comparisonResults.forEach { comparison ->
                ComparisonCard(comparison = comparison)
            }
        }
    }
}

private fun humanComparisonSummary(offers: List<ProductOffer>): String {
    val brands = offers.map { it.brand }.distinct()
    val types = offers.map { it.specialType }.distinct()
    val markets = offers.map { it.supermarket }.distinct().size
    return buildString {
        append("Compared across $markets markets")
        if (brands.isNotEmpty()) append(" • Brands: ${brands.take(2).joinToString()}${if (brands.size > 2) " +" else ""}")
        if (types.isNotEmpty()) append(" • Type: ${types.joinToString("/")}")
    }
}

private fun nutritionComparisonText(offer: ProductOffer): String {
    val nutrition = nutritionProfile(
        productName = offer.name,
        category = offer.category,
        keywords = offer.keywords
    )
    val rows = listOf(
        "Energy ${nutrition.energyKj} kJ",
        "Carbs ${nutrition.carbsG} g",
        "Protein ${nutrition.proteinG} g",
        "Fat ${nutrition.fatG} g"
    )
    return "Nutrition per 100g/ml • ${rows.joinToString(" • ")}"
}

private data class NutritionProfile(
    val energyKj: Int,
    val carbsG: Double,
    val proteinG: Double,
    val fatG: Double
)

private fun nutritionProfile(
    productName: String,
    category: String,
    keywords: List<String>
): NutritionProfile {
    val text = (productName + " " + keywords.joinToString(" ") + " " + category).lowercase()
    return when {
        text.contains("wasser") -> NutritionProfile(0, 0.0, 0.0, 0.0)
        text.contains("saft") || text.contains("juice") -> NutritionProfile(190, 10.5, 0.5, 0.1)
        text.contains("kaffee") || text.contains("coffee") || text.contains("tee") -> NutritionProfile(8, 0.0, 0.2, 0.0)

        text.contains("milch") && !text.contains("hafer") && !text.contains("soja") -> NutritionProfile(270, 4.8, 3.4, 3.6)
        text.contains("joghurt") || text.contains("yogurt") -> NutritionProfile(320, 5.8, 4.0, 4.2)
        text.contains("käse") || text.contains("kaese") || text.contains("gouda") -> NutritionProfile(1480, 1.5, 25.0, 30.0)
        text.contains("haferdrink") || text.contains("hafermilch") || text.contains("oat") -> NutritionProfile(180, 6.4, 1.0, 2.9)
        text.contains("sojadrink") || text.contains("soja") -> NutritionProfile(140, 2.5, 3.3, 1.8)

        text.contains("hähnchen") || text.contains("haehnchen") || text.contains("chicken") -> NutritionProfile(540, 0.0, 23.0, 3.0)
        text.contains("rinderhack") || text.contains("beef") || text.contains("rind") -> NutritionProfile(1030, 0.0, 19.0, 17.0)
        text.contains("lachs") || text.contains("salmon") -> NutritionProfile(870, 0.0, 20.0, 13.0)
        text.contains("wurst") || text.contains("aufschnitt") -> NutritionProfile(980, 1.2, 16.0, 19.0)

        text.contains("apfel") -> NutritionProfile(220, 12.0, 0.3, 0.2)
        text.contains("banane") -> NutritionProfile(370, 21.0, 1.1, 0.3)
        text.contains("beere") || text.contains("heidel") || text.contains("blueberr") -> NutritionProfile(240, 13.0, 0.7, 0.3)
        text.contains("tomat") -> NutritionProfile(80, 3.5, 0.9, 0.2)
        text.contains("paprika") -> NutritionProfile(130, 6.0, 1.0, 0.3)
        text.contains("kichererb") || text.contains("chickpea") -> NutritionProfile(690, 18.0, 7.0, 2.5)
        text.contains("bohnen") || text.contains("kidney") -> NutritionProfile(540, 13.0, 7.8, 0.6)
        text.contains("mais") || text.contains("corn") -> NutritionProfile(390, 17.0, 3.4, 1.4)

        text.contains("spaghetti") || text.contains("penne") || text.contains("pasta") || text.contains("nudel") ->
            NutritionProfile(1510, 72.0, 12.0, 1.8)
        text.contains("reis") || text.contains("basmati") -> NutritionProfile(1480, 78.0, 7.2, 0.8)
        text.contains("haferflock") || text.contains("oats") -> NutritionProfile(1540, 59.0, 13.0, 7.0)
        text.contains("brot") || text.contains("bröt") || text.contains("broet") -> NutritionProfile(980, 43.0, 8.5, 2.5)
        text.contains("croissant") -> NutritionProfile(1670, 45.0, 8.0, 21.0)

        text.contains("chips") -> NutritionProfile(2250, 52.0, 6.0, 35.0)
        text.contains("nuss") || text.contains("nut") -> NutritionProfile(2550, 14.0, 19.0, 52.0)
        text.contains("schokolade") || text.contains("chocolate") -> NutritionProfile(2250, 56.0, 7.0, 32.0)
        text.contains("pizza") -> NutritionProfile(1050, 28.0, 11.0, 10.0)
        text.contains("eis") || text.contains("ice cream") -> NutritionProfile(920, 24.0, 3.4, 10.0)

        text.contains("vegan") && text.contains("burger") -> NutritionProfile(980, 12.0, 16.0, 14.0)

        category.contains("obst", ignoreCase = true) || category.contains("gemüse", ignoreCase = true) ||
            category.contains("fruits", ignoreCase = true) || category.contains("vegetables", ignoreCase = true) ->
            NutritionProfile(180, 8.7, 1.2, 0.3)
        category.contains("milch", ignoreCase = true) || category.contains("dairy", ignoreCase = true) ->
            NutritionProfile(270, 4.8, 3.3, 3.4)
        category.contains("fleisch", ignoreCase = true) || category.contains("fisch", ignoreCase = true) ||
            category.contains("meat", ignoreCase = true) ->
            NutritionProfile(620, 0.0, 22.0, 8.2)
        else -> NutritionProfile(900, 34.0, 6.1, 12.0)
    }
}

private fun formatCount(value: Int): String {
    return NumberFormat.getIntegerInstance(Locale.GERMANY).apply {
        isGroupingUsed = true
    }.format(value)
}

private fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(Locale.GERMANY).apply {
        currency = Currency.getInstance("EUR")
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }.format(value)
}

private fun formatCompactCount(value: Int): String {
    val numberFormat = NumberFormat.getNumberInstance(Locale.GERMANY).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
        isGroupingUsed = true
    }
    val absValue = value.absoluteValue.toDouble()
    return when {
        absValue >= 1_000_000 -> "${numberFormat.format(value / 1_000_000.0)} Mio."
        absValue >= 1_000 -> "${numberFormat.format(value / 1_000.0)} Tsd."
        else -> formatCount(value)
    }
}

private fun formatPercent(value: Double): String {
    return NumberFormat.getNumberInstance(Locale.GERMANY).apply {
        maximumFractionDigits = 1
        minimumFractionDigits = 0
        isGroupingUsed = true
    }.format(value) + "%"
}

private fun formatMonthChipLabel(monthKey: String): String {
    return try {
        val parser = SimpleDateFormat("MM", Locale.US).apply { isLenient = false }
        val date: Date = parser.parse(monthKey) ?: return monthKey
        SimpleDateFormat("MMMM", Locale.ENGLISH).format(date)
    } catch (_: Exception) {
        monthKey
    }
}

private fun formatDayOfMonth(day: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val date: Date = parser.parse(day) ?: return day
        SimpleDateFormat("dd", Locale.GERMANY).format(date)
    } catch (_: Exception) {
        day
    }
}

private fun formatDayMonthForSelection(day: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
        val date: Date = parser.parse(day) ?: return day
        SimpleDateFormat("dd MMM", Locale.GERMANY).format(date)
    } catch (_: Exception) {
        day
    }
}

private fun formatWholeHourLabel(hour: String): String {
    val h24 = hour.trim().take(2).toIntOrNull() ?: return hour
    val h12 = when {
        h24 == 0 -> 12
        h24 > 12 -> h24 - 12
        else -> h24
    }
    val period = if (h24 < 12) "AM" else "PM"
    return "$h12 $period"
}

private fun formatProductLabel(name: String): String {
    if (name.isBlank()) return "-"
    return name.split(Regex("\\s+"))
        .joinToString(" ") { word ->
            word.replaceFirstChar { first ->
                if (first.isLowerCase()) first.titlecase(Locale.GERMANY) else first.toString()
            }
        }
}

private fun productQuickDetail(offer: ProductOffer): String? {
    return when {
        offer.category in listOf("Fruits", "Vegetables") -> "Fresh produce • ${offer.specialType}"
        offer.category == "Dairy" && offer.specialType == "High-Protein" -> "Good protein option for daily use"
        offer.category == "Bakery" && offer.specialType == "Gluten Free" -> "Suitable for gluten-free diet"
        offer.specialType == "Low Carb" -> "Lower-carb choice"
        offer.specialType == "Bio" -> "Organic-focused choice"
        else -> null
    }
}

private fun typeExplanation(type: String, category: String): String {
    return when (type) {
        "Bio" -> "Bio options focus on organic sourcing and cleaner ingredient profiles."
        "Gluten Free" -> "Gluten-free variant, useful for sensitive diets."
        "Low Carb" -> "Lower carbohydrate profile for balanced intake."
        "High-Protein" -> "Higher protein profile for satiety and active lifestyles."
        else -> "Standard $category profile for everyday shopping."
    }
}

private fun stockSignal(weeklySales: Int): String {
    return when {
        weeklySales >= 220 -> "Very high rotation"
        weeklySales >= 150 -> "High rotation"
        weeklySales >= 90 -> "Stable stock"
        else -> "Limited rotation"
    }
}

private fun offerImageRes(offer: ProductOffer): Int {
    return when {
        offer.specialType == "High-Protein" -> R.drawable.ic_product_fresh
        offer.specialType == "Bio" -> R.drawable.ic_product_organic
        offer.category == "Beverages" -> R.drawable.ic_product_beverage
        offer.category == "Frozen" -> R.drawable.ic_product_frozen
        offer.category == "Bakery" -> R.drawable.ic_product_bakery
        offer.category == "Pet" -> R.drawable.ic_product_pet
        offer.category in listOf("Fruits", "Vegetables") -> R.drawable.ic_product_fresh
        else -> R.drawable.ic_product_grocery
    }
}

@Composable
private fun ComparisonCard(comparison: ProductComparison) {
    val cheapest = comparison.cheapest
    val expensive = comparison.mostExpensive
    val averagePrice = if (comparison.offers.isNotEmpty()) {
        comparison.offers.sumOf { it.price } / comparison.offers.size
    } else {
        0.0
    }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        border = BorderStroke(1.dp, BorderSoft),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = comparison.productName,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )

            comparison.offers.forEach { offer ->
                val isCheapest = cheapest != null &&
                    offer.supermarket == cheapest.supermarket &&
                    offer.price == cheapest.price
                val textColor = if (isCheapest) SuccessGreen else TextSecondary
                val label = if (isCheapest) " (best)" else ""
                Text(
                    text = "${offer.supermarket}: ${formatCurrency(offer.price)}$label",
                    color = textColor
                )
            }

            if (cheapest != null && expensive != null) {
                Text(
                    text = "Savings potential: ${formatCurrency(expensive.price - cheapest.price)}",
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Price range: ${formatCurrency(cheapest.price)} - ${formatCurrency(expensive.price)}",
                    color = TextSecondary
                )
                Text(
                    text = "Average price: ${formatCurrency(averagePrice)}",
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun StatsScreen(viewModel: MarketViewModel, modifier: Modifier = Modifier) {
    val topProducts by viewModel.topSearchedProducts.collectAsStateWithLifecycle()
    val selectedTrend by viewModel.selectedProductTrend.collectAsStateWithLifecycle()
    val selectedHours by viewModel.selectedProductHours.collectAsStateWithLifecycle()
    var selectedMonthKey by remember { mutableStateOf<String?>(null) }
    var isMonthMenuOpen by remember { mutableStateOf(false) }
    var selectedTrendPoint by remember { mutableStateOf<ProductSearchTrendPoint?>(null) }
    var selectedHourPoint by remember { mutableStateOf<ProductSearchHourPoint?>(null) }
    var showPeakDayPopup by remember { mutableStateOf(false) }
    var showPeakHourPopup by remember { mutableStateOf(false) }
    var analyticsProductQuery by remember { mutableStateOf("") }
    var analyticsSuggestionsOpen by remember { mutableStateOf(false) }
    LaunchedEffect(topProducts) {
        if (viewModel.selectedAnalyticsProduct.isBlank() && topProducts.isNotEmpty()) {
            viewModel.selectAnalyticsProduct(topProducts.first().product)
        }
    }
    LaunchedEffect(viewModel.selectedAnalyticsProduct) {
        if (viewModel.selectedAnalyticsProduct.isNotBlank()) {
            analyticsProductQuery = viewModel.selectedAnalyticsProduct
        }
    }
    val currentYear = SimpleDateFormat("yyyy", Locale.US).format(Date())
    val currentMonthInt = SimpleDateFormat("MM", Locale.US).format(Date()).toIntOrNull() ?: 12
    val trendCurrentYearUntilNow = selectedTrend.filter { point ->
        val year = point.day.takeIf { it.length >= 4 }?.take(4) ?: return@filter false
        val month = point.day.takeIf { it.length >= 7 }?.substring(5, 7)?.toIntOrNull() ?: return@filter false
        year == currentYear && month <= currentMonthInt
    }
    val availableMonths = trendCurrentYearUntilNow
        .mapNotNull { point -> point.day.takeIf { it.length >= 7 }?.substring(5, 7) }
        .distinct()
        .sortedByDescending { it.toIntOrNull() ?: -1 }
    val currentMonthKey = "%02d".format(currentMonthInt)
    if (selectedMonthKey == null && currentMonthKey in availableMonths) {
        selectedMonthKey = currentMonthKey
    }
    if (selectedMonthKey != null && selectedMonthKey !in availableMonths) {
        selectedMonthKey = availableMonths.firstOrNull()
    }
    if (selectedMonthKey == null && availableMonths.isNotEmpty()) {
        selectedMonthKey = availableMonths.first()
    }
    val monthFilteredTrend = if (selectedMonthKey != null) {
        trendCurrentYearUntilNow.filter { point -> point.day.length >= 7 && point.day.substring(5, 7) == selectedMonthKey }
    } else {
        trendCurrentYearUntilNow
    }
    val displayedTrend = monthFilteredTrend.sortedBy { it.day }
    val topByName = topProducts.associateBy { it.product.lowercase() }
    val analyticsQueryNormalized = analyticsProductQuery.trim()
    val analyticsMatches = if (analyticsQueryNormalized.length < 3) {
        emptyList()
    } else {
        viewModel.analyticsCatalogProducts
            .asSequence()
            .filter { it.contains(analyticsQueryNormalized, ignoreCase = true) }
            .sortedWith(
                compareByDescending<String> { it.startsWith(analyticsQueryNormalized, ignoreCase = true) }
                    .thenBy { it.lowercase() }
            )
            .take(4)
            .toList()
    }
    if (selectedTrendPoint != null && monthFilteredTrend.none { it.day == selectedTrendPoint?.day }) {
        selectedTrendPoint = null
    }
    val maxTrendCount = (displayedTrend.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    val selectedProductTotal = topProducts.firstOrNull {
        it.product.equals(viewModel.selectedAnalyticsProduct, ignoreCase = true)
    }?.count ?: selectedTrend.sumOf { it.count }
    val peakTrendPoint = monthFilteredTrend.maxByOrNull { it.count }
    val totalDailySignals = monthFilteredTrend.sumOf { it.count }.coerceAtLeast(1)
    val peakDaySharePercent = peakTrendPoint?.let { (it.count * 100.0) / totalDailySignals }
    val dayCountInPeriod = monthFilteredTrend.size.coerceAtLeast(1)
    val avgDailySearches = totalDailySignals.toDouble() / dayCountInPeriod.toDouble()
    val dailyTarget = avgDailySearches.roundToInt().coerceAtLeast(1)
    val hourlyWeightTotal = selectedHours.sumOf { it.count }.coerceAtLeast(1)
    val rawHourWeights: List<Pair<String, Double>> = selectedHours.map { hourPoint ->
        hourPoint.hour to (hourPoint.count.toDouble() / hourlyWeightTotal.toDouble())
    }
    val adjustedHours = run {
        val baseCounts = rawHourWeights.associate { (hour, weight) ->
            hour to kotlin.math.floor(dailyTarget * weight).toInt().coerceAtLeast(0)
        }.toMutableMap()
        var distributed = baseCounts.values.sum()
        val remainders = rawHourWeights
            .map { (hour, weight) ->
                val exact = dailyTarget * weight
                val remainder = exact - kotlin.math.floor(exact)
                hour to remainder
            }
            .sortedByDescending { it.second }
        var idx = 0
        while (distributed < dailyTarget && idx < remainders.size) {
            val hour = remainders[idx].first
            baseCounts[hour] = (baseCounts[hour] ?: 0) + 1
            distributed += 1
            idx += 1
        }
        // If everything rounded to zero despite target > 0, force one visible peak hour.
        if (baseCounts.values.sum() == 0 && dailyTarget > 0 && remainders.isNotEmpty()) {
            val peakHour = remainders.first().first
            baseCounts[peakHour] = 1
        }
        selectedHours.map { hourPoint ->
            ProductSearchHourPoint(hour = hourPoint.hour, count = baseCounts[hourPoint.hour] ?: 0)
        }
    }
    if (selectedHourPoint != null && adjustedHours.none { it.hour == selectedHourPoint?.hour }) {
        selectedHourPoint = null
    }
    val maxHourCount = (adjustedHours.maxOfOrNull { it.count } ?: 1).coerceAtLeast(1)
    val peakAdjustedHour = adjustedHours.maxByOrNull { it.count }
    val peakHourSharePercent = if (peakTrendPoint != null && peakTrendPoint.count > 0 && peakAdjustedHour != null) {
        (peakAdjustedHour.count * 100.0) / peakTrendPoint.count.toDouble()
    } else {
        null
    }
    val whiteStatsCardColors = CardDefaults.cardColors(containerColor = Color.White)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFF), Color(0xFFEDEFFF))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Product Search Insights",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )
        Text(
            text = "Discover which products are searched most, when interest peaks, and how demand changes over time.",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        if (topProducts.isEmpty()) {
            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = whiteStatsCardColors
            ) {
                Text(
                    text = "No product search history yet. Search items in Compare to generate product-level analytics.",
                    modifier = Modifier.padding(16.dp),
                    color = TextSecondary
                )
            }
        } else {
            Card(
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                colors = whiteStatsCardColors
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Search product statistics",
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary
                    )
                    OutlinedTextField(
                        value = analyticsProductQuery,
                        onValueChange = { value ->
                            analyticsProductQuery = value
                            analyticsSuggestionsOpen = value.trim().length >= 3
                        },
                        singleLine = true,
                        label = { Text("Product name") },
                        placeholder = { Text("e.g. Bananas, Yogurt, Spaghetti") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (analyticsSuggestionsOpen && analyticsMatches.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F9FF))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                analyticsMatches.forEach { itemName ->
                                    val knownCount = topByName[itemName.lowercase()]?.count
                                    TextButton(
                                        onClick = {
                                            analyticsProductQuery = itemName
                                            viewModel.selectAnalyticsProduct(itemName)
                                            analyticsSuggestionsOpen = false
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = if (knownCount != null) {
                                                "${formatProductLabel(itemName)} (${formatCount(knownCount)})"
                                            } else {
                                                formatProductLabel(itemName)
                                            },
                                            color = TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (analyticsQueryNormalized.length >= 3 && analyticsMatches.isEmpty()) {
                        Text(
                            text = "No matching product in analytics data.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard("Selected product", formatProductLabel(viewModel.selectedAnalyticsProduct), Modifier.weight(1f))
                StatCard("Total searches", formatCount(selectedProductTotal), Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCard(
                    title = "Peak day",
                    value = peakTrendPoint?.let { formatDayMonthForSelection(it.day) } ?: "-",
                    modifier = Modifier.weight(1f),
                    onClick = { if (peakTrendPoint != null) showPeakDayPopup = true }
                )
                StatCard(
                    title = "Peak Hour",
                    value = peakAdjustedHour?.let {
                        "${formatCompactCount(it.count)} searches"
                    } ?: "-",
                    modifier = Modifier.weight(1f),
                    onClick = { if (peakAdjustedHour != null) showPeakHourPopup = true }
                )
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = whiteStatsCardColors
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Searches over time for '${formatProductLabel(viewModel.selectedAnalyticsProduct)}'",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Tap a pillar to see exact value.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box {
                            FilterChip(
                                selected = true,
                                onClick = { isMonthMenuOpen = true },
                                label = { Text("Current month: ${selectedMonthKey?.let(::formatMonthChipLabel) ?: "-"} ▾") }
                            )
                            DropdownMenu(
                                expanded = isMonthMenuOpen,
                                onDismissRequest = { isMonthMenuOpen = false }
                            ) {
                                availableMonths.forEach { month ->
                                    DropdownMenuItem(
                                        text = { Text(formatMonthChipLabel(month)) },
                                        onClick = {
                                            selectedMonthKey = month
                                            selectedTrendPoint = null
                                            isMonthMenuOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (monthFilteredTrend.isEmpty()) {
                        Text("No timeline data yet for this product.", color = TextSecondary)
                    } else {
                        val trendBars = displayedTrend
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(192.dp)
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            trendBars.forEach { point ->
                                val fraction = point.count.toFloat() / maxTrendCount.toFloat()
                                val isSelected = selectedTrendPoint?.day == point.day
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(24.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(136.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { selectedTrendPoint = point }
                                            .background(Color(0xFFEFF2FF)),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height((120 * fraction).dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = if (isSelected) {
                                                            listOf(Color(0xFF557BFF), Color(0xFF7A5BFF))
                                                        } else {
                                                            listOf(Color(0xFF9CB2FF), Color(0xFFB197FF))
                                                        }
                                                    )
                                                )
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = formatDayOfMonth(point.day),
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                        selectedTrendPoint?.let {
                            ChartSelectionPopup(
                                title = "Selected day",
                                primaryText = formatDayMonthForSelection(it.day),
                                secondaryText = "${formatCount(it.count)} searches"
                            )
                        }
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                colors = whiteStatsCardColors
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "When '${formatProductLabel(viewModel.selectedAnalyticsProduct)}' is searched",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )

                    if (adjustedHours.isEmpty()) {
                        Text("No hourly pattern yet for this product.", color = TextSecondary)
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            adjustedHours.forEach { point ->
                                val fraction = point.count.toFloat() / maxHourCount.toFloat()
                                val isSelected = selectedHourPoint?.hour == point.hour
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(20.dp)
                                            .height(132.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedHourPoint = point }
                                            .background(Color(0xFFF1EEFF)),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height((120 * fraction).dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    Brush.verticalGradient(
                                                        colors = if (isSelected) {
                                                            listOf(Color(0xFF6E59FF), Color(0xFF4D7CFF))
                                                        } else {
                                                            listOf(Color(0xFFB39EFF), Color(0xFF8CA8FF))
                                                        }
                                                    )
                                                )
                                        )
                                    }
                                }
                            }
                        }
                        selectedHourPoint?.let {
                            ChartSelectionPopup(
                                title = "Selected hour",
                                primaryText = formatWholeHourLabel(it.hour),
                                secondaryText = "${formatCount(it.count)} searches"
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPeakDayPopup && peakTrendPoint != null) {
        InsightMetricPopup(
            title = "Peak day",
            primaryText = formatDayMonthForSelection(peakTrendPoint.day),
            secondaryText = "${formatCount(peakTrendPoint.count)} searches • ${formatPercent(peakDaySharePercent ?: 0.0)} share in selected month",
            onDismiss = { showPeakDayPopup = false }
        )
    }
    if (showPeakHourPopup && peakAdjustedHour != null) {
        InsightMetricPopup(
            title = "Peak hour",
            primaryText = formatWholeHourLabel(peakAdjustedHour.hour),
            secondaryText = "${formatCount(peakAdjustedHour.count)} searches (estimated per day) • ${formatPercent(peakHourSharePercent ?: 0.0)} of peak day",
            onDismiss = { showPeakHourPopup = false }
        )
    }
}

@Composable
private fun ChartSelectionPopup(
    title: String,
    primaryText: String,
    secondaryText: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderSoft)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, color = TextSecondary, fontSize = 11.sp)
            Text(primaryText, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text(secondaryText, color = AccentBlue, fontSize = 12.sp)
        }
    }
}

@Composable
private fun InsightMetricPopup(
    title: String,
    primaryText: String,
    secondaryText: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, BorderSoft),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(primaryText, color = AccentBlue, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                Text(secondaryText, color = TextSecondary, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountSyncScreen(viewModel: MarketViewModel, modifier: Modifier = Modifier) {
    val session by viewModel.userSession.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFF8FAFF), Color(0xFFEDEFFF))
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Account & Sync",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary
        )

        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Demo credentials", fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text("User login: user / passwort", color = TextSecondary)
                Text("Admin login: admin / passwort", color = TextSecondary)
            }
        }

        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (session == null) {
                    OutlinedTextField(
                        value = viewModel.username,
                        onValueChange = { viewModel.username = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = viewModel.password,
                        onValueChange = { viewModel.password = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        singleLine = true
                    )
                    Button(
                        onClick = { viewModel.signIn() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sign in")
                    }
                    if (viewModel.authError != null) {
                        Text(viewModel.authError ?: "", color = Color(0xFFB42318))
                    }
                } else {
                    Text("Signed in as ${session?.username}", color = TextPrimary)
                    Text("Role: ${session?.role}", color = AccentBlue, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.syncNow() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sync now")
                        }
                        Button(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Sign out")
                        }
                    }
                    Text(
                        text = "Background sync runs periodically and uploads unsynced searches/comparisons.",
                        color = TextSecondary,
                        textAlign = TextAlign.Start
                    )
                    if (session?.role == "admin") {
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FF))
                        ) {
                            Text(
                                text = "Admin panel: You can audit trend behavior and trigger sync manually.",
                                modifier = Modifier.padding(12.dp),
                                color = TextPrimary
                            )
                        }
                    }
                }
                if (viewModel.syncMessage != null) {
                    Text(viewModel.syncMessage ?: "", color = AccentBlue)
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = if (onClick != null) modifier.clickable(onClick = onClick) else modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = TextSecondary, fontSize = 12.sp)
            Text(value, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
