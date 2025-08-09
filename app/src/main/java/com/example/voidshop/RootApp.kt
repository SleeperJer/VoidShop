package com.example.voidshop

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voidshop.cart.CartScreen
import com.example.voidshop.cart.CartViewModel
import com.example.voidshop.catalog.CatalogViewModel
import com.example.voidshop.model.Product
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootApp(
    catalogViewModel: CatalogViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel()
) {
    var showCart by remember { mutableStateOf(false) }
    var showPickerFor by remember { mutableStateOf<Product?>(null) }

    if (showCart) {
        CartScreen(
            lines = cartViewModel.items(),
            total = cartViewModel.total(),
            onAdd = { p, size -> cartViewModel.add(p, size) },
            onRemoveOne = { p, size -> cartViewModel.removeOne(p, size) },
            onRemoveLine = { p, size -> cartViewModel.removeLine(p, size) },
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
                title = { Text("VoidShop") },
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
                placeholder = { Text("Que es lo que le interesa buscar el dia de hoy") },
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
                        onAddToCart = {
                            if (p.sizes.isEmpty()) {
                                cartViewModel.add(p)
                            } else {
                                showPickerFor = p
                            }
                        }
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

            if (showPickerFor != null) {
                SizeQuantityDialog(
                    product = showPickerFor!!,
                    onDismiss = { showPickerFor = null },
                    onConfirm = { size, qty ->
                        cartViewModel.add(showPickerFor!!, size = size, qty = qty)
                        showPickerFor = null
                    }
                )
            }
        }
    }
}

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
                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(formatPrice(p.price), style = MaterialTheme.typography.bodyMedium)
                if (p.sizes.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Tallas disponibles: ${p.sizes.joinToString()}", style = MaterialTheme.typography.bodySmall)
                }
            }
            Button(onClick = onAddToCart) { Text("Agregar") }
        }
    }
}

@Composable
fun CartBottomBar(total: Double, onCheckout: () -> Unit, enabled: Boolean) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Total: ${formatPrice(total)}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Button(onClick = onCheckout, enabled = enabled) { Text("Comprar") }
        }
    }
}

private fun formatPrice(value: Double): String =
    "$" + String.format(Locale.US, "%,.2f", value)

/* ---------------------- Selector talla + cantidad (sin APIs experimentales) ---------------------- */

@Composable
fun SizeQuantityDialog(
    product: Product,
    onDismiss: () -> Unit,
    onConfirm: (size: String, qty: Int) -> Unit
) {
    var selectedSize by remember { mutableStateOf(product.sizes.firstOrNull().orEmpty()) }
    var qty by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Selecciona talla")

                // Reemplazo de FlowRow → LazyRow (no experimental)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(product.sizes) { size ->
                        FilterChip(
                            selected = selectedSize == size,
                            onClick = { selectedSize = size },
                            label = { Text(size) }
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text("Cantidad")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = qty > 1, onClick = { qty-- }) { Text("-") }
                    Text(qty.toString(), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { qty++ }) { Text("+") }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = selectedSize.isNotBlank(),
                onClick = { onConfirm(selectedSize, qty) }
            ) { Text("Agregar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
