package com.daftari.app.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class DaftariRepository(private val db: AppDatabase) {
    val products: Flow<List<Product>> = db.productDao().observeAll()
    val customers: Flow<List<Customer>> = db.customerDao().observeAll()
    val recentSales: Flow<List<Sale>> = db.saleDao().observeAll()
    val recentExpenses: Flow<List<Expense>> = db.expenseDao().observeAll()
    val recentPurchases: Flow<List<Purchase>> = db.purchaseDao().observeAll()
    val waste: Flow<List<Waste>> = db.wasteDao().observeAll()
    val inventoryValuations: Flow<List<InventoryValuation>> = db.inventoryLayerDao().observeValuations()

    suspend fun addProduct(name: String, sku: String, category: String, buy: Double, sell: Double, stock: Int) = db.withTransaction {
        val id = db.productDao().insert(Product(name = name, sku = sku, category = category, purchaseCost = buy, sellingPrice = sell, stockQty = stock))
        if (stock > 0) db.inventoryLayerDao().insert(InventoryLayer(productId = id, unitCost = buy, originalQuantity = stock, remainingQuantity = stock, createdAt = System.currentTimeMillis()))
    }

    suspend fun addSale(product: Product, quantity: Int, unitPrice: Double, customer: String, date: Long) = db.withTransaction {
        val fresh = db.productDao().get(product.id) ?: return@withTransaction
        if (quantity <= 0 || quantity > fresh.stockQty) return@withTransaction
        db.saleDao().insert(Sale(productId = product.id, productName = fresh.name, quantity = quantity, unitPrice = unitPrice, unitCost = 0.0, totalRevenue = quantity * unitPrice, totalCost = 0.0, customerName = customer, date = date))
        db.productDao().changeStock(product.id, -quantity)
        rebuildFifo()
    }

    suspend fun addPurchase(product: Product, quantity: Int, unitCost: Double, date: Long) = db.withTransaction {
        if (quantity <= 0) return@withTransaction
        db.purchaseDao().insert(Purchase(productId = product.id, productName = product.name, quantity = quantity, unitCost = unitCost, totalCost = quantity * unitCost, date = date))
        db.productDao().changeStock(product.id, quantity)
        rebuildFifo()
    }

    suspend fun addExpense(category: String, description: String, amount: Double, date: Long) = db.expenseDao().insert(Expense(category = category, description = description, amount = amount, date = date))

    suspend fun addWaste(product: Product, quantity: Int, reason: String, date: Long) = db.withTransaction {
        val fresh = db.productDao().get(product.id) ?: return@withTransaction
        if (quantity <= 0 || quantity > fresh.stockQty) return@withTransaction
        db.wasteDao().insert(Waste(productId = fresh.id, productName = fresh.name, quantity = quantity, estimatedCost = 0.0, reason = reason, date = date))
        db.productDao().changeStock(product.id, -quantity)
        rebuildFifo()
    }

    suspend fun updateSale(old: Sale, product: Product, quantity: Int, unitPrice: Double, customer: String, date: Long) = db.withTransaction {
        if (quantity <= 0) return@withTransaction
        if (old.productId == product.id) {
            val fresh = db.productDao().get(product.id) ?: return@withTransaction
            val availableAfterUndo = fresh.stockQty + old.quantity
            if (quantity > availableAfterUndo) return@withTransaction
            db.productDao().changeStock(product.id, old.quantity - quantity)
        } else {
            val newProduct = db.productDao().get(product.id) ?: return@withTransaction
            if (quantity > newProduct.stockQty) return@withTransaction
            db.productDao().changeStock(old.productId, old.quantity)
            db.productDao().changeStock(product.id, -quantity)
        }
        db.saleDao().updateFields(old.id, product.id, product.name, quantity, unitPrice, 0.0, quantity * unitPrice, 0.0, customer, date)
        rebuildFifo()
    }

    suspend fun deleteSale(sale: Sale) = db.withTransaction {
        db.saleDao().deleteById(sale.id)
        db.productDao().changeStock(sale.productId, sale.quantity)
        rebuildFifo()
    }

    suspend fun updatePurchase(old: Purchase, product: Product, quantity: Int, unitCost: Double, date: Long) = db.withTransaction {
        if (quantity <= 0) return@withTransaction
        db.productDao().changeStock(old.productId, -old.quantity)
        db.productDao().changeStock(product.id, quantity)
        db.purchaseDao().updateFields(old.id, product.id, product.name, quantity, unitCost, quantity * unitCost, date)
        rebuildFifo()
    }

    suspend fun deletePurchase(purchase: Purchase) = db.withTransaction {
        db.purchaseDao().deleteById(purchase.id)
        db.productDao().changeStock(purchase.productId, -purchase.quantity)
        rebuildFifo()
    }

    suspend fun updateExpense(expense: Expense, category: String, description: String, amount: Double, date: Long) = db.expenseDao().updateFields(expense.id, category, description, amount, date)
    suspend fun deleteExpense(expense: Expense) = db.expenseDao().deleteById(expense.id)

    suspend fun updateWaste(old: Waste, product: Product, quantity: Int, reason: String, date: Long) = db.withTransaction {
        if (quantity <= 0) return@withTransaction
        db.productDao().changeStock(old.productId, old.quantity)
        val fresh = db.productDao().get(product.id) ?: return@withTransaction
        if (quantity > fresh.stockQty) {
            db.productDao().changeStock(old.productId, -old.quantity)
            return@withTransaction
        }
        db.productDao().changeStock(product.id, -quantity)
        db.wasteDao().updateFields(old.id, product.id, product.name, quantity, 0.0, reason, date)
        rebuildFifo()
    }

    suspend fun deleteWaste(waste: Waste) = db.withTransaction {
        db.wasteDao().deleteById(waste.id)
        db.productDao().changeStock(waste.productId, waste.quantity)
        rebuildFifo()
    }

    suspend fun addCustomer(name: String, phone: String, notes: String) = db.customerDao().insert(Customer(name = name, phone = phone, notes = notes))

    suspend fun snapshot(start: Long, end: Long): BusinessData = BusinessData(
        products = db.productDao().observeAllOnce(),
        purchases = db.purchaseDao().between(start, end),
        sales = db.saleDao().between(start, end),
        expenses = db.expenseDao().between(start, end),
        waste = db.wasteDao().between(start, end)
    )

    suspend fun resetAllData() = db.withTransaction {
        db.inventoryLayerDao().clearAll()
        db.saleDao().clearAll()
        db.purchaseDao().clearAll()
        db.expenseDao().clearAll()
        db.wasteDao().clearAll()
        db.customerDao().clearAll()
        db.productDao().clearAll()
    }

    suspend fun rebuildInventoryFromHistory() = db.withTransaction { rebuildFifo() }

    private suspend fun rebuildFifo() {
        val products = db.productDao().observeAllOnce()
        val purchases = db.purchaseDao().allChronological()
        val sales = db.saleDao().allChronological()
        val waste = db.wasteDao().allChronological()
        val layerDao = db.inventoryLayerDao()
        layerDao.clearAll()

        for (product in products) {
            val pPurchases = purchases.filter { it.productId == product.id }
            val pSales = sales.filter { it.productId == product.id }
            val pWaste = waste.filter { it.productId == product.id }
            // Current stock is maintained by every inventory transaction. Reconstruct the
            // original opening stock before replaying purchase/sale/waste history.
            val openingQty = (product.stockQty + pSales.sumOf { it.quantity } + pWaste.sumOf { it.quantity } - pPurchases.sumOf { it.quantity }).coerceAtLeast(0)
            val firstDate = listOfNotNull(pPurchases.minOfOrNull { it.date }, pSales.minOfOrNull { it.date }, pWaste.minOfOrNull { it.date }).minOrNull() ?: product.createdAt
            val layers = mutableListOf<MutableLayer>()
            if (openingQty > 0) layers += MutableLayer(0, product.id, product.purchaseCost, openingQty, openingQty, firstDate - 1)
            for (purchase in pPurchases.sortedWith(compareBy<Purchase> { it.date }.thenBy { it.id })) {
                layers += MutableLayer(purchase.id, product.id, purchase.unitCost, purchase.quantity, purchase.quantity, purchase.date)
            }
            val events = buildList {
                pSales.forEach { add(InventoryEvent(it.date, 1, it.id, it.quantity, false, it.id)) }
                pWaste.forEach { add(InventoryEvent(it.date, 2, it.id, it.quantity, true, it.id)) }
            }.sortedWith(compareBy<InventoryEvent> { it.date }.thenBy { it.priority }.thenBy { it.id })

            for (event in events) {
                var remaining = event.quantity
                var cost = 0.0
                for (layer in layers) {
                    if (remaining <= 0) break
                    val take = minOf(remaining, layer.remaining)
                    if (take > 0) {
                        layer.remaining -= take
                        remaining -= take
                        cost += take * layer.unitCost
                    }
                }
                if (event.isWaste) {
                    db.wasteDao().updateFields(event.id, product.id, product.name, event.quantity, cost, pWaste.first { it.id == event.id }.reason, pWaste.first { it.id == event.id }.date)
                } else {
                    val sale = pSales.first { it.id == event.id }
                    val avg = if (event.quantity > 0) cost / event.quantity else 0.0
                    db.saleDao().updateFields(sale.id, product.id, product.name, sale.quantity, sale.unitPrice, avg, sale.totalRevenue, cost, sale.customerName, sale.date)
                }
            }
            layerDao.clearForProduct(product.id)
            for (layer in layers.filter { it.remaining > 0 }) {
                layerDao.insert(InventoryLayer(productId = layer.productId, sourcePurchaseId = layer.sourcePurchaseId, unitCost = layer.unitCost, originalQuantity = layer.original, remainingQuantity = layer.remaining, createdAt = layer.createdAt))
            }
            // Stock quantity is maintained by add/update/delete transaction methods. Do not overwrite it here.
        }
    }

    suspend fun seedDemoData() {
        if (db.productDao().observeAllOnce().isNotEmpty()) return
        val headlights = db.productDao().insert(Product(name = "LED Headlight H7", sku = "HL-H7", category = "Lighting", purchaseCost = 18.0, sellingPrice = 32.0, stockQty = 32))
        val tint = db.productDao().insert(Product(name = "Window Tint Service", sku = "TINT", category = "Service", purchaseCost = 4.0, sellingPrice = 25.0, stockQty = 999))
        val chargers = db.productDao().insert(Product(name = "USB-C Car Charger", sku = "CHG-C", category = "Accessories", purchaseCost = 5.0, sellingPrice = 12.0, stockQty = 7))
        val now = System.currentTimeMillis(); val day = 86_400_000L
        db.purchaseDao().insert(Purchase(productId = headlights, productName = "LED Headlight H7", quantity = 30, unitCost = 18.0, totalCost = 540.0, date = now - day * 45))
        db.purchaseDao().insert(Purchase(productId = headlights, productName = "LED Headlight H7", quantity = 10, unitCost = 18.0, totalCost = 180.0, date = now - day * 15))
        db.purchaseDao().insert(Purchase(productId = chargers, productName = "USB-C Car Charger", quantity = 20, unitCost = 5.0, totalCost = 100.0, date = now - day * 25))
        repeat(8) { i -> db.saleDao().insert(Sale(productId = headlights, productName = "LED Headlight H7", quantity = 1, unitPrice = 32.0, unitCost = 18.0, totalRevenue = 32.0, totalCost = 18.0, customerName = "Customer ${i + 1}", date = now - day * (40 - i * 4))) }
        repeat(18) { i -> db.saleDao().insert(Sale(productId = tint, productName = "Window Tint Service", quantity = 1, unitPrice = 25.0, unitCost = 4.0, totalRevenue = 25.0, totalCost = 4.0, customerName = "Tint Job ${i + 1}", date = now - day * (35 - i))) }
        db.expenseDao().insert(Expense(category = "Electricity", description = "Shop electricity", amount = 30.0, date = now - day * 40))
        db.expenseDao().insert(Expense(category = "Electricity", description = "Shop electricity", amount = 35.0, date = now - day * 10))
        db.expenseDao().insert(Expense(category = "Rent", description = "Monthly rent", amount = 250.0, date = now - day * 20))
        db.wasteDao().insert(Waste(productId = chargers, productName = "USB-C Car Charger", quantity = 2, estimatedCost = 10.0, reason = "Damaged", date = now - day * 12))
    }

    private data class MutableLayer(val sourcePurchaseId: Long, val productId: Long, val unitCost: Double, val original: Int, var remaining: Int, val createdAt: Long)
    private data class InventoryEvent(val date: Long, val priority: Int, val id: Long, val quantity: Int, val isWaste: Boolean, val sourceId: Long)
}

data class InventoryValuation(val productId: Long, val units: Int, val value: Double) {
    val averageCost: Double get() = if (units > 0) value / units else 0.0
}

data class BusinessData(val products: List<Product>, val purchases: List<Purchase>, val sales: List<Sale>, val expenses: List<Expense>, val waste: List<Waste>)
private suspend fun ProductDao.observeAllOnce(): List<Product> = observeAll().first()



