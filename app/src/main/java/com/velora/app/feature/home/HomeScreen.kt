package com.velora.app.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.itemsIndexed
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apexads.sdk.core.models.AdSize
import com.velora.app.VeloraApplication
import com.velora.app.ads.BannerAdSlot
import com.velora.app.ads.NativeAdCard
import com.velora.app.data.model.Category
import com.velora.app.data.model.FeedItem
import com.velora.app.data.model.UiState
import com.velora.app.feature.home.components.CategoryChips
import com.velora.app.feature.home.components.HeroCarousel
import com.velora.app.feature.home.components.HomeShimmer
import com.velora.app.feature.home.components.ProductCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onProductClick: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val cartCount by viewModel.cartItemCount.collectAsStateWithLifecycle()
    val gridState = rememberLazyStaggeredGridState()

    val isScrolled by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 20 }
    }
    val topBarAlpha by animateFloatAsState(
        targetValue = if (isScrolled) 1f else 0f,
        animationSpec = tween(300),
        label = "top_bar_alpha",
    )

    Box(modifier = Modifier.fillMaxSize()) {
        when (val state = uiState) {
            is UiState.Loading -> HomeShimmer()
            is UiState.Success -> {
                LazyVerticalStaggeredGrid(
                    state = gridState,
                    columns = StaggeredGridCells.Fixed(2),
                    contentPadding = PaddingValues(
                        start = 12.dp, end = 12.dp, top = 72.dp, bottom = 16.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalItemSpacing = 12.dp,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        HeroCarousel(
                            banners = state.data.banners,
                            onCtaClick = { banner ->
                                banner.targetCategory?.let { viewModel.selectCategory(it) }
                            },
                        )
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        CategoryChips(
                            categories = Category.entries,
                            selected = selectedCategory,
                            onSelect = viewModel::selectCategory,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        )
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        SectionHeader(
                            title = if (selectedCategory == Category.ALL) "New Arrivals"
                                    else selectedCategory.label,
                            itemCount = state.data.feedItems.count { it is FeedItem.ProductItem },
                        )
                    }
                    // Fix: was using indexOf(item) inside the key lambda — O(n) per item
                    // per recomposition. Now uses itemsIndexed so the stable index is
                    // available directly without a linear scan.
                    itemsIndexed(
                        items = state.data.feedItems,
                        key = { index, item ->
                            when (item) {
                                is FeedItem.ProductItem -> item.product.id
                                is FeedItem.NativeAdSlot -> "native_ad_$index"
                            }
                        },
                    ) { _, item ->
                        when (item) {
                            is FeedItem.ProductItem -> ProductCard(
                                product = item.product,
                                onClick = { onProductClick(it.id) },
                                onFavoriteToggle = viewModel::toggleFavorite,
                            )
                            is FeedItem.NativeAdSlot -> NativeAdCard(modifier = Modifier.fillMaxWidth())
                        }
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        BannerAdSlot(
                            placementId = VeloraApplication.PLACEMENT_HOME_BANNER,
                            adSize = AdSize.BANNER_320x50,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    }
                }
            }
            is UiState.Error -> ErrorState(message = state.message)
        }

        TopBar(
            cartCount = cartCount,
            backgroundAlpha = topBarAlpha,
            modifier = Modifier.align(Alignment.TopCenter),
        )
    }
}

@Composable
private fun TopBar(
    cartCount: Int,
    backgroundAlpha: Float,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface.copy(alpha = backgroundAlpha))
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "VELORA",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = {}) {
            Icon(Icons.Outlined.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurface)
        }
        BadgedBox(
            badge = {
                if (cartCount > 0) Badge { Text(cartCount.coerceAtMost(99).toString()) }
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.ShoppingCart,
                contentDescription = "Cart",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun SectionHeader(title: String, itemCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        TextButton(onClick = {}) {
            Text(
                text = "See all ($itemCount)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun ErrorState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Oops!", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
