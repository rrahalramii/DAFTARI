package com.daftari.app.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.daftari.app.DaftariViewModel
import com.daftari.app.DashboardMetrics
import com.daftari.app.data.*
import java.text.SimpleDateFormat
import java.util.*

private enum class Tab(val label: String) { HOME("Home"), TRANSACTIONS("Transactions"), INVENTORY("Inventory"), REPORTS("Reports"), AI("AI Analyst") }
private val Blue = Color(0xFF0B63CE)
private val Green = Color(0xFF14804A)
private val Red = Color(0xFFC62828)
private val Muted = Color(0xFF64748B)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DaftariApp(vm: DaftariViewModel) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    val products by vm.products.collectAsState(initial = emptyList())
    val customers by vm.customers.collectAsState(initial = emptyList())
    val sales by vm.recentSales.collectAsState(initial = emptyList())
    val expenses by vm.recentExpenses.collectAsState(initial = emptyList())
    val purchases by vm.recentPurchases.collectAsState(initial = emptyList())
    val waste by vm.waste.collectAsState(initial = emptyList())
    val todaySales by vm.todaySales.collectAsState()
    val todayExpenses by vm.todayExpenses.collectAsState()
    val todayPurchases by vm.todayPurchases.collectAsState()
    val todayWaste by vm.todayWaste.collectAsState()
    val dashboard by vm.dashboard.collectAsState()
    val analysis by vm.analysis.collectAsState()
    val busy by vm.aiBusy.collectAsState()
    val error by vm.aiError.collectAsState()
    val configured by vm.apiConfigured.collectAsState()
    val inventoryValuations by vm.inventoryValuations.collectAsState(initial = emptyList())
    var showSettings by remember { mutableStateOf(false) }
    var quickDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.MenuBook, null, tint = Blue, modifier = Modifier.size(27.dp)); Spacer(Modifier.width(8.dp)); Text("DAFTARI", fontWeight = FontWeight.Bold) } },
                actions = { IconButton({ showSettings = true }) { Icon(Icons.Default.Settings, "Settings") } }
            )
        },
        bottomBar = { BoxWithConstraints { val compact = maxWidth < 430.dp; NavigationBar { Tab.values().forEach { t -> NavigationBarItem(selected = tab == t, onClick = { tab = t }, icon = { Icon(when(t){Tab.HOME->Icons.Default.Home;Tab.TRANSACTIONS->Icons.Default.ReceiptLong;Tab.INVENTORY->Icons.Default.Inventory2;Tab.REPORTS->Icons.Default.BarChart;Tab.AI->Icons.Default.AutoAwesome}, null) }, label = { Text(if (compact) when(t){Tab.HOME->"Home";Tab.TRANSACTIONS->"Tx";Tab.INVENTORY->"Stock";Tab.REPORTS->"Reports";Tab.AI->"AI"} else t.label, fontSize = if(compact) 9.sp else 10.sp, maxLines=1, softWrap=false, overflow=TextOverflow.Ellipsis) }) } } } }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.HOME -> DashboardScreen(dashboard, todaySales, todayPurchases, todayExpenses, todayWaste, products, customers, vm, { quickDialog = it })
                Tab.TRANSACTIONS -> TransactionsScreen(sales, expenses, purchases, waste, products, customers, vm)
                Tab.INVENTORY -> InventoryScreen(products, sales, inventoryValuations, vm)
                Tab.REPORTS -> ReportsScreen(products, sales, expenses, purchases, waste)
                Tab.AI -> AiScreen(vm, analysis, busy, error, configured)
            }
        }
    }
    if (showSettings) SettingsDialog(vm) { showSettings = false }
    when (quickDialog) {
        "sale" -> SaleDialog(products, customers, vm) { quickDialog = null }
        "purchase" -> PurchaseDialog(products, vm) { quickDialog = null }
        "expense" -> ExpenseDialog(vm) { quickDialog = null }
    }
}

@Composable
private fun DashboardScreen(m: DashboardMetrics, sales: List<Sale>, purchases: List<Purchase>, expenses: List<Expense>, waste: List<Waste>, products: List<Product>, customers: List<Customer>, vm: DaftariViewModel, open: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Today", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(dateText(System.currentTimeMillis()), color = Muted)
            Spacer(Modifier.height(8.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                if (maxWidth < 390.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { HomeAction("Sale", Icons.Default.AddShoppingCart, Modifier.weight(1f)) { open("sale") }; HomeAction("Purchase", Icons.Default.LocalShipping, Modifier.weight(1f)) { open("purchase") } }
                        HomeAction("Expense", Icons.Default.Payments, Modifier.fillMaxWidth()) { open("expense") }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) { HomeAction("Sale", Icons.Default.AddShoppingCart, Modifier.weight(1f)) { open("sale") }; HomeAction("Purchase", Icons.Default.LocalShipping, Modifier.weight(1f)) { open("purchase") }; HomeAction("Expense", Icons.Default.Payments, Modifier.weight(1f)) { open("expense") } }
                }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { MetricCard("Revenue", money(m.revenue), Green, Modifier.weight(1f)); MetricCard("Profit", money(m.profit), Blue, Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) { MetricCard("Expenses", money(m.expenses), Red, Modifier.weight(1f)); MetricCard("Sales", "${m.salesCount}", Blue, Modifier.weight(1f)) } }
        item { Text("Today's activity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
        if (sales.isEmpty() && purchases.isEmpty() && expenses.isEmpty() && waste.isEmpty()) item { EmptyState("No transactions recorded today.") }
        items(sales) { s -> TransactionRow("Sale - ${s.productName} x ${s.quantity}", s.totalRevenue, true, s.date) }
        items(purchases) { p -> TransactionRow("Purchase - ${p.productName} x ${p.quantity}", p.totalCost, false, p.date) }
        items(expenses) { e -> TransactionRow("Expense - ${e.category}", e.amount, false, e.date) }
        items(waste) { w -> TransactionRow("Waste - ${w.productName} x ${w.quantity}", w.estimatedCost, false, w.date) }
    }
}

@Composable private fun HomeAction(label: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) { FilledTonalButton(onClick, modifier = modifier) { Icon(icon, null); Spacer(Modifier.width(5.dp)); Text(label, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis) } }
@Composable private fun MetricCard(title: String, value: String, accent: Color, modifier: Modifier) { Card(modifier, shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(16.dp)) { Text(title, color = Muted); Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accent) } } }

@Composable
private fun TransactionsScreen(sales: List<Sale>, expenses: List<Expense>, purchases: List<Purchase>, waste: List<Waste>, products: List<Product>, customers: List<Customer>, vm: DaftariViewModel) {
    var type by remember { mutableStateOf("All") }
    var datePreset by remember { mutableStateOf("All time") }
    var customStart by remember { mutableLongStateOf(dayRange(Calendar.getInstance()).first) }
    var customEnd by remember { mutableLongStateOf(dayRange(Calendar.getInstance()).second) }
    var dateTarget by remember { mutableStateOf<String?>(null) }
    var productFilter by remember { mutableStateOf("All products") }
    var customerFilter by remember { mutableStateOf("All customers") }
    var query by remember { mutableStateOf("") }
    var editSale by remember { mutableStateOf<Sale?>(null) }
    var editPurchase by remember { mutableStateOf<Purchase?>(null) }
    var editExpense by remember { mutableStateOf<Expense?>(null) }
    var editWaste by remember { mutableStateOf<Waste?>(null) }
    var deleteTitle by remember { mutableStateOf<String?>(null) }
    var deleteAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    val now = System.currentTimeMillis()
    val dateRange = when (datePreset) {
        "Today" -> dayRange(Calendar.getInstance())
        "Yesterday" -> {
            val cal = Calendar.getInstance().apply { timeInMillis = now; add(Calendar.DAY_OF_YEAR, -1) }
            dayRange(cal)
        }
        "Last 7 days" -> (dayRange(Calendar.getInstance()).first - 6L * 86_400_000L) to dayRange(Calendar.getInstance()).second
        "This month" -> {
            val cal = Calendar.getInstance().apply { timeInMillis = now; set(Calendar.DAY_OF_MONTH, 1) }
            dayRange(cal)
        }
        "Custom" -> customStart to customEnd
        else -> Long.MIN_VALUE to Long.MAX_VALUE
    }
    val (rangeStart, rangeEnd) = dateRange
    val q = query.trim().lowercase()
    val productMatches = { name: String -> productFilter == "All products" || name == productFilter }
    val customerMatches = { name: String -> customerFilter == "All customers" || name == customerFilter }
    val inRange = { d: Long -> d in rangeStart..rangeEnd }
    val textMatches = { text: String -> q.isBlank() || text.lowercase().contains(q) }

    val fs = sales.filter { inRange(it.date) && productMatches(it.productName) && customerMatches(it.customerName) && textMatches("sale ${it.productName} ${it.customerName} ${it.totalRevenue}") }
    val fp = purchases.filter { inRange(it.date) && productMatches(it.productName) && textMatches("purchase ${it.productName} ${it.totalCost}") }
    val fe = expenses.filter { inRange(it.date) && textMatches("expense ${it.category} ${it.description} ${it.amount}") }
    val fw = waste.filter { inRange(it.date) && productMatches(it.productName) && textMatches("waste ${it.productName} ${it.reason} ${it.estimatedCost}") }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Transactions", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Search and filter all recorded transactions.", color = Muted)
        Spacer(Modifier.height(8.dp))
        FilterChips(listOf("All", "Sales", "Purchases", "Expenses", "Waste"), type) { type = it }
        Spacer(Modifier.height(6.dp))
        FilterChips(listOf("All time", "Today", "Yesterday", "Last 7 days", "This month", "Custom"), datePreset) { datePreset = it }
        if (datePreset == "Custom") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { DateButton("From", customStart) { dateTarget = "start" } }
                Box(Modifier.weight(1f)) { DateButton("To", customEnd) { dateTarget = "end" } }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) { SimpleDropdown(productFilter, listOf("All products") + products.map { it.name }.distinct()) { productFilter = it } }
            Box(Modifier.weight(1f)) { SimpleDropdown(customerFilter, listOf("All customers") + customers.map { it.name }.filter { it.isNotBlank() }.distinct()) { customerFilter = it } }
        }
        Spacer(Modifier.height(6.dp))
        OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Search") }, placeholder = { Text("Product, customer, category, reason...") }, singleLine = true)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxSize()) {
            if (type == "All" || type == "Sales") {
                items(fs) { s -> TransactionRow("Sale - ${s.productName} x ${s.quantity}", s.totalRevenue, true, s.date, { editSale = s }, { deleteTitle = "Delete this sale?"; deleteAction = { vm.deleteSale(s) } }) }
            }
            if (type == "All" || type == "Purchases") {
                items(fp) { x -> TransactionRow("Purchase - ${x.productName} x ${x.quantity}", x.totalCost, false, x.date, { editPurchase = x }, { deleteTitle = "Delete this purchase?"; deleteAction = { vm.deletePurchase(x) } }) }
            }
            if (type == "All" || type == "Expenses") {
                items(fe) { x -> TransactionRow("Expense - ${x.category}: ${x.description}", x.amount, false, x.date, { editExpense = x }, { deleteTitle = "Delete this expense?"; deleteAction = { vm.deleteExpense(x) } }) }
            }
            if (type == "All" || type == "Waste") {
                items(fw) { x -> TransactionRow("Waste - ${x.productName} x ${x.quantity} (${x.reason})", x.estimatedCost, false, x.date, { editWaste = x }, { deleteTitle = "Delete this waste entry?"; deleteAction = { vm.deleteWaste(x) } }) }
            }
            if ((type == "All" && fs.isEmpty() && fp.isEmpty() && fe.isEmpty() && fw.isEmpty()) ||
                (type == "Sales" && fs.isEmpty()) || (type == "Purchases" && fp.isEmpty()) ||
                (type == "Expenses" && fe.isEmpty()) || (type == "Waste" && fw.isEmpty())) {
                item { EmptyState("No transactions match these filters.") }
            }
        }
    }
    editSale?.let { old -> EditSaleDialog(old, products, customers, vm) { editSale = null } }
    editPurchase?.let { old -> EditPurchaseDialog(old, products, vm) { editPurchase = null } }
    editExpense?.let { old -> EditExpenseDialog(old, vm) { editExpense = null } }
    editWaste?.let { old -> EditWasteDialog(old, products, vm) { editWaste = null } }
    if (deleteTitle != null) DeleteConfirmDialog(title = deleteTitle!!, onDelete = { deleteAction?.invoke(); deleteTitle = null; deleteAction = null }, onCancel = { deleteTitle = null; deleteAction = null })
    dateTarget?.let { target -> DatePickerPopup(if (target == "start") customStart else customEnd) { picked -> if (target == "start") customStart = picked else customEnd = picked; dateTarget = null } }
}

private fun dayRange(cal: Calendar): Pair<Long, Long> {
    val start = cal.clone() as Calendar
    start.set(Calendar.HOUR_OF_DAY, 0); start.set(Calendar.MINUTE, 0); start.set(Calendar.SECOND, 0); start.set(Calendar.MILLISECOND, 0)
    val end = start.clone() as Calendar
    end.set(Calendar.HOUR_OF_DAY, 23); end.set(Calendar.MINUTE, 59); end.set(Calendar.SECOND, 59); end.set(Calendar.MILLISECOND, 999)
    return start.timeInMillis to end.timeInMillis
}

@Composable private fun DeleteConfirmDialog(title:String, onDelete:()->Unit, onCancel:()->Unit) {
    AlertDialog(onDismissRequest=onCancel,title={Text("Confirm delete")},text={Text(title)},confirmButton={Button(onClick=onDelete,colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("Delete")}},dismissButton={TextButton(onClick=onCancel){Text("Cancel")}})
}

@Composable private fun TransactionRow(title:String, amount:Double, positive:Boolean, date:Long, onEdit:(()->Unit)?=null, onDelete:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth().padding(vertical=5.dp), verticalAlignment=Alignment.CenterVertically) {
        Icon(if(positive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,null,tint=if(positive) Green else Red)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) { Text(title,maxLines=2,overflow=TextOverflow.Ellipsis); Text(dateText(date),fontSize=12.sp,color=Muted) }
        Spacer(Modifier.width(4.dp))
        Column(horizontalAlignment=Alignment.End) {
            Text((if(positive) "+" else "-")+money(amount),fontWeight=FontWeight.Bold,color=if(positive) Green else Red,maxLines=1,softWrap=false)
            if(onEdit!=null || onDelete!=null) Row { if(onEdit!=null) IconButton(onClick=onEdit,modifier=Modifier.size(34.dp)){Icon(Icons.Default.Edit,"Edit",tint=Blue)}; if(onDelete!=null) IconButton(onClick=onDelete,modifier=Modifier.size(34.dp)){Icon(Icons.Default.Delete,"Delete",tint=Red)} }
        }
    }
}

@Composable
private fun InventoryScreen(products: List<Product>, sales: List<Sale>, valuations: List<com.daftari.app.data.InventoryValuation>, vm: DaftariViewModel) {
    var add by remember { mutableStateOf(false) }; var wasteProduct by remember { mutableStateOf<Product?>(null) }; var sort by remember { mutableStateOf("Highest qty") }
    val valuationByProduct = valuations.associateBy { it.productId }; val inventoryValue = valuations.sumOf { it.value }; val units = products.sumOf { it.stockQty }
    val soldByProduct = sales.groupBy { it.productId }.mapValues { (_, v) -> v.sumOf { it.quantity } }
    val sorted = when(sort) { "Lowest qty" -> products.sortedBy { it.stockQty }; "Best sellers" -> products.sortedByDescending { soldByProduct[it.id] ?: 0 }; "Worst sellers" -> products.sortedBy { soldByProduct[it.id] ?: 0 }; else -> products.sortedByDescending { it.stockQty } }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Inventory",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
        Text("Current stock on this phone",color=Muted)
        Spacer(Modifier.height(8.dp))
        Card(shape=RoundedCornerShape(20.dp),colors=CardDefaults.cardColors(containerColor=Blue.copy(alpha=.08f))) { Column(Modifier.fillMaxWidth().padding(18.dp)) { Text("TOTAL INVENTORY VALUE",fontSize=12.sp,fontWeight=FontWeight.Bold,color=Muted); Text(money(inventoryValue),fontSize=30.sp,fontWeight=FontWeight.Bold,color=Blue); Text("${products.size} products - $units units - valued at purchase cost",fontSize=12.sp,color=Muted) } }
        Spacer(Modifier.height(8.dp))
        BoxWithConstraints(Modifier.fillMaxWidth()) { if(maxWidth < 390.dp) { Column(verticalArrangement=Arrangement.spacedBy(8.dp)) { Row(verticalAlignment=Alignment.CenterVertically){Text("Sort",fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp));SortMenu(sort,listOf("Highest qty","Lowest qty","Best sellers","Worst sellers")){sort=it};Spacer(Modifier.weight(1f));IconButton({add=true}){Icon(Icons.Default.AddCircle,"Add product",tint=Blue)}} } } else { Row(verticalAlignment=Alignment.CenterVertically){Text("Sort",fontWeight=FontWeight.Bold);Spacer(Modifier.width(8.dp));SortMenu(sort,listOf("Highest qty","Lowest qty","Best sellers","Worst sellers")){sort=it};Spacer(Modifier.weight(1f));IconButton({add=true}){Icon(Icons.Default.AddCircle,"Add product",tint=Blue)}} } }
        LazyColumn(verticalArrangement=Arrangement.spacedBy(9.dp)) { items(sorted) { p -> Card(shape=RoundedCornerShape(16.dp)){ Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){ Icon(Icons.Default.Inventory2,null,tint=Blue); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)){Text(p.name,fontWeight=FontWeight.Bold);Text("${p.category.ifBlank{"Uncategorized"}} - Buy ${money(p.purchaseCost)} - Sell ${money(p.sellingPrice)}",fontSize=12.sp,color=Muted);Text("Sold: ${soldByProduct[p.id] ?: 0}",fontSize=12.sp,color=Muted);Text("Current average cost: ${money(valuationByProduct[p.id]?.averageCost ?: p.purchaseCost)} / pc",fontSize=12.sp,color=Blue);Text("Inventory value: ${money(valuationByProduct[p.id]?.value ?: 0.0)}",fontSize=12.sp,color=Muted)}; Column(horizontalAlignment=Alignment.End){Text("${p.stockQty}",fontSize=20.sp,fontWeight=FontWeight.Bold,color=if(p.stockQty<=5)Red else Blue);Text("in stock",fontSize=11.sp,color=Muted)};IconButton({wasteProduct=p}){Icon(Icons.Default.DeleteSweep,"Record waste",tint=Red)} } } } }
    }
    if(add) ProductDialog(vm){add=false}; wasteProduct?.let{p->WasteDialog(p,vm){wasteProduct=null}}
}

@Composable private fun SortMenu(selected:String, options:List<String>, onSelect:(String)->Unit){ var expanded by remember{mutableStateOf(false)}; Box{OutlinedButton({expanded=true}){Text(selected);Icon(Icons.Default.ArrowDropDown,null)};DropdownMenu(expanded,{expanded=false}){options.forEach{DropdownMenuItem(text={Text(it)},onClick={onSelect(it);expanded=false})}}} }

@Composable
private fun ReportsScreen(products: List<Product>, sales: List<Sale>, expenses: List<Expense>, purchases: List<Purchase>, waste: List<Waste>) {
    var start by remember { mutableLongStateOf(DaftariViewModel.startOfMonth()) }; var end by remember { mutableLongStateOf(DaftariViewModel.endOfToday()) }; var dateTarget by remember { mutableStateOf<String?>(null) }; var type by remember { mutableStateOf("All") }; var product by remember { mutableStateOf("All products") }; var category by remember { mutableStateOf("All categories") }; var query by remember { mutableStateOf("") }
    val matchingSales=sales.filter{it.date in start..end && (product=="All products"||it.productName==product) && (category=="All categories"||products.find{p->p.id==it.productId}?.category==category) && (query.isBlank()||it.productName.contains(query,true)||it.customerName.contains(query,true))}
    val matchingPurchases=purchases.filter{it.date in start..end && (product=="All products"||it.productName==product) && (category=="All categories"||products.find{p->p.id==it.productId}?.category==category) && (query.isBlank()||it.productName.contains(query,true))}
    val matchingExpenses=expenses.filter{it.date in start..end && (query.isBlank()||it.category.contains(query,true)||it.description.contains(query,true))}
    val matchingWaste=waste.filter{it.date in start..end && (product=="All products"||it.productName==product) && (query.isBlank()||it.productName.contains(query,true)||it.reason.contains(query,true))}
    val filteredSales = if (type=="Purchases" || type=="Expenses" || type=="Waste") emptyList() else matchingSales
    val filteredPurchases = if (type=="Sales" || type=="Expenses" || type=="Waste") emptyList() else matchingPurchases
    val filteredExpenses = if (type=="Sales" || type=="Purchases" || type=="Waste") emptyList() else matchingExpenses
    val filteredWaste = if (type=="Sales" || type=="Purchases" || type=="Expenses") emptyList() else matchingWaste
    val revenue=filteredSales.sumOf{it.totalRevenue}; val cogs=filteredSales.sumOf{it.totalCost}; val exp=filteredExpenses.sumOf{it.amount}; val wasteCost=filteredWaste.sumOf{it.estimatedCost}; val purchaseSpend=filteredPurchases.sumOf{it.totalCost}; val profit=revenue-cogs-exp-wasteCost
    Column(Modifier.fillMaxSize().padding(16.dp)){ LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){ item{Text("Reports",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Filter historical business data for detailed answers.",color=Muted)}; item{BoxWithConstraints(Modifier.fillMaxWidth()){if(maxWidth<390.dp) Column(verticalArrangement=Arrangement.spacedBy(8.dp)){DateButton("From",start){dateTarget="start"};DateButton("To",end){dateTarget="end"}} else Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){Box(Modifier.weight(1f)){DateButton("From",start){dateTarget="start"}};Box(Modifier.weight(1f)){DateButton("To",end){dateTarget="end"}}}}}; item{FilterChips(listOf("All","Sales","Purchases","Expenses","Waste"),type){type=it}}; item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){SimpleDropdown(product,products.map{it.name}.distinct().let{listOf("All products")+it}){product=it};SimpleDropdown(category,products.map{it.category}.filter{it.isNotBlank()}.distinct().let{listOf("All categories")+it}){category=it}}}; item{OutlinedTextField(query,{query=it},modifier=Modifier.fillMaxWidth(),label={Text("Search")},placeholder={Text("Product, customer, category...")},singleLine=true)}; item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){ReportCard("Revenue",money(revenue),"${filteredSales.sumOf{it.quantity}} units sold",Modifier.weight(1f));ReportCard("Profit",money(profit),"After listed costs",Modifier.weight(1f))}}; item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){ReportCard("Purchases",money(purchaseSpend),"Selected period",Modifier.weight(1f));ReportCard("Expenses",money(exp),"Selected period",Modifier.weight(1f))}}; item{Text("Detailed results",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}; if(type=="All"||type=="Sales") items(filteredSales){TransactionRow("Sale - ${it.productName} x ${it.quantity}",it.totalRevenue,true,it.date)}; if(type=="All"||type=="Purchases") items(filteredPurchases){TransactionRow("Purchase - ${it.productName} x ${it.quantity}",it.totalCost,false,it.date)}; if(type=="All"||type=="Expenses") items(filteredExpenses){TransactionRow("Expense - ${it.category}: ${it.description}",it.amount,false,it.date)}; if(type=="All"||type=="Waste") items(filteredWaste){TransactionRow("Waste - ${it.productName} x ${it.quantity} (${it.reason})",it.estimatedCost,false,it.date)} } }
    dateTarget?.let{target->DatePickerPopup(if(target=="start")start else end){picked->if(target=="start")start=picked else end=picked;dateTarget=null}}
}

@Composable private fun ReportCard(title:String,value:String,sub:String,modifier:Modifier=Modifier){Card(modifier,shape=RoundedCornerShape(18.dp)){Column(Modifier.padding(14.dp)){Text(title,color=Muted);Text(value,fontSize=21.sp,fontWeight=FontWeight.Bold);Text(sub,fontSize=12.sp,color=Muted)}}}

@Composable
private fun AiScreen(vm: DaftariViewModel, analysis: String, busy: Boolean, error: String, configured: Boolean) {
    var start by remember { mutableLongStateOf(DaftariViewModel.startOfMonth()) }; var end by remember { mutableLongStateOf(DaftariViewModel.endOfToday()) }; var question by remember { mutableStateOf("") }; var dateTarget by remember { mutableStateOf<String?>(null) }; var mode by remember { mutableStateOf("General analysis") }
    Column(Modifier.fillMaxSize().padding(16.dp)){
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp)){
            item{Text("DAFTARI AI",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);Text("Your digital business employee",color=Muted)}
            item{Card(shape=RoundedCornerShape(18.dp),colors=CardDefaults.cardColors(containerColor=Blue.copy(alpha=.08f))){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.Badge,null,tint=Blue,modifier=Modifier.size(30.dp));Spacer(Modifier.width(10.dp));Column{Text("Boss, I'm ready to check the numbers.",fontWeight=FontWeight.Bold);Text("I will use only the data in your selected period.",fontSize=12.sp,color=Muted)}}}}
            if(!configured) item{Card(colors=CardDefaults.cardColors(containerColor=Color(0xFFFFF4E5))){Text("Add your Gemini API key in Settings before running AI analysis.",Modifier.padding(14.dp))}}
            item{Text("Analysis period",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()){DateButton("From",start){dateTarget="start"};DateButton("To",end){dateTarget="end"}}}
            item{FilterChips(listOf("General analysis","Specific question"),mode){mode=it}}
            if(mode=="Specific question") item{OutlinedTextField(question,{question=it},modifier=Modifier.fillMaxWidth(),label={Text("Ask your employee")},placeholder={Text("Which products should I consider buying less of?" )})}
            if(mode=="General analysis") item{Text("I'll examine sales, purchases, inventory, expenses and waste together and report what deserves your attention.",color=Muted,fontSize=13.sp)}
            item{Button(onClick={vm.analyze(start,end,if(mode=="Specific question")question else "")},enabled=!busy&&configured&& (mode=="General analysis"||question.isNotBlank()),modifier=Modifier.fillMaxWidth()){if(busy)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)else Icon(Icons.Default.AutoAwesome,null);Spacer(Modifier.width(8.dp));Text(if(busy)"Your employee is checking the books..." else if(mode=="Specific question")"Ask DAFTARI" else "Analyze my business")}}
            if(error.isNotBlank()) item{Text(error,color=Red)}
            item{Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){if(analysis.isBlank()){Column(Modifier.fillMaxWidth().padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally){Icon(Icons.Default.Insights,null,tint=Blue,modifier=Modifier.size(50.dp));Spacer(Modifier.height(10.dp));Text("Ready when you are, Boss.",fontWeight=FontWeight.Bold,textAlign=TextAlign.Center);Text("Choose a date interval and ask a question or run a general analysis.",color=Muted,textAlign=TextAlign.Center)}}else Text(analysis,Modifier.padding(18.dp),lineHeight=21.sp)}}
        }
    }
    dateTarget?.let{target->DatePickerPopup(if(target=="start")start else end){picked->if(target=="start")start=picked else end=picked;dateTarget=null}}
}

@Composable private fun FilterChips(options:List<String>,selected:String,onSelect:(String)->Unit){Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())){options.forEach{item->FilterChip(selected==item,{onSelect(item)},label={Text(item,fontSize=12.sp,maxLines=1,softWrap=false)})}}}
@Composable private fun SimpleDropdown(selected:String,options:List<String>,modifier:Modifier=Modifier.fillMaxWidth(),onSelect:(String)->Unit){var expanded by remember{mutableStateOf(false)};Box(modifier){OutlinedButton({expanded=true},modifier=Modifier.fillMaxWidth()){Text(selected,maxLines=1,overflow=TextOverflow.Ellipsis);Spacer(Modifier.weight(1f));Icon(Icons.Default.ArrowDropDown,null)};DropdownMenu(expanded,{expanded=false}){options.forEach{DropdownMenuItem(text={Text(it)},onClick={onSelect(it);expanded=false})}}}}

@Composable private fun DateButton(label:String,date:Long,onClick:()->Unit){OutlinedButton(onClick=onClick,modifier=Modifier.fillMaxWidth()){Text("$label\n${dateText(date)}",textAlign=TextAlign.Center,modifier=Modifier.fillMaxWidth())}}
@Composable private fun EditableDateField(label:String,date:Long,onPicked:(Long)->Unit){var open by remember{mutableStateOf(false)};OutlinedButton(onClick={open=true},modifier=Modifier.fillMaxWidth()){Text("$label: ${dateText(date)}",maxLines=1,softWrap=false,overflow=TextOverflow.Ellipsis)};if(open){DatePickerPopup(date){onPicked(it);open=false}}}
@Composable private fun DatePickerPopup(current:Long,onPicked:(Long)->Unit){val ctx=LocalContext.current;LaunchedEffect(Unit){val c=Calendar.getInstance().apply{timeInMillis=current};DatePickerDialog(ctx,{_,y,m,d->onPicked(Calendar.getInstance().apply{set(y,m,d,0,0,0);set(Calendar.MILLISECOND,0)}.timeInMillis)},c.get(Calendar.YEAR),c.get(Calendar.MONTH),c.get(Calendar.DAY_OF_MONTH)).show()}}

@Composable private fun EmptyState(text:String){Text(text,color=Muted,modifier=Modifier.fillMaxWidth().padding(24.dp),textAlign=TextAlign.Center)}

@Composable
private fun SettingsDialog(vm:DaftariViewModel,close:()->Unit){
    val ctx=LocalContext.current
    val store=remember{com.daftari.app.SettingsStore(ctx)}
    var key by remember{mutableStateOf(store.apiKey)}
    var model by remember{mutableStateOf(store.model)}
    var confirm1 by remember{mutableStateOf(false)}
    var confirm2 by remember{mutableStateOf(false)}
    AlertDialog(onDismissRequest=close,title={Text("Daftari Settings")},text={
        Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
            Text("AI provider: Gemini",color=Muted)
            OutlinedTextField(key,{key=it},label={Text("Gemini API key")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(model,{model=it},label={Text("Model")},singleLine=true,modifier=Modifier.fillMaxWidth())
            Text("For a competition prototype, the key is stored locally on this phone. For a public release, use a secure backend so the API key is never shipped to the APK.",fontSize=12.sp,color=Muted)
            Spacer(Modifier.height(6.dp))
            OutlinedButton(onClick={confirm1=true},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Red)){Icon(Icons.Default.DeleteForever,null);Spacer(Modifier.width(8.dp));Text("Reset All Business Data")}
        }
    },confirmButton={Button({vm.saveApiSettings(key,model);close()}){Text("Save")}},dismissButton={TextButton(close){Text("Cancel")}})
    if(confirm1) AlertDialog(onDismissRequest={confirm1=false},title={Text("Reset all data?",fontWeight=FontWeight.Bold)},text={Text("Are you sure you want to erase all DAFTARI business data? This cannot be undone.")},confirmButton={Button(onClick={confirm1=false;confirm2=true},colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("Yes, continue")}},dismissButton={TextButton({confirm1=false}){Text("Cancel")}})
    if(confirm2) AlertDialog(onDismissRequest={confirm2=false},title={Text("Final confirmation",fontWeight=FontWeight.Bold)},text={Text("This will permanently delete products, sales, purchases, expenses, customers, waste records, inventory FIFO layers, and reports. Your AI settings will remain. Continue?")},confirmButton={Button(onClick={confirm2=false;close();vm.resetAllData()},colors=ButtonDefaults.buttonColors(containerColor=Red)){Text("RESET EVERYTHING")}},dismissButton={TextButton({confirm2=false}){Text("Cancel")}})
}

@Composable private fun EditSaleDialog(old:Sale,products:List<Product>,customers:List<Customer>,vm:DaftariViewModel,close:()->Unit){
    var selected by remember{mutableStateOf(products.find{it.id==old.productId} ?: products.firstOrNull())}; var qty by remember{mutableStateOf(old.quantity.toString())}; var price by remember{mutableStateOf(old.unitPrice.toString())}; var customer by remember{mutableStateOf(old.customerName)}; var date by remember{mutableLongStateOf(old.date)}
    if(selected==null){AlertDialog(onDismissRequest=close,title={Text("Edit sale")},text={Text("The original product is no longer available.")},confirmButton={Button(close){Text("OK")}});return}
    AlertDialog(onDismissRequest=close,title={Text("Edit sale")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ProductDropdown(products,selected!!){selected=it;price=it.sellingPrice.toString()};OutlinedTextField(qty,{qty=it},label={Text("Quantity")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(price,{price=it},label={Text("Unit price")},singleLine=true,modifier=Modifier.fillMaxWidth());OutlinedTextField(customer,{customer=it},label={Text("Customer (optional)")},singleLine=true,modifier=Modifier.fillMaxWidth());EditableDateField("Date",date){date=it};Text("Use the date button to change the transaction date.",fontSize=11.sp,color=Muted)}},confirmButton={Button({vm.updateSale(old,selected!!,qty,price,customer,date);close()}){Text("Save changes")}},dismissButton={TextButton(close){Text("Cancel")}})
}

@Composable private fun EditPurchaseDialog(old:Purchase,products:List<Product>,vm:DaftariViewModel,close:()->Unit){var selected by remember{mutableStateOf(products.find{it.id==old.productId} ?: products.firstOrNull())};var qty by remember{mutableStateOf(old.quantity.toString())};var cost by remember{mutableStateOf(old.unitCost.toString())};var date by remember{mutableLongStateOf(old.date)};if(selected==null){AlertDialog(onDismissRequest=close,title={Text("Edit purchase")},text={Text("The original product is no longer available.")},confirmButton={Button(close){Text("OK")}});return};AlertDialog(onDismissRequest=close,title={Text("Edit purchase")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ProductDropdown(products,selected!!){selected=it;cost=it.purchaseCost.toString()};OutlinedTextField(qty,{qty=it},label={Text("Quantity")},singleLine=true);OutlinedTextField(cost,{cost=it},label={Text("Unit cost")},singleLine=true);EditableDateField("Date",date){date=it}}},confirmButton={Button({vm.updatePurchase(old,selected!!,qty,cost,date);close()}){Text("Save changes")}},dismissButton={TextButton(close){Text("Cancel")}})}

@Composable private fun EditExpenseDialog(old:Expense,vm:DaftariViewModel,close:()->Unit){var category by remember{mutableStateOf(old.category)};var desc by remember{mutableStateOf(old.description)};var amount by remember{mutableStateOf(old.amount.toString())};var date by remember{mutableLongStateOf(old.date)};AlertDialog(onDismissRequest=close,title={Text("Edit expense")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(category,{category=it},label={Text("Category")},singleLine=true);OutlinedTextField(desc,{desc=it},label={Text("Description")},singleLine=true);OutlinedTextField(amount,{amount=it},label={Text("Amount")},singleLine=true);EditableDateField("Date",date){date=it}}},confirmButton={Button({vm.updateExpense(old,category,desc,amount,date);close()}){Text("Save changes")}},dismissButton={TextButton(close){Text("Cancel")}})}

@Composable private fun EditWasteDialog(old:Waste,products:List<Product>,vm:DaftariViewModel,close:()->Unit){var selected by remember{mutableStateOf(products.find{it.id==old.productId} ?: products.firstOrNull())};var qty by remember{mutableStateOf(old.quantity.toString())};var reason by remember{mutableStateOf(old.reason)};var date by remember{mutableLongStateOf(old.date)};if(selected==null){AlertDialog(onDismissRequest=close,title={Text("Edit waste")},text={Text("The original product is no longer available.")},confirmButton={Button(close){Text("OK")}});return};AlertDialog(onDismissRequest=close,title={Text("Edit waste / damage")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ProductDropdown(products,selected!!){selected=it};OutlinedTextField(qty,{qty=it},label={Text("Quantity")},singleLine=true);OutlinedTextField(reason,{reason=it},label={Text("Reason")},singleLine=true);EditableDateField("Date",date){date=it}}},confirmButton={Button({vm.updateWaste(old,selected!!,qty,reason,date);close()}){Text("Save changes")}},dismissButton={TextButton(close){Text("Cancel")}})}

@Composable private fun ProductDialog(vm:DaftariViewModel,close:()->Unit){var name by remember{mutableStateOf("")};var sku by remember{mutableStateOf("")};var category by remember{mutableStateOf("")};var buy by remember{mutableStateOf("")};var sell by remember{mutableStateOf("")};var stock by remember{mutableStateOf("")};SimpleFormDialog("Add product",close,listOf("Product name" to name,"SKU" to sku,"Category" to category,"Purchase cost" to buy,"Selling price" to sell,"Opening stock" to stock),onValues={v->name=v[0];sku=v[1];category=v[2];buy=v[3];sell=v[4];stock=v[5]}){vm.addProduct(name,sku,category,buy,sell,stock);close()}}
@Composable private fun SaleDialog(products:List<Product>,customers:List<Customer>,vm:DaftariViewModel,close:()->Unit){if(products.isEmpty()){AlertDialog(onDismissRequest=close,title={Text("No products")},text={Text("Add a product to Inventory first.")},confirmButton={Button(close){Text("OK")}});return};var selected by remember{mutableStateOf(products.first())};var qty by remember{mutableStateOf("1")};var price by remember{mutableStateOf(selected.sellingPrice.toString())};var customer by remember{mutableStateOf("")};AlertDialog(onDismissRequest=close,title={Text("Record sale")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ProductDropdown(products,selected){selected=it;price=it.sellingPrice.toString()};OutlinedTextField(qty,{qty=it},label={Text("Quantity")},singleLine=true);OutlinedTextField(price,{price=it},label={Text("Unit price")},singleLine=true);OutlinedTextField(customer,{customer=it},label={Text("Customer (optional)")},singleLine=true);Text("Stock available: ${selected.stockQty}",color=Muted,fontSize=12.sp)}},confirmButton={Button({vm.addSale(selected,qty,price,customer,System.currentTimeMillis());close()}){Text("Save sale")}},dismissButton={TextButton(close){Text("Cancel")}})}
@Composable private fun PurchaseDialog(products:List<Product>,vm:DaftariViewModel,close:()->Unit){if(products.isEmpty()){AlertDialog(onDismissRequest=close,title={Text("No products")},text={Text("Add a product first.")},confirmButton={Button(close){Text("OK")}});return};var selected by remember{mutableStateOf(products.first())};var qty by remember{mutableStateOf("1")};var cost by remember{mutableStateOf(selected.purchaseCost.toString())};AlertDialog(onDismissRequest=close,title={Text("Record purchase")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){ProductDropdown(products,selected){selected=it;cost=it.purchaseCost.toString()};OutlinedTextField(qty,{qty=it},label={Text("Quantity")},singleLine=true);OutlinedTextField(cost,{cost=it},label={Text("Unit cost")},singleLine=true)}},confirmButton={Button({vm.addPurchase(selected,qty,cost,System.currentTimeMillis());close()}){Text("Save purchase")}},dismissButton={TextButton(close){Text("Cancel")}})}
@Composable private fun CustomerDialog(vm:DaftariViewModel,close:()->Unit){var name by remember{mutableStateOf("")};var phone by remember{mutableStateOf("")};var notes by remember{mutableStateOf("")};SimpleFormDialog("Add customer",close,listOf("Name" to name,"Phone" to phone,"Notes" to notes),onValues={v->name=v[0];phone=v[1];notes=v[2]}){vm.addCustomer(name,phone,notes);close()}}
@Composable private fun ExpenseDialog(vm:DaftariViewModel,close:()->Unit){var category by remember{mutableStateOf("Electricity")};var desc by remember{mutableStateOf("")};var amount by remember{mutableStateOf("")};SimpleFormDialog("Record expense",close,listOf("Category" to category,"Description" to desc,"Amount" to amount),onValues={v->category=v[0];desc=v[1];amount=v[2]}){vm.addExpense(category,desc,amount,System.currentTimeMillis());close()}}
@Composable private fun WasteDialog(product:Product,vm:DaftariViewModel,close:()->Unit){var qty by remember{mutableStateOf("1")};var reason by remember{mutableStateOf("Damaged")};AlertDialog(onDismissRequest=close,title={Text("Record waste / damage")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(product.name,fontWeight=FontWeight.Bold);OutlinedTextField(qty,{qty=it},label={Text("Quantity")},singleLine=true);OutlinedTextField(reason,{reason=it},label={Text("Reason")},singleLine=true);Text("This reduces stock and is included in AI analysis.",fontSize=12.sp,color=Muted)}},confirmButton={Button({vm.addWaste(product,qty,reason,System.currentTimeMillis());close()}){Text("Save")}},dismissButton={TextButton(close){Text("Cancel")}})}
@Composable private fun ProductDropdown(products:List<Product>,selected:Product,onSelect:(Product)->Unit){var expanded by remember{mutableStateOf(false)};Box{OutlinedButton({expanded=true},modifier=Modifier.fillMaxWidth()){Text(selected.name,maxLines=1,overflow=TextOverflow.Ellipsis);Spacer(Modifier.weight(1f));Icon(Icons.Default.ArrowDropDown,null)};DropdownMenu(expanded,{expanded=false}){products.forEach{p->DropdownMenuItem(text={Text(p.name)},onClick={onSelect(p);expanded=false})}}}}
@Composable private fun SimpleFormDialog(title:String,close:()->Unit,fields:List<Pair<String,String>>,onValues:(List<String>)->Unit,save:()->Unit){val values=remember(fields.size){fields.map{it.second}.toMutableStateList()};AlertDialog(onDismissRequest=close,title={Text(title)},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){fields.forEachIndexed{i,(label,_)->OutlinedTextField(values[i],{values[i]=it},label={Text(label)},singleLine=true,modifier=Modifier.fillMaxWidth())}}},confirmButton={Button({onValues(values);save()}){Text("Save")}},dismissButton={TextButton(close){Text("Cancel")}})}
private fun money(v:Double)=String.format(Locale.US,"$%.2f",v)
private fun dateText(ms:Long)=SimpleDateFormat("dd MMM yyyy",Locale.getDefault()).format(Date(ms))


