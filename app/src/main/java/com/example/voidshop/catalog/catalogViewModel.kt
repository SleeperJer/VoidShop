package com.example.voidshop.catalog

import androidx.lifecycle.ViewModel
import com.example.voidshop.R
import com.example.voidshop.model.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class CatalogViewModel : ViewModel() {
    private val _products = MutableStateFlow<List<Product>>(
        listOf(
            Product(
                id = "1",
                name = "Auriculares Xbox",
                price = 10.99,
                imageRes = R.drawable.ic_launcher_foreground
            ),
            Product(
                id = "2",
                name = "Mouse Razer DeathAdder",
                price = 15.49,
                imageRes = R.drawable.ic_launcher_foreground
            ),
            Product(
                id = "3",
                name = "Teclado Keymove K61SE",
                price = 22.00,
                imageRes = R.drawable.ic_launcher_foreground
            ),
            Product(
                id = "4",
                name = "Teclado Logitech G213",
                price = 26.00,
                imageRes = R.drawable.ic_launcher_foreground
            )
        )
    )

    val products: StateFlow<List<Product>> = _products

}