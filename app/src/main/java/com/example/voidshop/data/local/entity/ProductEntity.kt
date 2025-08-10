// app/src/main/java/com/example/voidshop/data/local/entity/ProductEntity.kt
package com.example.voidshop.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [Index(value = ["productId"], unique = true)]
)
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: String,           // UUID/string único para mapear al modelo UI
    val name: String,
    val price: Double,
    val imageRes: Int,
    val category: String,            // Category.name
    val keywords: String             // "palabra1,palabra2,..."
)
