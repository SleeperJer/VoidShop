package com.example.voidshop.cart

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.voidshop.model.Product
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    lines: List<CartViewModel.CartLine>,
    total: Double,
    onAdd: (Product, String?) -> Unit,
    onRemoveOne: (Product, String?) -> Unit,
    onRemoveLine: (Product, String?) -> Unit,
    onClearAll: () -> Unit,
    onCheckout: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tu carrito") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Atrás")
                    }
                },
                actions = {
                    TextButton(onClick = onClearAll, enabled = lines.isNotEmpty()) {
                        Text("Vaciar")
                    }
                }
            )
        },
        bottomBar = {
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
                    Button(onClick = onCheckout, enabled = lines.isNotEmpty()) {
                        Text("Comprar")
                    }
                }
            }
        }
    ) { inner ->
        if (lines.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Text("Tu carrito está vacío")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(inner)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lines) { line ->
                    CartLineItem(
                        line = line,
                        onAdd = onAdd,
                        onRemoveOne = onRemoveOne,
                        onRemoveLine = onRemoveLine
                    )
                }
            }
        }
    }
}

@Composable
private fun CartLineItem(
    line: CartViewModel.CartLine,
    onAdd: (Product, String?) -> Unit,
    onRemoveOne: (Product, String?) -> Unit,
    onRemoveLine: (Product, String?) -> Unit
) {
    Card {
        Row(Modifier.padding(12.dp)) {
            Image(
                painter = painterResource(id = line.product.imageRes),
                contentDescription = line.product.name,
                modifier = Modifier.size(56.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(line.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(formatPrice(line.product.price), style = MaterialTheme.typography.bodyMedium)
                Text(
                    "Cantidad: ${line.quantity}  ·  Subtotal: ${formatPrice(line.lineTotal)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onAdd(line.product, line.size) }) { Text("+") }
                OutlinedButton(onClick = { onRemoveOne(line.product, line.size) }) { Text("-") }
                IconButton(onClick = { onRemoveLine(line.product, line.size) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}

private fun formatPrice(value: Double): String =
    "$" + String.format(Locale.US, "%,.2f", value)
