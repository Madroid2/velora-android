package com.velora.app.data.repository

import com.velora.app.data.model.Category
import com.velora.app.data.model.FeedItem
import com.velora.app.data.model.HeroBanner
import com.velora.app.data.model.Product
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory fake repository — simulates a 600 ms network round-trip on first
 * load. Replace with a Retrofit + Room implementation following the same
 * [ProductRepository] contract; ViewModels never need to change.
 */
@Singleton
class FakeProductRepository @Inject constructor() : ProductRepository {

    private fun img(seed: String, w: Int = 400, h: Int = 560) =
        "https://picsum.photos/seed/$seed/$w/$h"

    private val allProducts = listOf(
        Product("1",  "Leather Crossbody",    "Maison Veloré", "Full-grain Italian leather with adjustable strap. Hand-stitched edges.",       128.00, 180.00, Category.BAGS,        4.8f, 312, img("bag-crossbody"),  listOf("XS","S","M"),              badge = "Sale"),
        Product("2",  "Merino Turtleneck",    "Velora Studio", "Ultra-fine 18.5-micron Merino. Relaxed fit, ribbed cuffs and hem.",             94.00,  null,   Category.KNITWEAR,    4.7f, 218, img("knit-turtle"),    listOf("XS","S","M","L","XL")),
        Product("3",  "Oversized Wool Coat",  "Maison Veloré", "Double-faced wool blend. Dropped shoulders, satin-lined.",                      285.00, null,   Category.OUTERWEAR,   4.9f, 94,  img("coat-wool"),      listOf("XS","S","M","L"),          badge = "New"),
        Product("4",  "Suede Chelsea Boots",  "Velora Shoes",  "Premium suede upper, cushioned leather insole, rubber sole.",                   195.00, null,   Category.FOOTWEAR,    4.6f, 156, img("boots-chelsea"),  listOf("36","37","38","39","40","41")),
        Product("5",  "Silk Scarf",           "Velora Studio", "100% silk twill. Hand-rolled edges. 90×90 cm.",                                 68.00,  null,   Category.ACCESSORIES, 4.5f, 203, img("scarf-silk"),     emptyList()),
        Product("6",  "Bucket Tote",          "Maison Veloré", "Pebbled calf leather, interior zip pocket, adjustable drawstring.",             158.00, null,   Category.BAGS,        4.7f, 187, img("bag-bucket"),     listOf("One Size")),
        Product("7",  "Cashmere Crewneck",    "Velora Studio", "Grade-A Mongolian cashmere. Garment-washed for softness.",                      142.00, null,   Category.KNITWEAR,    4.9f, 421, img("knit-cashmere"),  listOf("XS","S","M","L","XL"),     badge = "Bestseller"),
        Product("8",  "Leather Loafers",      "Velora Shoes",  "Hand-burnished calfskin. Classic penny strap. Leather-lined.",                  220.00, null,   Category.FOOTWEAR,    4.8f, 98,  img("shoes-loafer"),   listOf("36","37","38","39","40","41","42")),
        Product("9",  "Trench Coat",          "Maison Veloré", "Water-resistant cotton gabardine. Storm flap, gun patch, epaulettes.",           340.00, null,   Category.OUTERWEAR,   4.9f, 67,  img("coat-trench"),    listOf("XS","S","M","L"),          badge = "New"),
        Product("10", "Silver Cuff Bracelet", "Velora Gold",   "Solid sterling silver. Brushed finish. 6 mm wide.",                             58.00,  null,   Category.ACCESSORIES, 4.4f, 334, img("jewelry-cuff"),   emptyList()),
        Product("11", "Mini Crossbody",       "Maison Veloré", "Structured lambskin. Detachable chain strap. Interior card slots.",              115.00, 160.00, Category.BAGS,        4.6f, 245, img("bag-mini"),       listOf("One Size"),                badge = "Sale"),
        Product("12", "Ribbed Cardigan",      "Velora Studio", "Sustainable cotton blend. Button-front, patch pockets.",                         88.00,  null,   Category.KNITWEAR,    4.5f, 178, img("knit-cardigan"),  listOf("XS","S","M","L")),
        Product("13", "Quilted Jacket",       "Maison Veloré", "90/10 down fill. Matte shell, reversible design.",                               178.00, null,   Category.OUTERWEAR,   4.7f, 129, img("jacket-quilted"), listOf("XS","S","M","L","XL")),
        Product("14", "Slingback Heels",      "Velora Shoes",  "Nappa leather upper. 65 mm block heel. Adjustable slingback.",                   168.00, null,   Category.FOOTWEAR,    4.5f, 112, img("shoes-heels"),    listOf("36","37","38","39","40")),
        Product("15", "Wool Beret",           "Velora Studio", "100% virgin wool. Structured crown. One size fits most.",                         42.00,  null,   Category.ACCESSORIES, 4.6f, 289, img("hat-beret"),      emptyList()),
        Product("16", "Clutch Evening Bag",   "Maison Veloré", "Satin-covered frame bag. Rhinestone clasp. Detachable wristlet chain.",           95.00,  null,   Category.BAGS,        4.8f, 73,  img("bag-clutch"),     listOf("One Size"),                badge = "New"),
    )

    private val favorites = MutableStateFlow(setOf<String>())

    private val allProductsFlow: Flow<List<Product>> = favorites.map { favs ->
        allProducts.map { it.copy(isFavorite = it.id in favs) }
    }

    override fun getHeroBanners(): Flow<List<HeroBanner>> = flow {
        delay(400)
        emit(listOf(
            HeroBanner("h1", "New Season\nArrivals",  "Curated pieces for the discerning wardrobe",
                "Shop Now", 0xFF1A237E, 0xFF283593, img("hero-fashion-1", 800, 480), Category.OUTERWEAR),
            HeroBanner("h2", "The Bag\nEdit",         "Iconic shapes in premium leathers",
                "Discover",  0xFF1B0000, 0xFF4A1010, img("hero-bags-2",    800, 480), Category.BAGS),
            HeroBanner("h3", "Effortless\nKnitwear",  "Cashmere, Merino and beyond",
                "Explore",   0xFF003300, 0xFF1B4332, img("hero-knit-3",    800, 480), Category.KNITWEAR),
        ))
    }

    override fun getProducts(category: Category): Flow<List<Product>> =
        allProductsFlow.map { if (category == Category.ALL) it else it.filter { p -> p.category == category } }

    override fun getFeedItems(category: Category): Flow<List<FeedItem>> =
        getProducts(category).map { products ->
            buildList {
                products.forEachIndexed { index, product ->
                    add(FeedItem.ProductItem(product))
                    if ((index + 1) % 6 == 0) add(FeedItem.NativeAdSlot)
                }
            }
        }

    override fun getProduct(id: String): Flow<Product?> =
        allProductsFlow.map { it.find { p -> p.id == id } }

    override suspend fun toggleFavorite(productId: String) {
        // Fix: was a non-atomic read-modify-write (`favorites.value = favorites.value.toMutableSet()…`).
        // Two concurrent callers could both read the same snapshot and one update would be lost.
        // MutableStateFlow.update {} is atomic — it retries on CAS failure.
        favorites.update { current ->
            if (productId in current) current - productId else current + productId
        }
    }
}
