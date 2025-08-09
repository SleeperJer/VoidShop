package com.example.voidshop.cart

import androidx.compose.runtime.mutableStateMapOf
import androidx.lifecycle.ViewModel
import com.example.voidshop.model.Product

class CartViewModel : ViewModel() {

    //partes importantes tener cuidado que mediante con lo que sigue son las funciones para borrar y aññadir del carrito
    private val quantities = mutableStateMapOf<String, Int>()
    //llegar rapido al objeto del carrito
    private val products = mutableStateMapOf<String, Product>()
    fun add(p: Product) {
        products[p.id] = p
        quantities[p.id] = (quantities[p.id] ?: 0) + 1
    }
    /** Quita 1 unidad; si llega a 0, elimina la línea */
    fun removeOne(p: Product) {
        val q = (quantities[p.id] ?: 0) - 1
        if (q <= 0) {
            quantities.remove(p.id)
            products.remove(p.id)
        } else {
            quantities[p.id] = q
        }
    }
   //funcion para borrar
    fun removeLine(p: Product) {
        quantities.remove(p.id)
        products.remove(p.id)
    }
    /** Vacía todo el carrito */
    fun clear() {
        quantities.clear()
        products.clear()
    }
    /** Cantidad total de ítems */
    fun count(): Int = quantities.values.sum()
    /** Total en dinero */
    fun total(): Double = quantities.entries.sumOf { (id, q) ->
        (products[id]?.price ?: 0.0) * q
    }
    /** Líneas para mostrar en la UI */
    data class CartLine(val product: Product, val quantity: Int) {
        val lineTotal: Double get() = product.price * quantity
    }
    /** Lista observable (se recalcula al leer; quantities es snapshot state) */
    fun items(): List<CartLine> =
        quantities.mapNotNull { (id, q) ->
            products[id]?.let { CartLine(it, q) }
        }
}
