package com.velora.app.feature.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velora.app.data.model.Product
import com.velora.app.data.model.UiState
import com.velora.app.data.repository.CartRepository
import com.velora.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProductEvent {
    data class Error(val message: String) : ProductEvent
}

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val _productId = MutableStateFlow<String?>(null)

    private val _selectedSize = MutableStateFlow<String?>(null)
    val selectedSize: StateFlow<String?> = _selectedSize.asStateFlow()

    private val _events = MutableSharedFlow<ProductEvent>()
    val events = _events.asSharedFlow()

    /**
     * Fix: was using `stateIn(...).value` inside a `map {}` — this created a
     * new Eagerly-started StateFlow on every emission and synchronously read
     * `.value` which is always null before the first emission. Now uses
     * `flatMapLatest` to properly chain the upstream product Flow.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<Product>> = _productId
        .flatMapLatest { id ->
            if (id == null) {
                kotlinx.coroutines.flow.flowOf(UiState.Loading)
            } else {
                productRepository.getProduct(id)
                    .map<Product?, UiState<Product>> { product ->
                        if (product != null) UiState.Success(product)
                        else UiState.Error("Product not found")
                    }
            }
        }
        .onStart { emit(UiState.Loading) }
        .catch { e -> emit(UiState.Error(e.message ?: "Error loading product")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    /**
     * Fix: was a one-shot synchronous read via `cartRepository.contains(id)`.
     * Now combines `_productId` with the live `cartRepository.items` Flow so
     * the badge updates whenever items are added/removed while this screen is open.
     */
    val isInCart: StateFlow<Boolean> = combine(
        _productId,
        cartRepository.items,
    ) { id, items ->
        id != null && items.any { it.product.id == id }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun loadProduct(productId: String) { _productId.value = productId }
    fun selectSize(size: String) { _selectedSize.value = size }
    fun addToCart(product: Product) { cartRepository.addItem(product, _selectedSize.value ?: "") }

    /**
     * Fix: was a fire-and-forget launch with no error handling.
     * Errors are now surfaced via the events channel.
     */
    fun toggleFavorite() {
        val id = _productId.value ?: return
        viewModelScope.launch {
            runCatching { productRepository.toggleFavorite(id) }
                .onFailure { e ->
                    _events.emit(ProductEvent.Error(e.message ?: "Could not update favourite"))
                }
        }
    }
}
