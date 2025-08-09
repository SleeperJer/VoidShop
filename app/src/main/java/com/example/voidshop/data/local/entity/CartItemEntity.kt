// app/src/main/java/com/example/voidshop/data/local/entity/CartItemEntity.kt
package com.example.voidshop.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Ítems del carrito persistidos en SQLite (Room).
 * productId + size deben ser únicos para evitar líneas duplicadas del mismo producto/talla.
 */
@Entity(
    tableName = "cart_items",
    indices = [Index(value = ["productId", "size"], unique = true)]
)
data class CartItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: String,
    val name: String,
    val price: Double,
    val imageRes: Int,
    val size: String?,     // null si no aplica talla
    val quantity: Int
)
