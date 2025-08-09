package com.example.voidshop.cart

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voidshop.data.local.AppDatabase
import com.example.voidshop.data.local.entity.CartItemEntity
import com.example.voidshop.data.repository.CartRepository
import com.example.voidshop.model.Product
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CartViewModel(application: Application) : AndroidViewModel(application) {

    // Línea para UI (igual que antes)
    data class CartLine(
        val product: Product,
        val size: String?,
        val quantity: Int
    ) {
        val lineTotal: Double get() = product.price * quantity
        val label: String get() = if (size.isNullOrBlank()) product.name else "${product.name} · Talla $size"
    }

    private val repo: CartRepository by lazy {
        val dao = AppDatabase.get(getApplication()).cartDao()
        CartRepository(dao)
    }

    // Mapeo de Entity -> CartLine (recuperamos Product “ligero” para UI)
    private fun entityToProduct(e: CartItemEntity): Product =
        Product(
            id = e.productId,
            name = e.name,
            price = e.price,
            imageRes = e.imageRes,
            category = com.example.voidshop.model.Category.ROPA, // NO usado para carrito
            keywords = emptyList(),
            sizes = emptyList()
        )

    private val _lines: StateFlow<List<CartLine>> =
        repo.observe()
            .map { list ->
                list.map { e ->
                    CartLine(
                        product = entityToProduct(e),
                        size = e.size,
                        quantity = e.quantity
                    )
                }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val lines: StateFlow<List<CartLine>> = _lines

    val count: StateFlow<Int> =
        _lines.map { it.sumOf { l -> l.quantity } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val total: StateFlow<Double> =
        _lines.map { it.sumOf { l -> l.lineTotal } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    /** API pública igual que antes (pero persistente) */
    fun add(p: Product, size: String? = null, qty: Int = 1) {
        viewModelScope.launch { repo.add(p, size, qty) }
    }

    fun removeOne(p: Product, size: String? = null) {
        viewModelScope.launch { repo.removeOne(p, size) }
    }

    fun removeLine(p: Product, size: String? = null) {
        viewModelScope.launch { repo.removeLine(p, size) }
    }

    fun clear() {
        viewModelScope.launch { repo.clear() }
    }

    // Métodos “compat” si los usabas así:
    fun items(): List<CartLine> = lines.value
    fun count(): Int = count.value
    fun total(): Double = total.value
}
