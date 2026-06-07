package com.velora.app.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.velora.app.data.model.Category
import com.velora.app.data.model.FeedItem
import com.velora.app.data.model.HeroBanner
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiModel(
    val banners: List<HeroBanner>,
    val feedItems: List<FeedItem>,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val cartRepository: CartRepository,
) : ViewModel() {

    private val _selectedCategory = MutableStateFlow(Category.ALL)
    val selectedCategory: StateFlow<Category> = _selectedCategory.asStateFlow()

    val cartItemCount: StateFlow<Int> = cartRepository.itemCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState<HomeUiModel>> = _selectedCategory
        .flatMapLatest { category ->
            combine(
                productRepository.getHeroBanners(),
                productRepository.getFeedItems(category),
            ) { banners, feed ->
                UiState.Success(HomeUiModel(banners = banners, feedItems = feed)) as UiState<HomeUiModel>
            }
        }
        .onStart { emit(UiState.Loading) }
        .catch { e -> emit(UiState.Error(e.message ?: "Something went wrong")) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UiState.Loading,
        )

    private val _events = MutableSharedFlow<String>()
    /** One-shot error messages for transient failures (e.g. favourite toggle). */
    val events = _events.asSharedFlow()

    fun selectCategory(category: Category) {
        _selectedCategory.value = category
    }

    fun toggleFavorite(productId: String) {
        // Fix: was a fire-and-forget launch with no error handling.
        // Errors are surfaced via the one-shot events channel.
        viewModelScope.launch {
            runCatching { productRepository.toggleFavorite(productId) }
                .onFailure { e ->
                    _events.emit(e.message ?: "Could not update favourite")
                }
        }
    }
}
