package com.sukashawarma.superapp.presentation.absensi.checklist

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistManageScreen(onExit: () -> Unit, viewModel: ChecklistManageViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    var showAddCategory by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manajemen Checklist") },
                navigationIcon = {
                    IconButton(onClick = onExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddCategory = true }) { Icon(Icons.Filled.Add, contentDescription = "Tambah kategori") }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    state.error ?: "",
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = MaterialTheme.colorScheme.error,
                )
                state.categories.isEmpty() -> Text("Belum ada kategori checklist.", modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(contentPadding = PaddingValues(bottom = 80.dp)) {
                    items(state.categories, key = { it.id }) { cat -> CategoryCard(cat, viewModel) }
                }
            }
        }
    }

    if (showAddCategory) {
        AddCategoryDialog(
            onDismiss = { showAddCategory = false },
            onConfirm = { name, phase -> viewModel.addCategory(name, phase); showAddCategory = false },
        )
    }
}

@Composable
private fun CategoryCard(cat: ManageCategory, viewModel: ChecklistManageViewModel) {
    var showAddItem by remember { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(cat.name, fontWeight = FontWeight.Bold)
                    Text(cat.phase.label, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = { showAddItem = true }) { Icon(Icons.Filled.Add, contentDescription = "Tambah item") }
                IconButton(onClick = { viewModel.deleteCategory(cat.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Hapus kategori") }
            }
            cat.items.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name + if (item.isRequired) " *" else "", modifier = Modifier.weight(1f))
                    IconButton(onClick = { viewModel.deleteItem(item.id) }) { Icon(Icons.Filled.Delete, contentDescription = "Hapus item") }
                }
            }
        }
    }

    if (showAddItem) {
        AddItemDialog(
            onDismiss = { showAddItem = false },
            onConfirm = { name, required -> viewModel.addItem(cat.id, name, required); showAddItem = false },
        )
    }
}

@Composable
private fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, ChecklistPhase) -> Unit) {
    var name by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf(ChecklistPhase.BUKA) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Kategori") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama kategori") })
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChecklistPhase.entries.forEach { p ->
                        FilterChip(selected = phase == p, onClick = { phase = p }, label = { Text(p.label) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, phase) }, enabled = name.isNotBlank()) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}

@Composable
private fun AddItemDialog(onDismiss: () -> Unit, onConfirm: (String, Boolean) -> Unit) {
    var name by remember { mutableStateOf("") }
    var required by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tambah Item") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama item") })
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = required, onCheckedChange = { required = it })
                    Text("Wajib")
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name, required) }, enabled = name.isNotBlank()) { Text("Simpan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } },
    )
}
