package com.example.voidshop.catalog

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.voidshop.R
import com.example.voidshop.data.local.AppDatabase
import com.example.voidshop.data.repository.ProductRepository
import com.example.voidshop.model.Category
import com.example.voidshop.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CatalogViewModel(application: Application) : AndroidViewModel(application) {

    private val builtIn = MutableStateFlow(
        listOf(
            Product("1",  "Auriculares Xbox",          10.99, R.drawable.auriculares_xbox,          Category.GAMES,
                keywords = listOf("audifonos", "headset", "gaming", "xbox")),
            Product("2",  "Mouse Razer DeathAdder",     15.49, R.drawable.mouse_razer_deathadder,    Category.GAMES,
                keywords = listOf("mouse", "razer", "gaming", "raton")),
            Product("3",  "Teclado Keymove K61SE",      22.00, R.drawable.teclado_keymove_k61se,     Category.GAMES,
                keywords = listOf("teclado", "keyboard", "mecanico", "60%")),

            Product("4",  "Poloche Oversize Negro",     12.90, R.drawable.poloche_oversize_negro,    Category.ROPA,
                keywords = listOf("ropa", "poloche", "camiseta", "oversize", "unisex", "negro")),
            Product("5",  "Poloche Sin Mangas Blanco",  11.50, R.drawable.poloche_con_mangas_blanco, Category.ROPA,
                keywords = listOf("ropa", "poloche", "camiseta", "sin", "mangas", "blanco")),
            Product("6",  "Poloche con Mangas Azul",    13.20, R.drawable.poloche_mangas_azul,       Category.ROPA,
                keywords = listOf("ropa", "poloche", "camiseta", "mangas", "azul")),

            Product("7",  "Cuchillo Napi",              99.99, R.drawable.cuchillo_napi,             Category.COCINA,
                keywords = listOf("cocina", "cuchillo", "chef")),
            Product("8",  "Kit de Cucharas",            34.99, R.drawable.kit_cucharas,              Category.COCINA,
                keywords = listOf("cocina", "cucharas", "kit")),

            Product("9",  "Tenis Jordan 1",             29.99, R.drawable.jordan_1,                  Category.CALZADO,
                keywords = listOf("calzado", "zapatos", "tenis", "jordan")),
            Product("10", "Zapatos Salvatore",          54.99, R.drawable.zapatos_salvatore,         Category.CALZADO,
                keywords = listOf("calzado", "zapatos", "salvatore", "formal")),

            Product("11", "Pulsera de Plata 925",       99.99, R.drawable.pulsera_plata_925,         Category.JOYERIA,
                keywords = listOf("joyeria", "pulsera", "plata", "925")),
            Product("12", "Cadena de Oro 23K",          199.99, R.drawable.cadena_oro_23k,           Category.JOYERIA,
                keywords = listOf("joyeria", "cadena", "oro", "23k"))
        )
    )

    private val repo: ProductRepository by lazy {
        val dao = AppDatabase.get(getApplication()).productDao()
        ProductRepository(dao)
    }

    private val userProducts = repo.observeUserProducts()

    val products: StateFlow<List<Product>> =
        combine(builtIn, userProducts) { base, user ->
            user + base
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addUserProduct(
        name: String,
        price: Double,
        category: Category,
        keywords: List<String>
    ) {
        viewModelScope.launch {
            repo.addUserProduct(name, price, category, keywords)
        }
    }
}
