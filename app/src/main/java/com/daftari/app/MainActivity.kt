package com.daftari.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.daftari.app.ai.GeminiClient
import com.daftari.app.data.*
import com.daftari.app.domain.BusinessAnalyzer
import com.daftari.app.ui.DaftariApp
import com.daftari.app.ui.theme.DaftariTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AppDatabase.get(this)
        val repo = DaftariRepository(db)
        val settings = SettingsStore(this)
        val vm = ViewModelProvider(this, DaftariViewModel.factory(repo, settings))[DaftariViewModel::class.java]
        setContent { DaftariTheme { DaftariApp(vm) } }
    }
}

class DaftariViewModel(
    private val repo: DaftariRepository,
    private val settings: SettingsStore
) : ViewModel() {
    private val ai = GeminiClient()
    private val analyzer = BusinessAnalyzer()

    val products = repo.products
    val customers = repo.customers
    val recentSales = repo.recentSales
    val recentExpenses = repo.recentExpenses
    val recentPurchases = repo.recentPurchases
    val waste = repo.waste
    val inventoryValuations = repo.inventoryValuations

    private val _todaySales = MutableStateFlow<List<Sale>>(emptyList())
    val todaySales: StateFlow<List<Sale>> = _todaySales.asStateFlow()
    private val _todayExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val todayExpenses: StateFlow<List<Expense>> = _todayExpenses.asStateFlow()
    private val _todayPurchases = MutableStateFlow<List<Purchase>>(emptyList())
    val todayPurchases: StateFlow<List<Purchase>> = _todayPurchases.asStateFlow()
    private val _todayWaste = MutableStateFlow<List<Waste>>(emptyList())
    val todayWaste: StateFlow<List<Waste>> = _todayWaste.asStateFlow()

    private val _dashboard = MutableStateFlow(DashboardMetrics())
    val dashboard: StateFlow<DashboardMetrics> = _dashboard.asStateFlow()

    private val _analysis = MutableStateFlow("")
    val analysis: StateFlow<String> = _analysis.asStateFlow()
    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()
    private val _aiError = MutableStateFlow("")
    val aiError: StateFlow<String> = _aiError.asStateFlow()
    private val _apiConfigured = MutableStateFlow(settings.apiKey.isNotBlank())
    val apiConfigured: StateFlow<Boolean> = _apiConfigured.asStateFlow()

    init {
        viewModelScope.launch { repo.seedDemoData(); repo.rebuildInventoryFromHistory(); refreshDashboard() }
    }

    private suspend fun refreshDashboard() {
        val start = startOfToday()
        val end = endOfToday()
        val data = repo.snapshot(start, end)
        val revenue = data.sales.sumOf { it.totalRevenue }
        val cogs = data.sales.sumOf { it.totalCost }
        val expenses = data.expenses.sumOf { it.amount }
        val waste = data.waste.sumOf { it.estimatedCost }
        _todaySales.value = data.sales
        _todayExpenses.value = data.expenses
        _todayPurchases.value = data.purchases
        _todayWaste.value = data.waste
        _dashboard.value = DashboardMetrics(
            revenue = revenue,
            profit = revenue - cogs - expenses,
            expenses = expenses,
            inventoryValue = repo.inventoryValuations.first().sumOf { it.value },
            items = data.products.sumOf { it.stockQty },
            salesCount = data.sales.sumOf { it.quantity }
        )
    }

    fun saveApiSettings(key: String, model: String) {
        settings.apiKey = key
        settings.model = model.ifBlank { "gemini-3.6-flash" }
        _apiConfigured.value = key.isNotBlank()
    }

    fun addProduct(name: String, sku: String, category: String, buy: String, sell: String, stock: String) = viewModelScope.launch {
        repo.addProduct(name, sku, category, buy.toDoubleOrNull() ?: 0.0, sell.toDoubleOrNull() ?: 0.0, stock.toIntOrNull() ?: 0); refreshDashboard()
    }

    fun addSale(product: Product, quantity: String, price: String, customer: String, date: Long) = viewModelScope.launch {
        repo.addSale(product, quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1, price.toDoubleOrNull() ?: product.sellingPrice, customer, date); refreshDashboard()
    }

    fun addPurchase(product: Product, quantity: String, cost: String, date: Long) = viewModelScope.launch {
        repo.addPurchase(product, quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1, cost.toDoubleOrNull() ?: product.purchaseCost, date); refreshDashboard()
    }

    fun addExpense(category: String, description: String, amount: String, date: Long) = viewModelScope.launch {
        repo.addExpense(category, description, amount.toDoubleOrNull() ?: 0.0, date); refreshDashboard()
    }

    fun addWaste(product: Product, quantity: String, reason: String, date: Long) = viewModelScope.launch {
        repo.addWaste(product, quantity.toIntOrNull()?.coerceAtLeast(1) ?: 1, reason, date); refreshDashboard()
    }


    fun updateSale(old: Sale, product: Product, quantity: String, price: String, customer: String, date: Long) = viewModelScope.launch {
        repo.updateSale(old, product, quantity.toIntOrNull()?.coerceAtLeast(1) ?: old.quantity, price.toDoubleOrNull() ?: old.unitPrice, customer, date); refreshDashboard()
    }
    fun deleteSale(sale: Sale) = viewModelScope.launch { repo.deleteSale(sale); refreshDashboard() }
    fun updatePurchase(old: Purchase, product: Product, quantity: String, cost: String, date: Long) = viewModelScope.launch {
        repo.updatePurchase(old, product, quantity.toIntOrNull()?.coerceAtLeast(1) ?: old.quantity, cost.toDoubleOrNull() ?: old.unitCost, date); refreshDashboard()
    }
    fun deletePurchase(purchase: Purchase) = viewModelScope.launch { repo.deletePurchase(purchase); refreshDashboard() }
    fun updateExpense(expense: Expense, category: String, description: String, amount: String, date: Long) = viewModelScope.launch {
        repo.updateExpense(expense, category, description, amount.toDoubleOrNull() ?: expense.amount, date); refreshDashboard()
    }
    fun deleteExpense(expense: Expense) = viewModelScope.launch { repo.deleteExpense(expense); refreshDashboard() }
    fun updateWaste(old: Waste, product: Product, quantity: String, reason: String, date: Long) = viewModelScope.launch {
        repo.updateWaste(old, product, quantity.toIntOrNull()?.coerceAtLeast(1) ?: old.quantity, reason, date); refreshDashboard()
    }
    fun deleteWaste(waste: Waste) = viewModelScope.launch { repo.deleteWaste(waste); refreshDashboard() }

    fun addCustomer(name: String, phone: String, notes: String) = viewModelScope.launch {
        repo.addCustomer(name, phone, notes); refreshDashboard()
    }

    fun analyze(start: Long, end: Long, question: String = "") = viewModelScope.launch {
        _aiError.value = ""
        _aiBusy.value = true
        try {
            val data = repo.snapshot(start, end)
            val snapshot = analyzer.buildSnapshot(start, end, data.products, data.purchases, data.sales, data.expenses, data.waste)
            val context = analyzer.toAiContext(snapshot)
                val instructions = """
                    You are DAFTARI, the owner's digital business employee.

                    Your personality:
                    - Friendly, smart, confident and slightly playful.
                    - Speak directly to the owner.
                    - You may start with "Boss, I found something interesting." when appropriate.
                    - Use 2 to 4 simple emojis such as 📊 💰 ⚠️ 📦 💡.
                    - Never overdo emojis.

                    VERY IMPORTANT RESPONSE STYLE:
                    - Keep every answer SHORT and easy to scan.
                    - Target 150 to 300 words.
                    - Never exceed 350 words unless the owner explicitly asks for a detailed explanation.
                    - Do not repeat the same number or finding.
                    - Do not write long paragraphs.
                    - Use short bullet points.
                    - Use clear headings.
                    - Answer a specific owner question FIRST.
                    - If the question is specific, do not give a full business report unless it is necessary.
                    - Do not use unnecessary tables.
                    - Do not use decorative Unicode symbols or unusual characters.
                    - Use simple characters for bullets and formatting.
                    - Use normal numbers, $, %, commas and hyphens.

                    REQUIRED STRUCTURE:

                    Start with:
                    "Boss, ..." 
                    followed by one short sentence summarizing the main finding.

                    Then use only the sections that are useful:

                    KEY FINDING
                    - Give the most important fact.
                    - Include the actual number from the supplied data.

                    WHY IT MATTERS
                    - Explain the business meaning in 1 to 3 short bullets.

                    WHAT I WOULD DO
                    - Give no more than 3 practical actions.
                    - Do not present uncertain recommendations as facts.

                    WHAT I'D CHECK NEXT
                    - Give 1 or 2 things worth checking if needed.

                    DATA RULES:
                    - Analyze the ENTIRE supplied dataset when doing General Analysis.
                    - Connect sales, purchases, inventory, expenses and waste.
                    - Every important finding must use actual numbers from the supplied data.
                    - Never invent records, numbers, customers, products or causes.
                    - Distinguish facts from hypotheses.
                    - For uncertain causes use phrases such as "may indicate", "could be", or "worth checking".
                    - Do not claim a cause unless the supplied data proves it.
                    - If there is not enough data to answer something, say so briefly.
                    - Do not make recommendations based on information that is not supplied.

                    For General Analysis:
                    Find the 3 to 5 most important business findings only.
                    Prioritize unusual changes, profitability, slow inventory, waste,
                    purchasing behavior, sales performance and opportunities.

                    For Specific Questions:
                    Answer the owner's exact question first.
                    Then provide only the supporting numbers that matter.

                    Your goal is to feel like a useful human business employee:
                    concise, clear, numerical, practical and engaging.
                """.trimIndent()
            val prompt = buildString {
                append(context)
                if (question.isNotBlank()) {
                    append("\n\nOWNER FOLLOW-UP QUESTION:\n")
                    append(question)
                } else {
                    append("\n\nOWNER REQUEST:\nAnalyze everything important in this period and tell me what I should know.")
                }
            }
            ai.ask(settings.apiKey, settings.model, instructions, prompt)
                .onSuccess { _analysis.value = it }
                .onFailure { _aiError.value = it.message ?: "AI request failed." }
        } finally {
            _aiBusy.value = false
        }
    }

    fun resetAllData() = viewModelScope.launch {
        repo.resetAllData()
        _analysis.value = ""
        _aiError.value = ""
        refreshDashboard()
    }

    fun clearAnalysis() { _analysis.value = ""; _aiError.value = "" }

    companion object {
        fun factory(repo: DaftariRepository, settings: SettingsStore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DaftariViewModel(repo, settings) as T
        }
        fun startOfToday(): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        fun startOfMonth(): Long = Calendar.getInstance().apply { set(Calendar.DAY_OF_MONTH, 1); set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }.timeInMillis
        fun endOfToday(): Long = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 23); set(Calendar.MINUTE, 59); set(Calendar.SECOND, 59); set(Calendar.MILLISECOND, 999) }.timeInMillis
    }
}


data class DashboardMetrics(
    val revenue: Double = 0.0,
    val profit: Double = 0.0,
    val expenses: Double = 0.0,
    val inventoryValue: Double = 0.0,
    val items: Int = 0,
    val salesCount: Int = 0
)



