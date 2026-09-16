package com.daftari.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Query("SELECT * FROM products ORDER BY name") fun observeAll(): Flow<List<Product>>
    @Query("SELECT * FROM products WHERE id = :id LIMIT 1") suspend fun get(id: Long): Product?
    @Insert suspend fun insert(product: Product): Long
    @Update suspend fun update(product: Product)
    @Delete suspend fun delete(product: Product)
    @Query("UPDATE products SET stockQty = stockQty + :delta WHERE id = :id") suspend fun changeStock(id: Long, delta: Int)
    @Query("UPDATE products SET stockQty = :stock WHERE id = :id") suspend fun setStock(id: Long, stock: Int)
    @Query("DELETE FROM products") suspend fun clearAll()
}

@Dao
interface PurchaseDao {
    @Query("SELECT * FROM purchases WHERE date BETWEEN :start AND :end ORDER BY date DESC") suspend fun between(start: Long, end: Long): List<Purchase>
    @Query("SELECT * FROM purchases ORDER BY date DESC LIMIT :limit") fun recent(limit: Int): Flow<List<Purchase>>
    @Query("SELECT * FROM purchases ORDER BY date DESC") fun observeAll(): Flow<List<Purchase>>
    @Query("SELECT * FROM purchases ORDER BY date ASC, id ASC") suspend fun allChronological(): List<Purchase>
    @Insert suspend fun insert(purchase: Purchase): Long
    @Query("UPDATE purchases SET productId = :productId, productName = :productName, quantity = :quantity, unitCost = :unitCost, totalCost = :totalCost, date = :date WHERE id = :id")
    suspend fun updateFields(id: Long, productId: Long, productName: String, quantity: Int, unitCost: Double, totalCost: Double, date: Long)
    @Query("DELETE FROM purchases WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM purchases") suspend fun clearAll()
}

@Dao
interface SaleDao {
    @Query("SELECT * FROM sales WHERE date BETWEEN :start AND :end ORDER BY date DESC") suspend fun between(start: Long, end: Long): List<Sale>
    @Query("SELECT * FROM sales ORDER BY date DESC LIMIT :limit") fun recent(limit: Int): Flow<List<Sale>>
    @Query("SELECT * FROM sales ORDER BY date DESC") fun observeAll(): Flow<List<Sale>>
    @Query("SELECT * FROM sales ORDER BY date ASC, id ASC") suspend fun allChronological(): List<Sale>
    @Insert suspend fun insert(sale: Sale): Long
    @Query("UPDATE sales SET productId = :productId, productName = :productName, quantity = :quantity, unitPrice = :unitPrice, unitCost = :unitCost, totalRevenue = :totalRevenue, totalCost = :totalCost, customerName = :customerName, date = :date WHERE id = :id")
    suspend fun updateFields(id: Long, productId: Long, productName: String, quantity: Int, unitPrice: Double, unitCost: Double, totalRevenue: Double, totalCost: Double, customerName: String, date: Long)
    @Query("DELETE FROM sales WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM sales") suspend fun clearAll()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses WHERE date BETWEEN :start AND :end ORDER BY date DESC") suspend fun between(start: Long, end: Long): List<Expense>
    @Query("SELECT * FROM expenses ORDER BY date DESC LIMIT :limit") fun recent(limit: Int): Flow<List<Expense>>
    @Query("SELECT * FROM expenses ORDER BY date DESC") fun observeAll(): Flow<List<Expense>>
    @Insert suspend fun insert(expense: Expense): Long
    @Query("UPDATE expenses SET category = :category, description = :description, amount = :amount, date = :date WHERE id = :id")
    suspend fun updateFields(id: Long, category: String, description: String, amount: Double, date: Long)
    @Query("DELETE FROM expenses WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM expenses") suspend fun clearAll()
}

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY name") fun observeAll(): Flow<List<Customer>>
    @Insert suspend fun insert(customer: Customer): Long
    @Delete suspend fun delete(customer: Customer)
    @Query("DELETE FROM customers") suspend fun clearAll()
}

@Dao
interface WasteDao {
    @Query("SELECT * FROM waste WHERE date BETWEEN :start AND :end ORDER BY date DESC") suspend fun between(start: Long, end: Long): List<Waste>
    @Query("SELECT * FROM waste ORDER BY date DESC") fun observeAll(): Flow<List<Waste>>
    @Query("SELECT * FROM waste ORDER BY date ASC, id ASC") suspend fun allChronological(): List<Waste>
    @Insert suspend fun insert(waste: Waste): Long
    @Query("UPDATE waste SET productId = :productId, productName = :productName, quantity = :quantity, estimatedCost = :estimatedCost, reason = :reason, date = :date WHERE id = :id")
    suspend fun updateFields(id: Long, productId: Long, productName: String, quantity: Int, estimatedCost: Double, reason: String, date: Long)
    @Query("DELETE FROM waste WHERE id = :id") suspend fun deleteById(id: Long)
    @Query("DELETE FROM waste") suspend fun clearAll()
}

@Dao
interface InventoryLayerDao {
    @Query("SELECT productId, COALESCE(SUM(remainingQuantity), 0) AS units, COALESCE(SUM(remainingQuantity * unitCost), 0.0) AS value FROM inventory_layers GROUP BY productId") fun observeValuations(): Flow<List<InventoryValuation>>
    @Query("SELECT * FROM inventory_layers WHERE productId = :productId ORDER BY createdAt ASC, id ASC") suspend fun forProduct(productId: Long): List<InventoryLayer>
    @Insert suspend fun insert(layer: InventoryLayer): Long
    @Query("DELETE FROM inventory_layers") suspend fun clearAll()
    @Query("DELETE FROM inventory_layers WHERE productId = :productId") suspend fun clearForProduct(productId: Long)
}
