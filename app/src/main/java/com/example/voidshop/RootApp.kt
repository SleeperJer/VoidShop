// app/src/main/java/com/example/voidshop/RootApp.kt
package com.example.voidshop

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
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
import kotlinx.coroutines.launch
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
    val products by catalogViewModel.products.collectAsState()

    // Carrito persistente
    val cartLines by cartViewModel.lines.collectAsState()
    val cartCount by cartViewModel.count.collectAsState(0)
    val cartTotal by cartViewModel.total.collectAsState(0.0)

    var showCart by remember { mutableStateOf(false) }
    var selectedProductId by remember { mutableStateOf<String?>(null) }

    var checkoutOpen by remember { mutableStateOf(false) }
    var orderPlacedId by remember { mutableStateOf<String?>(null) }

    var addOpen by remember { mutableStateOf(false) }

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

    // Checkout sheet (forzar expandido)
    if (checkoutOpen) {
        val globalSheetState = rememberModalBottomSheetState(
            skipPartiallyExpanded = true,
            confirmValueChange = { it != SheetValue.PartiallyExpanded }
        )
        val scope = rememberCoroutineScope()
        LaunchedEffect(checkoutOpen) {
            if (checkoutOpen) scope.launch { globalSheetState.show() }
        }
        CheckoutSheet(
            count = cartCount,
            total = cartTotal,
            onDismiss = { checkoutOpen = false },
            onConfirm = { _, _ ->
                val orderId = "VS-" + Random.nextInt(100_000, 999_999)
                cartViewModel.clear()
                checkoutOpen = false
                orderPlacedId = orderId
            },
            sheetState = globalSheetState
        )
    }

    // Carrito
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

    // Detalle
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
        } else selectedProductId = null
    }

    // Home
    var query by remember { mutableStateOf("") } // solo nombre
    val minPriceRaw = remember(products) { products.minOfOrNull { it.price }?.toFloat() ?: 0f }
    val maxPriceRaw = remember(products) { products.maxOfOrNull { it.price }?.toFloat() ?: 0f }
    val hasPriceRange = maxPriceRaw > minPriceRaw
    val priceBounds = if (hasPriceRange) (minPriceRaw..maxPriceRaw) else (0f..1f)
    var selectedCategories by remember { mutableStateOf(setOf<Category>()) }
    var priceRange by remember(minPriceRaw, maxPriceRaw) { mutableStateOf(priceBounds) }
    var sortOption by remember { mutableStateOf(SortOption.NONE) }
    var filtersOpen by remember { mutableStateOf(false) }
    var keywordsFilter by remember { mutableStateOf("") } // filtro por keywords en sheet

    val filtered by remember(products, query, selectedCategories, priceRange, sortOption, keywordsFilter) {
        derivedStateOf {
            val q = query.trim()
            var seq = products.asSequence()
            if (q.isNotEmpty()) seq = seq.filter { it.name.contains(q, ignoreCase = true) }
            if (selectedCategories.isNotEmpty()) seq = seq.filter { it.category in selectedCategories }
            val pr = (priceRange.start..priceRange.endInclusive).coerceIn(priceBounds)
            seq = seq.filter { it.price.toFloat() in pr.start..pr.endInclusive }
            val tokens = keywordsFilter.split(',', ' ', ';').map { it.trim() }.filter { it.isNotEmpty() }
            if (tokens.isNotEmpty()) {
                seq = seq.filter { p -> tokens.any { t -> p.keywords.any { it.contains(t, ignoreCase = true) } } }
            }
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
            // Buscador (solo nombre)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                placeholder = { Text("Buscar por nombre…") },
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

            // Botón "+" debajo del buscador, derecha
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(onClick = { addOpen = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar producto")
                }
            }

            Spacer(Modifier.height(4.dp))

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
                            "Sin resultados.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }
    }

    // Sheet filtros (incluye keywords)
    if (filtersOpen) {
        val filtersSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(onDismissRequest = { filtersOpen = false }, sheetState = filtersSheetState) {
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
                keywords = keywordsFilter,
                onKeywordsChange = { keywordsFilter = it },
                onReset = {
                    selectedCategories = emptySet()
                    priceRange = priceBounds
                    sortOption = SortOption.NONE
                    keywordsFilter = ""
                },
                onApply = { filtersOpen = false }
            )
        }
    }

    // Sheet agregar producto
    if (addOpen) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        AddProductSheet(
            onDismiss = { addOpen = false },
            onSubmit = { name, price, category, keywords ->
                catalogViewModel.addUserProduct(name, price, category, keywords)
                addOpen = false
            },
            sheetState = sheet
        )
    }
}

/* ================== Add Product Sheet ================== */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddProductSheet(
    onDismiss: () -> Unit,
    onSubmit: (String, Double, Category, List<String>) -> Unit,
    sheetState: SheetState
) {
    var name by remember { mutableStateOf("") }
    var priceText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(Category.GAMES) }
    var keywordsText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Agregar producto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it; error = null },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = priceText,
                onValueChange = { priceText = it; error = null },
                label = { Text("Precio") },
                placeholder = { Text("Ej: 12.99") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Text("Categoría", style = MaterialTheme.typography.titleSmall)
            WrapFlow(horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
                Category.values().forEach { cat ->
                    FilterChip(
                        selected = category == cat,
                        onClick = { category = cat },
                        label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            OutlinedTextField(
                value = keywordsText,
                onValueChange = { keywordsText = it },
                label = { Text("Keywords (separadas por coma)") },
                placeholder = { Text("gaming, azul, algodón") },
                modifier = Modifier.fillMaxWidth()
            )

            if (error != null) {
                Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Button(
                onClick = {
                    val p = priceText.replace(',', '.').toDoubleOrNull()
                    val kws = keywordsText.split(',', ';')
                        .map { it.trim() }.filter { it.isNotEmpty() }

                    when {
                        name.isBlank() -> error = "Nombre requerido"
                        p == null -> error = "Precio inválido"
                        else -> onSubmit(name.trim(), p, category, kws)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar") }

            Spacer(Modifier.height(8.dp))
        }
    }
}

/* ================== Filter Sheet ================== */

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
    keywords: String,
    onKeywordsChange: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Filtros", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Text("Categorías", style = MaterialTheme.typography.titleMedium)
        WrapFlow(modifier = Modifier.fillMaxWidth(), horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
            Category.values().forEach { cat ->
                FilterChip(
                    selected = selectedCategories.contains(cat),
                    onClick = { onToggleCategory(cat) },
                    label = { Text(cat.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Text("Precio", style = MaterialTheme.typography.titleMedium)
        if (hasPriceRange) {
            Text(
                "Rango: ${formatPrice(priceRange.start.toDouble())} - " +
                        "${formatPrice(priceRange.endInclusive.toDouble())}"
            )
            RangeSlider(
                value = priceRange,
                onValueChange = { onValueChange -> onPriceRangeChange(onValueChange.coerceIn(bounds)) },
                valueRange = bounds,
                steps = 0
            )
        } else {
            Text("No hay rango de precios disponible para filtrar.")
        }

        Text("Ordenar por", style = MaterialTheme.typography.titleMedium)
        WrapFlow(modifier = Modifier.fillMaxWidth(), horizontalSpacing = 8.dp, verticalSpacing = 8.dp) {
            SortOption.values().forEach { opt ->
                FilterChip(
                    selected = sortOption == opt,
                    onClick = { onSortChange(opt) },
                    label = { Text(sortLabel(opt)) }
                )
            }
        }

        OutlinedTextField(
            value = keywords,
            onValueChange = onKeywordsChange,
            label = { Text("Keywords (coma o espacio)") },
            placeholder = { Text("gaming, algodón") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Text("Restablecer") }
            Button(onClick = onApply, modifier = Modifier.weight(1f)) { Text("Aplicar") }
        }
        Spacer(Modifier.height(12.dp))
    }
}

/* ================== UI Aux ================== */

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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenDetails() }
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {

            // ✅ Fallback seguro para imageRes=0 (evita crash)
            val safeRes = p.imageRes.takeIf { it != 0 } ?: R.drawable.ic_launcher_foreground

            Image(
                painter = painterResource(id = safeRes),
                contentDescription = p.name,
                modifier = Modifier.size(64.dp),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(p.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(formatPrice(p.price), style = MaterialTheme.typography.bodyMedium)
            }
            Button(onClick = onAddToCart) { Text("Agregar") }
        }
    }
}

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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.95f)
                .navigationBarsPadding()
                .imePadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Finalizar compra", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Resumen del pedido", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Artículos:"); Text("$count")
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total:", fontWeight = FontWeight.SemiBold)
                            Text(formatPrice(total), fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Text("Método de pago", style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = method == PaymentMethod.CARD, onClick = { method = PaymentMethod.CARD }, label = { Text("💳 Tarjeta") })
                    FilterChip(selected = method == PaymentMethod.COD,  onClick = { method = PaymentMethod.COD  }, label = { Text("💵 Efectivo") })
                }

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it; showError = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Dirección de entrega") },
                    placeholder = { Text("Ingresa tu dirección completa") },
                    isError = showError,
                    supportingText = if (showError) { { Text("La dirección es obligatoria", color = MaterialTheme.colorScheme.error) } } else null
                )

                Button(
                    onClick = {
                        if (address.isBlank()) showError = true else onConfirm(method, address)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = count > 0
                ) { Text("Confirmar compra - ${formatPrice(total)}") }

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun OrderSuccessDialog(orderId: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("Continuar comprando") } },
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

/* ================== Layout Helper ================== */

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

/* ================== Utils ================== */

private fun sortLabel(opt: SortOption) = when (opt) {
    SortOption.NONE -> "Relevancia"
    SortOption.PRICE_ASC -> "Precio ↑"
    SortOption.PRICE_DESC -> "Precio ↓"
    SortOption.NAME -> "Nombre"
}

fun formatPrice(value: Double) = "$" + String.format(Locale.US, "%,.2f", value)

private fun ClosedFloatingPointRange<Float>.coerceIn(bounds: ClosedFloatingPointRange<Float>): ClosedFloatingPointRange<Float> {
    val s = start.coerceIn(bounds.start, bounds.endInclusive)
    val e = endInclusive.coerceIn(bounds.start, bounds.endInclusive)
    return if (s <= e) s..e else bounds
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
            Button(onClick = onCheckout, enabled = enabled) { Text("Comprar") }
        }
    }
}
