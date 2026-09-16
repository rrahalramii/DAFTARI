package com.daftari.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Product::class, Purchase::class, Sale::class, Expense::class, Customer::class, Waste::class, InventoryLayer::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun purchaseDao(): PurchaseDao
    abstract fun saleDao(): SaleDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun customerDao(): CustomerDao
    abstract fun wasteDao(): WasteDao
    abstract fun inventoryLayerDao(): InventoryLayerDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS inventory_layers (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        sourcePurchaseId INTEGER NOT NULL DEFAULT 0,
                        unitCost REAL NOT NULL,
                        originalQuantity INTEGER NOT NULL,
                        remainingQuantity INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // v2 stored the product stock as opening stock while purchases/sales/waste
                // were replayed only in the FIFO layer. v3 makes stockQty the real current
                // stock, so bring existing users forward without losing their data.
                db.execSQL("""
                    UPDATE products SET stockQty = stockQty
                        + COALESCE((SELECT SUM(quantity) FROM purchases WHERE purchases.productId = products.id), 0)
                        - COALESCE((SELECT SUM(quantity) FROM sales WHERE sales.productId = products.id), 0)
                        - COALESCE((SELECT SUM(quantity) FROM waste WHERE waste.productId = products.id), 0)
                """.trimIndent())
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "daftari.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { INSTANCE = it }
        }
    }
}
