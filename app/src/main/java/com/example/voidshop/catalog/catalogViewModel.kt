package com.example.voidshop.catalog

import androidx.lifecycle.ViewModel
import com.example.voidshop.R
import com.example.voidshop.model.Category
import com.example.voidshop.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CatalogViewModel : ViewModel() {

    private val _products = MutableStateFlow<List<Product>>(
        listOf(
            // ===== Juegos (sin tallas) =====
            Product("1", "Auriculares Xbox", 10.99, R.drawable.ic_launcher_foreground, Category.GAMES,
                keywords = listOf("audifonos", "headset", "gaming", "xbox")),
            Product("2", "Mouse Razer DeathAdder", 15.49, R.drawable.ic_launcher_foreground, Category.GAMES,
                keywords = listOf("mouse", "razer", "gaming", "raton")),
            Product("3", "Teclado Keymove K61SE", 22.00, R.drawable.ic_launcher_foreground, Category.GAMES,
                keywords = listOf("teclado", "keyboard", "mecanico", "60%")),
            // ===== Ropa (con tallas) =====
            Product("4", "Poloche Oversize Negro", 12.90, R.drawable.ic_launcher_foreground, Category.ROPA,
                keywords = listOf("ropa","poloche","camiseta","oversize","unisex","negro"),
                sizes = listOf("XS","S","M","L","XL")),
            Product("5", "Poloche Sin Mangas Blanco", 11.50, R.drawable.ic_launcher_foreground, Category.ROPA,
                keywords = listOf("ropa","poloche","camiseta","sin mangas","blanco"),
                sizes = listOf("S","M","L")),
            Product("6", "Poloche con Mangas Azul", 13.20, R.drawable.ic_launcher_foreground, Category.ROPA,
                keywords = listOf("ropa","poloche","camiseta","con mangas","azul"),
                sizes = listOf("S","M","L","XL")),
            // ===== Cocina (sin tallas) =====
            Product("7", "Cuchillo Napi", 99.99, R.drawable.ic_launcher_foreground, Category.COCINA,
                keywords = listOf("cocina", "cuchillo", "chef")),
            Product("8", "Kit de Cucharas", 34.99, R.drawable.ic_launcher_foreground, Category.COCINA,
                keywords = listOf("cocina", "cucharas", "kit")),
            // ===== Calzado (con tallas) =====
            Product("9", "Tenis Jordan 1", 29.99, R.drawable.ic_launcher_foreground, Category.CALZADO,
                keywords = listOf("calzado", "zapatos", "tenis", "jordan"),
                sizes = listOf("38","39","40","41","42","43")),
            Product("10", "Zapatos Salvatore", 54.99, R.drawable.ic_launcher_foreground, Category.CALZADO,
                keywords = listOf("calzado", "zapatos", "salvatore", "formal"),
                sizes = listOf("39","40","41","42","43")),
            // ===== Joyería (sin tallas) =====
            Product("11", "Pulsera de Plata 925", 99.99, R.drawable.ic_launcher_foreground, Category.JOYERIA,
                keywords = listOf("joyeria", "pulsera", "plata", "925")),
            Product("12", "Cadena de Oro 23K", 199.99, R.drawable.ic_launcher_foreground, Category.JOYERIA,
                keywords = listOf("joyeria", "cadena", "oro", "23k"))
        )
    )
    val products: StateFlow<List<Product>> = _products
}
