package com.example.voidshop

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton   // 👈 IMPORT CLAVE
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voidshop.model.Product
import kotlin.math.roundToInt

// Datos de detalle “tipo Amazon”
data class ProductDetails(
    val images: List<Int>,
    val description: String,
    val rating: Double,   // 0..5
    val reviews: Int,
    val highlights: List<String> = emptyList()
)

// Repositorio fake de detalles (cámbialo cuando tengas backend)
object ProductDetailsRepo {
    fun get(product: Product): ProductDetails {
        val pics = listOf(product.imageRes, product.imageRes, product.imageRes) // placeholder
        return ProductDetails(
            images = pics,
            description = "Artículo ${product.name} de alta calidad. Ideal para uso diario.",
            rating = 4.6,
            reviews = 213,
            highlights = listOf(
                "Envío rápido y devoluciones fáciles",
                "Garantía oficial 12 meses",
                "Materiales resistentes y duraderos"
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    product: Product,
    details: ProductDetails,
    cartCount: Int,
    onAddToCart: (Product, Int) -> Unit,
    onBuyNow: (Product, Int) -> Unit,
    onBack: () -> Unit,
    onOpenCart: () -> Unit
) {
    var selected by remember { mutableStateOf(0) }
    var qty by remember { mutableStateOf(1) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(product.name, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) {
                        IconButton(onClick = onOpenCart) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                        }
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 12.dp)
        ) {
            val main = details.images.getOrNull(selected) ?: product.imageRes
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(main),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(details.images) { index, res ->
                    val selectedBorder = if (index == selected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.outlineVariant
                    Image(
                        painter = painterResource(res),
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .clip(MaterialTheme.shapes.small)
                            .border(2.dp, selectedBorder, MaterialTheme.shapes.small)
                            .clickable { selected = index },
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(formatPrice(product.price), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)

            Spacer(Modifier.height(6.dp))

            RatingRow(rating = details.rating, reviews = details.reviews)

            Spacer(Modifier.height(12.dp))

            if (details.highlights.isNotEmpty()) {
                Text("Acerca de este artículo", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    details.highlights.forEach { bullet ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text("•  ", style = MaterialTheme.typography.bodyMedium)
                            Text(bullet, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            QuantitySelector(qty = qty, onChange = { qty = it })

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onAddToCart(product, qty) },
                    modifier = Modifier.weight(1f)
                ) { Text("Agregar al carrito") }

                FilledTonalButton(
                    onClick = { onBuyNow(product, qty) },
                    modifier = Modifier.weight(1f)
                ) { Text("Comprar ahora") }
            }

            Spacer(Modifier.height(16.dp))

            // Descripción
            Text("Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Text(details.description, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RatingRow(rating: Double, reviews: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("${"%.1f".format(rating)}  |  ${reviews} reseñas", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun Stars(rating: Double) {
    val full = rating.roundToInt() // placeholder simple
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { i ->
            val icon = if (i < full) R.drawable.ic_launcher_foreground else R.drawable.ic_launcher_background
            // Reemplaza por drawables de estrella si los tienes
            Image(
                painter = painterResource(icon),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                contentScale = ContentScale.Fit
            )
        }
    }
}

@Composable
private fun QuantitySelector(qty: Int, onChange: (Int) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = { if (qty > 1) onChange(qty - 1) }) { Text("-") }
        Text("$qty", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { onChange(qty + 1) }) { Text("+") }
    }
}
