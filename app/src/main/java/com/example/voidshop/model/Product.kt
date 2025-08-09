package com.example.voidshop.model

import androidx.annotation.DrawableRes

enum class Category { GAMES, ROPA, COCINA, CALZADO, JOYERIA }

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    @DrawableRes val imageRes: Int,
    val category: Category,
    val keywords: List<String> = emptyList(),
    val sizes: List<String> = emptyList()
)



