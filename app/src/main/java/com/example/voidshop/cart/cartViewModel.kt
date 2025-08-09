package com.example.voidshop.cart
import androidx.lifecycle.ViewModel
import androidx.compose.runtime.mutableStateMapOf
import com.example.voidshop.model.Product
class CartViewModel : ViewModel() {
    private val quantities = mutableStateMapOf<String, Int>()
    private val products = mutableStateMapOf<String, Product>()
    fun add(p: Product) {
        products[p.id] = p
        quantities[p.id] = (quantities[p.id] ?: 0) + 1
    }
    fun removeOne(p: Product) {
        val q = (quantities[p.id] ?: 0) - 1
        if (q <= 0) { quantities.remove(p.id); products.remove(p.id) }
        else quantities[p.id] = q
    }
    fun count(): Int = quantities.values.sum()
    fun total(): Double = quantities.entries.sumOf { (id, q) -> (products[id]?.price ?: 0.0) * q }
}
