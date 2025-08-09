package com.example.voidshop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voidshop.cart.CartViewModel
import com.example.voidshop.catalog.CatalogViewModel
import com.example.voidshop.model.Product
import java.util.Locale
import com.example.voidshop.cart.CartScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
//aqui es la pantalla principal donde se ve todo en la app es decir por este podemos buscar y agregar en pocas palabras de aca para abajo
fun RootApp(
    catalogViewModel: CatalogViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel()
) {
    var showCart by remember { mutableStateOf(false) }
    if (showCart) {
        CartScreen(
            lines = cartViewModel.items(),
            total = cartViewModel.total(),
            onAdd = { cartViewModel.add(it) },
            onRemoveOne = { cartViewModel.removeOne(it) },
            onRemoveLine = { cartViewModel.removeLine(it) },
            onClearAll = { cartViewModel.clear() },
            onCheckout = { /* TODO: pago */ },
            onBack = { showCart = false }
        )
        return
    }
    val products by catalogViewModel.products.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = remember(products, query) {
        val q = query.trim()
        if (q.isBlank()) products
        else products.filter { p ->
            p.name.contains(q, ignoreCase = true) ||
                    p.keywords.any { it.contains(q, ignoreCase = true) }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { GalaxyTitle() },
                actions = {
                    val count = cartViewModel.count()
                    BadgedBox(badge = { if (count > 0) Badge { Text(count.toString()) } }) {
                        IconButton(onClick = { showCart = true }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                        }
                    }
                }
            )
        },
        bottomBar = {
            CartBottomBar(
                total = cartViewModel.total(),
                onCheckout = { showCart = true },
                enabled = cartViewModel.count() > 0
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                placeholder = { Text("Que desea buscar hoy?") },//este seria la barra de busqueda donde usamos la keywords para buscar en especifico
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar")
                        }
                    }
                }
            )
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { p: Product ->
                    ProductCard(
                        p = p,
                        onAddToCart = { cartViewModel.add(p) }
                    )
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            "Sin resultados para “$query”.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }
}

//por si quieren cambiar el texto color o letra aqui esta
@Composable
private fun GalaxyTitle() {
    Text(
        text = "VoidShop",
        style = TextStyle(
            fontFamily = FontFamily.Serif, // Fuente nativa similar a Times New Roman
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF6A00FF),
                    Color(0xFFE91E63),
                    Color(0xFF00BCD4),
                    Color(0xFFFFEB3B)
                )
            )
        )
    )
}
//aqui es para ver los articulos en el catalogo y poder agregarlos
@Composable
fun ProductCard(p: Product, onAddToCart: () -> Unit) {
    Card {
        Row(Modifier.padding(12.dp)) {
            Image(
                painter = painterResource(id = p.imageRes),
                contentDescription = p.name,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(formatPrice(p.price), style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onAddToCart) { Text("Agregar") }
        }
    }
}//esta parte es la que se encarga de permitir ver el total del acumulado de los articulos que ve abajo
@Composable
fun CartBottomBar(total: Double, onCheckout: () -> Unit, enabled: Boolean) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Total: ${formatPrice(total)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(onClick = onCheckout, enabled = enabled) { Text("Comprar") }
        }
    }
}
private fun formatPrice(value: Double): String =
    "$" + String.format(Locale.US, "%,.2f", value)
