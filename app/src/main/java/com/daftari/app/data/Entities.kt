package com.daftari.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sku: String = "",
    val category: String = "",
    val purchaseCost: Double,
    val sellingPrice: Double,
    val stockQty: Int,
    val createdAt: Long = System.currentTimeMillis(),
    val active: Boolean = true
)

@Entity(tableName = "purchases")
data class Purchase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val totalCost: Double,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Double,
    val unitCost: Double,
    val totalRevenue: Double,
    val totalCost: Double,
    val customerName: String = "",
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val description: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "waste")
data class Waste(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val estimatedCost: Double,
    val reason: String,
    val date: Long = System.currentTimeMillis()
)

@Entity(tableName = "inventory_layers")
data class InventoryLayer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val sourcePurchaseId: Long = 0,
    val unitCost: Double,
    val originalQuantity: Int,
    val remainingQuantity: Int,
    val createdAt: Long
)
