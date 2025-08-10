// app/src/main/java/com/example/voidshop/data/repository/ProductRepository.kt
package com.example.voidshop.data.repository

import com.example.voidshop.R
import com.example.voidshop.data.local.dao.ProductDao
import com.example.voidshop.data.local.entity.ProductEntity
import com.example.voidshop.model.Category
import com.example.voidshop.model.Product
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ProductRepository(private val dao: ProductDao) {

    /** Flujo de productos de usuario convertidos a modelo de UI. */
    fun observeUserProducts(): Flow<List<Product>> =
        dao.observeAll().map { list -> list.map { it.toModel() } }

    /** Inserta un producto de usuario. Devuelve el productId generado. */
    suspend fun addUserProduct(
        name: String,
        price: Double,
        category: Category,
        keywords: List<String>
    ): String {
        val productId = "U-" + UUID.randomUUID().toString()
        val entity = ProductEntity(
            productId = productId,
            name = name,
            price = price,
            imageRes = R.drawable.ic_launcher_foreground, // placeholder
            category = category.name,
            keywords = keywords.joinToString(",") { it.trim() }
        )
        dao.insert(entity)
        return productId
    }

    private fun ProductEntity.toModel(): Product =
        Product(
            id = productId,
            name = name,
            price = price,
            imageRes = imageRes,
            category = Category.valueOf(category),
            keywords = if (keywords.isBlank()) emptyList() else keywords.split(",").map { it.trim() },
            sizes = emptyList()
        )
}
