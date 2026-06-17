package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.*
import com.example.data.Habit
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()

                NavHost(navController = navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            viewModel = mainViewModel,
                            onAddHabit = { navController.navigate("add") },
                            onDeleteHabit = { mainViewModel.deleteHabit(it) }
                        )
                    }
                    composable("add") {
                        val currentCurrency by mainViewModel.currency.collectAsStateWithLifecycle()
                        AddHabitScreen(
                            currencySymbol = currentCurrency,
                            onSave = { name, costAmount, costFrequency ->
                                mainViewModel.addHabit(name, costAmount, costFrequency)
                                navController.popBackStack()
                            },
                            onCancel = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: MainViewModel, onAddHabit: () -> Unit, onDeleteHabit: (Int) -> Unit) {
    val habits by viewModel.uiState.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    var showCurrencyMenu by remember { mutableStateOf(false) }

    val currentTime by produceState(initialValue = System.currentTimeMillis()) {
        while (true) {
            delay(1000L)
            value = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habit Saver") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                actions = {
                    Box {
                        TextButton(onClick = { showCurrencyMenu = true }) {
                            Text(currency, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        DropdownMenu(
                            expanded = showCurrencyMenu,
                            onDismissRequest = { showCurrencyMenu = false }
                        ) {
                            listOf("$", "€", "£", "₹", "¥").forEach { symbol ->
                                DropdownMenuItem(
                                    text = { Text(symbol) },
                                    onClick = {
                                        viewModel.setCurrency(symbol)
                                        showCurrencyMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddHabit) {
                Icon(Icons.Filled.Add, contentDescription = "Add Habit")
            }
        }
    ) { innerPadding ->
        if (habits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("No habits tracked yet. Tap + to add one.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            val totalSaved = habits.sumOf { habit ->
                val elapsedTime = currentTime - habit.quitTimestamp
                val validTime = if (elapsedTime > 0) elapsedTime else 0L

                val timePeriodMs = when (habit.costFrequency) {
                    "DAILY" -> 86400000.0
                    "WEEKLY" -> 604800000.0
                    "MONTHLY" -> 2592000000.0 // 30 days
                    else -> 86400000.0
                }

                (validTime / timePeriodMs) * habit.costAmount
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    TotalSavedCard(totalSaved, currency)
                }
                items(habits, key = { it.id }) { habit ->
                    HabitCard(habit = habit, currentTime = currentTime, currency = currency, onDelete = { onDeleteHabit(habit.id) })
                }
            }
        }
    }
}

@Composable
fun TotalSavedCard(totalSaved: Double, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Total Money Saved", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = String.format("%s%.2f", currency, totalSaved),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun HabitCard(habit: Habit, currentTime: Long, currency: String, onDelete: () -> Unit) {
    val elapsedTime = currentTime - habit.quitTimestamp
    val validTime = if (elapsedTime > 0) elapsedTime else 0L

    val timePeriodMs = when (habit.costFrequency) {
        "DAILY" -> 86400000.0
        "WEEKLY" -> 604800000.0
        "MONTHLY" -> 2592000000.0 // 30 days
        else -> 86400000.0
    }

    val savedMoney = (validTime / timePeriodMs) * habit.costAmount

    val days = (validTime / 86400000).toInt()
    val hours = ((validTime % 86400000) / 3600000).toInt()
    val minutes = ((validTime % 3600000) / 60000).toInt()
    val seconds = ((validTime % 60000) / 1000).toInt()

    val timeString = buildString {
        if (days > 0) append("${days}d ")
        if (hours > 0 || days > 0) append("${hours}h ")
        if (minutes > 0 || hours > 0 || days > 0) append("${minutes}m ")
        append("${seconds}s")
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = habit.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Delete")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Time since quit:", style = MaterialTheme.typography.labelMedium)
            Text(timeString, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Money Saved (${habit.costFrequency.lowercase().replaceFirstChar { it.uppercase() }} Rate):", style = MaterialTheme.typography.labelMedium)
            Text(String.format("%s%.2f", currency, savedMoney), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(currencySymbol: String, onSave: (String, Double, String) -> Unit, onCancel: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var costAmount by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("DAILY") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Habit to Quit") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("What are you giving up to save money?", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Habit Name (e.g. Vaping, Coffee)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = costAmount,
                onValueChange = { costAmount = it },
                label = { Text("Cost Amount ($currencySymbol)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Text("How often do you spend this amount?", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FrequencyChip("DAILY", frequency) { frequency = it }
                FrequencyChip("WEEKLY", frequency) { frequency = it }
                FrequencyChip("MONTHLY", frequency) { frequency = it }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val amount = costAmount.toDoubleOrNull() ?: 0.0
                        if (name.isNotBlank() && amount > 0) {
                            onSave(name, amount, frequency)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && (costAmount.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text("Save & Quit")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrequencyChip(label: String, selectedFrequency: String, onSelect: (String) -> Unit) {
    FilterChip(
        selected = (label == selectedFrequency),
        onClick = { onSelect(label) },
        label = { Text(label.lowercase().replaceFirstChar { it.uppercase() }) }
    )
}
