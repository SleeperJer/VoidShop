// RootApp.kt
package com.example.voidshop

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.voidshop.cart.CartScreen
import com.example.voidshop.cart.CartViewModel
import com.example.voidshop.catalog.CatalogViewModel
import com.example.voidshop.model.Category
import com.example.voidshop.model.Product
import kotlinx.coroutines.flow.collectLatest
import java.util.Locale
import kotlin.math.max
import kotlin.random.Random

enum class SortOption { NONE, PRICE_ASC, PRICE_DESC, NAME }
enum class PaymentMethod { CARD, COD }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootApp(
    catalogViewModel: CatalogViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel()
) {
    // Estado de catálogo
    val products by catalogViewModel.products.collectAsState()

    // Estado del carrito (persistente con Room vía StateFlow)
    val cartLines by cartViewModel.lines.collectAsState()
    val cartCount by cartViewModel.count.collectAsState(0)
    val cartTotal by cartViewModel.total.collectAsState(0.0)

    // Estados de navegación
    var showCart by remember { mutableStateOf(false) }
    var selectedProductId by remember { mutableStateOf<String?>(null) }

    // Estados globales de checkout
    var checkoutOpen by remember { mutableStateOf(false) }
    var orderPlacedId by remember { mutableStateOf<String?>(null) }

    /* ================= SUCCESS DIALOG - Global ================= */
    if (orderPlacedId != null) {
        OrderSuccessDialog(
            orderId = orderPlacedId!!,
            onDismiss = {
                orderPlacedId = null
                showCart = false
                selectedProductId = null
            }
        )
    }

    /* ================= CHECKOUT SHEET - Global ================= */
    if (checkoutOpen) {
        val globalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        CheckoutSheet(
            count = cartCount,
            total = cartTotal,
            onDismiss = { checkoutOpen = false },
            onConfirm = { paymentMethod, address ->
                if (address.isNotBlank()) {
                    val orderId = "VS-" + Random.nextInt(100_000, 999_999)
                    cartViewModel.clear()
                    checkoutOpen = false
                    orderPlacedId = orderId
                }
            },
            sheetState = globalSheetState
        )
    }

    /* ================= PANTALLA CARRITO ================= */
    if (showCart) {
        CartScreen(
            lines = cartLines,
            total = cartTotal,
            onAdd = { p, s -> cartViewModel.add(p, s) },
            onRemoveOne = { p, s -> cartViewModel.removeOne(p, s) },
            onRemoveLine = { p, s -> cartViewModel.removeLine(p, s) },
            onClearAll = { cartViewModel.clear() },
            onCheckout = { checkoutOpen = true },
            onBack = { showCart = false }
        )
        return
    }

    /* ================= PANTALLA DETALLE PRODUCTO ================= */
    selectedProductId?.let { pid ->
        val product = products.firstOrNull { it.id == pid }
        if (product != null) {
            val details = ProductDetailsRepo.get(product)

            ProductDetailScreen(
                product = product,
                details = details,
                cartCount = cartCount,
                onAddToCart = { prod, qty -> cartViewModel.add(prod, qty = qty) },
                onBuyNow = { prod, qty ->
                    cartViewModel.add(prod, qty = qty)
                    selectedProductId = null
                    checkoutOpen = true
                },
                onBack = { selectedProductId = null },
                onOpenCart = { showCart = true }
            )
            return
        } else {
            selectedProductId = null
        }
    }

    /* ================= PANTALLA HOME ================= */
    var query by remember { mutableStateOf("") }
    val minPriceRaw = remember(products) { products.minOfOrNull { it.price }?.toFloat() ?: 0f }
    val maxPriceRaw = remember(products) { products.maxOfOrNull { it.price }?.toFloat() ?: 0f }
    val hasPriceRange = maxPriceRaw > minPriceRaw
    val priceBounds = if (hasPriceRange) (minPriceRaw..maxPriceRaw) else (0f..1f)
    var selectedCategories by remember { mutableStateOf(setOf<Category>()) }
    var priceRange by remember(minPriceRaw, maxPriceRaw) { mutableStateOf(priceBounds) }
    var sortOption by remember { mutableStateOf(SortOption.NONE) }
    var filtersOpen by remember { mutableStateOf(false) }

    val filtered by remember(products, query, selectedCategories, priceRange, sortOption) {
        derivedStateOf {
            val q = query.trim()
            var seq = products.asSequence()
            if (q.isNotEmpty()) {
                seq = seq.filter { p ->
                    p.name.contains(q, ignoreCase = true) ||
                            p.keywords.any { it.contains(q, ignoreCase = true) }
                }
            }
            if (selectedCategories.isNotEmpty()) {
                seq = seq.filter { it.category in selectedCategories }
            }
            val pr = (priceRange.start..priceRange.endInclusive).coerceIn(priceBounds)
            seq = seq.filter { it.price.toFloat() in pr.start..pr.endInclusive }
            when (sortOption) {
                SortOption.NONE -> seq.toList()
                SortOption.PRICE_ASC -> seq.sortedBy { it.price }.toList()
                SortOption.PRICE_DESC -> seq.sortedByDescending { it.price }.toList()
                SortOption.NAME -> seq.sortedBy { it.name.lowercase(Locale.getDefault()) }.toList()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { GalaxyTitle() },
                actions = {
                    BadgedBox(badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } }) {
                        IconButton(onClick = { showCart = true }) {
                            Icon(Icons.Default.ShoppingCart, contentDescription = "Carrito")
                        }
                    }
                }
            )
        },
        bottomBar = {
            CartBottomBar(
                total = cartTotal,
                onCheckout = { checkoutOpen = true },
                enabled = cartCount > 0
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
                placeholder = { Text("¿Qué deseas buscar hoy?") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar")
                            }
                        }
                        IconButton(onClick = { filtersOpen = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Filtros")
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
                items(filtered) { product ->
                    ProductCard(
                        p = product,
                        onAddToCart = { cartViewModel.add(product) },
                        onOpenDetails = { selectedProductId = product.id }
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

    /* ================= SHEET DE FILTROS ================= */
    if (filtersOpen) {
        val filtersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { filtersOpen = false },
            sheetState = filtersSheetState
        ) {
            FilterSheet(
                hasPriceRange = hasPriceRange,
                bounds = priceBounds,
                selectedCategories = selectedCategories,
                onToggleCategory = { cat ->
                    selectedCategories = selectedCategories.toMutableSet().apply {
                        if (contains(cat)) remove(cat) else add(cat)
                    }
                },
                priceRange = priceRange,
                onPriceRangeChange = { priceRange = it.coerceIn(priceBounds) },
                sortOption = sortOption,
                onSortChange = { sortOption = it },
                onReset = {
                    selectedCategories = emptySet()
                    priceRange = priceBounds
                    sortOption = SortOption.NONE
                },
                onApply = { filtersOpen = false }
            )
        }
    }
}

/* ================== UI COMPONENTS ================== */

@Composable
private fun GalaxyTitle() {
    Text(
        text = "VoidShop",
        style = TextStyle(
            fontFamily = FontFamily.Serif,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF6A00FF), Color(0xFFE91E63), Color(0xFF00BCD4), Color(0xFFFFEB3B)
                )
            )
        )
    )
}

@Composable
fun ProductCard(
    p: Product,
    onAddToCart: () -> Unit,
    onOpenDetails: () -> Unit
) {
    androidx.compose.material3.Card(modifier = Modifier.clickable { onOpenDetails() }) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = p.imageRes),
                contentDescription = p.name,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = p.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = formatPrice(p.price),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(onClick = onAddToCart) {
                Text("Agregar")
            }
        }
    }
}

@Composable
fun CartBottomBar(total: Double, onCheckout: () -> Unit, enabled: Boolean) {
    Surface(shadowElevation = 8.dp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Total: ${formatPrice(total)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = onCheckout,
                enabled = enabled
            ) {
                Text("Comprar")
            }
        }
    }
}

/* ================== CHECKOUT SHEET ================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutSheet(
    count: Int,
    total: Double,
    onDismiss: () -> Unit,
    onConfirm: (PaymentMethod, String) -> Unit,
    sheetState: SheetState
) {
    var method by remember { mutableStateOf(PaymentMethod.CARD) }
    var address by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Finalizar compra",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Resumen del pedido",
                        style = MaterialTheme.typography.titleMedium // typo fix below
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Artículos:")
                        Text("$count")
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Total:",
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = formatPrice(total),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Fix: correct property name
            // (Compose preview sometimes doesn't catch this; ensure it's MaterialTheme.typography)
            // Keeping a small no-op read to avoid unused warning after fix
        }
    }
}

// Re-declare Checkout content after typographic fix to avoid confusion
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutSheetContent(
    count: Int,
    total: Double,
    method: PaymentMethod,
    onMethodChange: (PaymentMethod) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    showError: Boolean,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Finalizar compra",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Resumen del pedido",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Artículos:")
                    Text("$count")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total:",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formatPrice(total),
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Text(
            text = "Método de pago",
            style = MaterialTheme.typography.titleMedium
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = method == PaymentMethod.CARD,
                onClick = { onMethodChange(PaymentMethod.CARD) },
                label = { Text("💳 Tarjeta") }
            )
            FilterChip(
                selected = method == PaymentMethod.COD,
                onClick = { onMethodChange(PaymentMethod.COD) },
                label = { Text("💵 Efectivo") }
            )
        }

        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Dirección de entrega") },
            placeholder = { Text("Ingresa tu dirección completa") },
            isError = showError,
            supportingText = if (showError) {
                {
                    Text(
                        text = "La dirección es obligatoria",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else null
        )

        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = count > 0
        ) {
            Text("Confirmar compra - ${formatPrice(total)}")
        }

        Spacer(Modifier.height(8.dp))
    }
}

/* ================== FILTER SHEET ================== */

@Composable
private fun FilterSheet(
    hasPriceRange: Boolean,
    bounds: ClosedFloatingPointRange<Float>,
    selectedCategories: Set<Category>,
    onToggleCategory: (Category) -> Unit,
    priceRange: ClosedFloatingPointRange<Float>,
    onPriceRangeChange: (ClosedFloatingPointRange<Float>) -> Unit,
    sortOption: SortOption,
    onSortChange: (SortOption) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Filtros",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Categorías",
            style = MaterialTheme.typography.titleMedium
        )
        WrapFlow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 8.dp,
            verticalSpacing = 8.dp
        ) {
            Category.values().forEach { cat ->
                FilterChip(
                    selected = selectedCategories.contains(cat),
                    onClick = { onToggleCategory(cat) },
                    label = {
                        Text(cat.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                )
            }
        }

        Text(
            text = "Precio",
            style = MaterialTheme.typography.titleMedium
        )
        if (hasPriceRange) {
            Text(
                "Rango: ${formatPrice(priceRange.start.toDouble())} - " +
                        "${formatPrice(priceRange.endInclusive.toDouble())}"
            )
            RangeSlider(
                value = priceRange,
                onValueChange = { onPriceRangeChange(it.coerceIn(bounds)) },
                valueRange = bounds,
                steps = 0
            )
        } else {
            Text("No hay rango de precios disponible para filtrar.")
        }

        Text(
            text = "Ordenar por",
            style = MaterialTheme.typography.titleMedium
        )
        WrapFlow(
            modifier = Modifier.fillMaxWidth(),
            horizontalSpacing = 8.dp,
            verticalSpacing = 8.dp
        ) {
            SortOption.values().forEach { opt ->
                FilterChip(
                    selected = sortOption == opt,
                    onClick = { onSortChange(opt) },
                    label = { Text(sortLabel(opt)) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onReset,
                modifier = Modifier.weight(1f)
            ) {
                Text("Restablecer")
            }
            Button(
                onClick = onApply,
                modifier = Modifier.weight(1f)
            ) {
                Text("Aplicar")
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/* ================== SUCCESS DIALOG ================== */

@Composable
private fun OrderSuccessDialog(orderId: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Continuar comprando")
            }
        },
        title = { Text("¡Pedido realizado con éxito! 🎉") },
        text = {
            Column {
                Text("Número de pedido: $orderId")
                Spacer(Modifier.height(8.dp))
                Text("Recibirás un email de confirmación pronto.")
                Text("¡Gracias por tu compra!")
            }
        }
    )
}

/* ================== LAYOUT HELPERS ================== */

@Composable
private fun WrapFlow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 0.dp,
    verticalSpacing: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    Layout(modifier = modifier, content = content) { measurables, constraints ->
        val hSpace = horizontalSpacing.roundToPx()
        val vSpace = verticalSpacing.roundToPx()
        val rows = mutableListOf<List<Placeable>>()
        val rowHeights = mutableListOf<Int>()
        var currentRow = mutableListOf<Placeable>()
        var currentWidth = 0
        var currentHeight = 0
        var maxWidthUsed = 0
        var totalHeight = 0

        fun commitRow() {
            if (currentRow.isNotEmpty()) {
                rows.add(currentRow.toList())
                rowHeights.add(currentHeight)
                maxWidthUsed = max(maxWidthUsed, currentWidth)
                totalHeight += if (rows.size == 1) currentHeight else vSpace + currentHeight
                currentRow = mutableListOf(); currentWidth = 0; currentHeight = 0
            }
        }

        measurables.forEach { measurable ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0))
            val itemWidth = placeable.width
            val tentativeWidth = if (currentRow.isEmpty()) itemWidth else currentWidth + hSpace + itemWidth
            if (tentativeWidth <= constraints.maxWidth || currentRow.isEmpty()) {
                currentRow.add(placeable)
                currentWidth = tentativeWidth
                currentHeight = max(currentHeight, placeable.height)
            } else {
                commitRow()
                currentRow.add(placeable)
                currentWidth = itemWidth
                currentHeight = placeable.height
            }
        }
        commitRow()

        val layoutWidth = maxWidthUsed.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeight = totalHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

        layout(layoutWidth, layoutHeight) {
            var y = 0
            rows.forEachIndexed { rowIndex, row ->
                var x = 0
                row.forEachIndexed { index, placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + if (index < row.lastIndex) hSpace else 0
                }
                y += rowHeights[rowIndex] + if (rowIndex < rows.lastIndex) vSpace else 0
            }
        }
    }
}

/* ================== UTILITY FUNCTIONS ================== */

private fun sortLabel(opt: SortOption) = when (opt) {
    SortOption.NONE -> "Relevancia"
    SortOption.PRICE_ASC -> "Precio ↑"
    SortOption.PRICE_DESC -> "Precio ↓"
    SortOption.NAME -> "Nombre"
}

fun formatPrice(value: Double) =
    "$" + String.format(Locale.US, "%,.2f", value)

private fun ClosedFloatingPointRange<Float>.coerceIn(bounds: ClosedFloatingPointRange<Float>)
        : ClosedFloatingPointRange<Float> {
    val s = start.coerceIn(bounds.start, bounds.endInclusive)
    val e = endInclusive.coerceIn(bounds.start, bounds.endInclusive)
    return if (s <= e) s..e else bounds
}
