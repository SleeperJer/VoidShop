package com.example.voidshop.cart

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.example.voidshop.model.Product

class CartViewModel : ViewModel() {

    // clave por producto + talla (si aplica)
    data class Key(val productId: String, val size: String?)

    private val quantities = mutableStateMapOf<Key, Int>()
    private val products   = mutableStateMapOf<Key, Product>()

    /** Agrega qty unidades del producto (por talla si aplica). */
    fun add(p: Product, size: String? = null, qty: Int = 1) {
        val k = Key(p.id, size)
        products[k] = p
        quantities[k] = (quantities[k] ?: 0) + qty
    }

    /** Resta 1; si llega a 0 elimina la línea. */
    fun removeOne(p: Product, size: String? = null) {
        val k = Key(p.id, size)
        val q = (quantities[k] ?: 0) - 1
        if (q <= 0) {
            quantities.remove(k)
            products.remove(k)
        } else {
            quantities[k] = q
        }
    }

    /** Elimina la línea completa (producto + talla). */
    fun removeLine(p: Product, size: String? = null) {
        val k = Key(p.id, size)
        quantities.remove(k)
        products.remove(k)
    }

    /** Vacía todo el carrito. */
    fun clear() {
        quantities.clear()
        products.clear()
    }

    /** Ítems totales (sumatoria de cantidades). */
    fun count(): Int = quantities.values.sum()

    /** Total $ del carrito. */
    fun total(): Double = quantities.entries.sumOf { (k, q) ->
        (products[k]?.price ?: 0.0) * q
    }

    /** Línea para UI. */
    data class CartLine(
        val product: Product,
        val size: String?,
        val quantity: Int
    ) {
        val lineTotal: Double get() = product.price * quantity
        val label: String get() = if (size.isNullOrBlank()) product.name else "${product.name} · Talla $size"
    }

    /** Lista observable (se recalcula a partir del estado). */
    fun items(): List<CartLine> =
        quantities.mapNotNull { (k, q) ->
            products[k]?.let { CartLine(it, k.size, q) }
        }
}
