package com.velora.app.feature.cart

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.apexads.sdk.core.error.AdError
import com.apexads.sdk.video.VideoAd
import com.apexads.sdk.video.VideoAdListener
import com.velora.app.VeloraApplication
import com.velora.app.core.design.ErrorRed
import com.velora.app.core.design.SuccessGreen
import com.velora.app.data.model.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(viewModel: CartViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var discountApplied by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        Text("Your Cart", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp))

        if (uiState.items.isEmpty()) { EmptyCart(); return@Column }

        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            items(uiState.items, key = { "${it.product.id}_${it.selectedSize}" }) { item ->
                SwipeToDeleteItem(
                    item = item,
                    onDismiss = { viewModel.removeItem(item.product.id) },
                    onIncrement = { viewModel.increment(item.product.id) },
                    onDecrement = { viewModel.decrement(item.product.id) },
                )
            }
            item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }
            item {
                if (!discountApplied) {
                    RewardedVideoOffer(onWatchAd = {
                        val activity = context as? androidx.activity.ComponentActivity ?: return@RewardedVideoOffer
                        // Build the ad and keep a reference so we can cancel the delayed
                        // show if the Activity is destroyed before the 800 ms fires.
                        val ad = VideoAd.Builder(VeloraApplication.PLACEMENT_CART_REWARDED)
                            .listener(object : VideoAdListener {
                                override fun onVideoAdLoaded() {}
                                override fun onVideoAdFailed(error: AdError) {}
                                override fun onRewardEarned() {
                                    viewModel.applyRewardedDiscount()
                                    discountApplied = true
                                }
                            })
                            .build()
                        ad.load()
                        // Hold the Runnable reference so it can be removed from the
                        // message queue in DisposableEffect if the Composable leaves.
                        val showRunnable = Runnable {
                            if (!activity.isDestroyed && !activity.isFinishing) {
                                ad.show(activity)
                            }
                        }
                        val decorView = activity.window.decorView
                        decorView.postDelayed(showRunnable, 800)
                        // Attach cleanup: cancel the pending show and destroy the ad
                        // if this Composable leaves composition before the timer fires.
                        activity.lifecycle.addObserver(
                            object : androidx.lifecycle.DefaultLifecycleObserver {
                                override fun onDestroy(owner: androidx.lifecycle.LifecycleOwner) {
                                    decorView.removeCallbacks(showRunnable)
                                    owner.lifecycle.removeObserver(this)
                                }
                            }
                        )
                    })
                } else {
                    DiscountAppliedBanner()
                }
            }
        }

        Surface(tonalElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding()
                    .animateContentSize(),
            ) {
                SummaryRow("Subtotal", "$${"%.2f".format(uiState.subtotal)}")
                if (uiState.isDiscountApplied) {
                    SummaryRow("Discount (20%)", "-$${"%.2f".format(uiState.subtotal * 0.20)}", valueColor = SuccessGreen)
                }
                SummaryRow("Shipping", "Free", valueColor = SuccessGreen)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                SummaryRow("Total", "$${"%.2f".format(uiState.total)}", bold = true)
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text("Proceed to Checkout", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    item: CartItem,
    onDismiss: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) { onDismiss(); true } else false
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                ErrorRed else MaterialTheme.colorScheme.surfaceVariant
            Box(Modifier.fillMaxSize().background(color).padding(end = 24.dp), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Color.White)
            }
        },
        content = {
            Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = item.product.imageUrl, contentDescription = item.product.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(72.dp).clip(MaterialTheme.shapes.medium),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.product.brand, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary)
                        Text(item.product.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                        if (item.selectedSize.isNotEmpty()) {
                            Text("Size: ${item.selectedSize}", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("$${item.product.price.toInt()}", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FilledTonalButton(onClick = onDecrement,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            modifier = Modifier.size(32.dp), shape = MaterialTheme.shapes.extraLarge) {
                            Icon(Icons.Outlined.Remove, null, modifier = Modifier.size(16.dp))
                        }
                        Text(item.quantity.toString(), style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(horizontal = 10.dp))
                        FilledTonalButton(onClick = onIncrement,
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                            modifier = Modifier.size(32.dp), shape = MaterialTheme.shapes.extraLarge) {
                            Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun RewardedVideoOffer(onWatchAd: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.medium),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Get 20% off your order", style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text("Watch a short ad to unlock your discount", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onWatchAd, shape = MaterialTheme.shapes.extraLarge) {
                Icon(Icons.Filled.PlayCircle, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Watch")
            }
        }
    }
}

@Composable
private fun DiscountAppliedBanner() {
    Surface(
        color = SuccessGreen.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.medium),
    ) {
        Text("✓  20% discount applied!", style = MaterialTheme.typography.titleSmall,
            color = SuccessGreen, modifier = Modifier.padding(12.dp))
    }
}

@Composable
private fun SummaryRow(label: String, value: String, bold: Boolean = false, valueColor: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface)
        Text(value, style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            color = if (valueColor == Color.Unspecified) MaterialTheme.colorScheme.onSurface else valueColor,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun EmptyCart() {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.ShoppingCart, null, modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.outline)
            Spacer(Modifier.height(16.dp))
            Text("Your cart is empty", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text("Discover something beautiful", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
