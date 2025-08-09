package com.example.voidshop.model

enum class Category { GAMES, ROPA, COCINA, CALZADO, JOYERIA }

data class Product(
    val id: String,
    val name: String,
    val price: Double,
    val imageRes: Int,
    val category: Category,
    val keywords: List<String> = emptyList(),
    val sizes: List<String> = emptyList()
)



