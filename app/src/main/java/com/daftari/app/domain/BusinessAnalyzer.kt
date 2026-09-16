package com.daftari.app.domain

import com.daftari.app.data.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

class BusinessAnalyzer {
    data class Snapshot(
        val start: Long,
        val end: Long,
        val products: List<Product>,
        val purchases: List<Purchase>,
        val sales: List<Sale>,
        val expenses: List<Expense>,
        val waste: List<Waste>
    )

    fun buildSnapshot(start: Long, end: Long, products: List<Product>, purchases: List<Purchase>, sales: List<Sale>, expenses: List<Expense>, waste: List<Waste>): Snapshot =
        Snapshot(start, end, products, purchases, sales, expenses, waste)

    fun toAiContext(s: Snapshot): String {
        val revenue = s.sales.sumOf { it.totalRevenue }
        val cogs = s.sales.sumOf { it.totalCost }
        val expenses = s.expenses.sumOf { it.amount }
        val waste = s.waste.sumOf { it.estimatedCost }
        val gross = revenue - cogs
        val net = gross - expenses - waste
        val qtySold = s.sales.sumOf { it.quantity }
        val purchaseSpend = s.purchases.sumOf { it.totalCost }
        val lowStock = s.products.filter { it.stockQty in 1..5 }
        val stockValue = s.products.sumOf { it.stockQty * it.purchaseCost }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val productLines = s.products.joinToString("\n") {
            "- ${it.name} | category=${it.category} | stock=${it.stockQty} | buy=${money(it.purchaseCost)} | sell=${money(it.sellingPrice)}"
        }
        val saleLines = s.sales.joinToString("\n") {
            "- ${fmt.format(Date(it.date))} | ${it.productName} | qty=${it.quantity} | revenue=${money(it.totalRevenue)} | cost=${money(it.totalCost)} | customer=${it.customerName.ifBlank { "N/A" }}"
        }
        val purchaseLines = s.purchases.joinToString("\n") {
            "- ${fmt.format(Date(it.date))} | ${it.productName} | qty=${it.quantity} | total=${money(it.totalCost)}"
        }
        val expenseLines = s.expenses.joinToString("\n") {
            "- ${fmt.format(Date(it.date))} | ${it.category} | ${it.description} | ${money(it.amount)}"
        }
        val wasteLines = s.waste.joinToString("\n") {
            "- ${fmt.format(Date(it.date))} | ${it.productName} | qty=${it.quantity} | cost=${money(it.estimatedCost)} | reason=${it.reason}"
        }

        return """
            DAFTARI BUSINESS ANALYSIS DATA
            PERIOD: ${fmt.format(Date(s.start))} through ${fmt.format(Date(s.end))}

            CALCULATED METRICS
            Revenue: ${money(revenue)}
            COGS: ${money(cogs)}
            Gross profit: ${money(gross)}
            Operating expenses: ${money(expenses)}
            Waste cost: ${money(waste)}
            Estimated net profit after listed expenses/waste: ${money(net)}
            Units sold: $qtySold
            Purchase spend: ${money(purchaseSpend)}
            Current inventory value at purchase cost: ${money(stockValue)}
            Low-stock products (1-5 units): ${lowStock.size}

            PRODUCTS / CURRENT INVENTORY
            ${productLines.ifBlank { "No products recorded." }}

            PURCHASES IN PERIOD
            ${purchaseLines.ifBlank { "No purchases recorded." }}

            SALES IN PERIOD
            ${saleLines.ifBlank { "No sales recorded." }}

            EXPENSES IN PERIOD
            ${expenseLines.ifBlank { "No expenses recorded." }}

            WASTE / DAMAGE IN PERIOD
            ${wasteLines.ifBlank { "No waste recorded." }}
        """.trimIndent()
    }

    private fun money(v: Double) = String.format(Locale.US, "$%.2f", v)
}
