// app/src/main/java/com/example/voidshop/data/repository/CartRepository.kt
package com.example.voidshop.data.repository

import com.example.voidshop.data.local.dao.CartDao
import com.example.voidshop.data.local.entity.CartItemEntity
import com.example.voidshop.model.Product
import kotlinx.coroutines.flow.Flow

/**
 * Lógica de acceso a datos para el carrito (persistente con Room).
 */
class CartRepository(private val dao: CartDao) {

    fun observe(): Flow<List<CartItemEntity>> = dao.observeAll()

    suspend fun add(product: Product, size: String?, qty: Int = 1) {
        val existing = dao.getByKey(product.id, size)
        val newQty = (existing?.quantity ?: 0) + qty

        val entity = CartItemEntity(
            id = existing?.id ?: 0L,
            productId = product.id,
            name = product.name,
            price = product.price,
            imageRes = product.imageRes,
            size = size,
            quantity = newQty
        )

        if (existing == null) {
            dao.insert(entity)
        } else {
            dao.update(entity)
        }
    }

    suspend fun removeOne(product: Product, size: String?) {
        val existing = dao.getByKey(product.id, size) ?: return
        val newQty = existing.quantity - 1
        if (newQty <= 0) {
            dao.delete(existing)
        } else {
            dao.update(existing.copy(quantity = newQty))
        }
    }

    suspend fun removeLine(product: Product, size: String?) {
        val existing = dao.getByKey(product.id, size) ?: return
        dao.delete(existing)
    }

    suspend fun clear() = dao.clear()
}
