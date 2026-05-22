package org.flossware.jnexus.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.flossware.jnexus.*
import org.flossware.jnexus.android.NexusApplication
import java.text.NumberFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepositoryListScreen(app: NexusApplication) {
    val credentials = app.credentials
    val service = app.service

    var repository by remember { mutableStateOf(credentials.defaultRepository ?: "") }
    var components by remember { mutableStateOf<List<ComponentMetadata>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedComponent by remember { mutableStateOf<ComponentMetadata?>(null) }
    var showDeleteConfirm by remember { mutableStateOf<ComponentMetadata?>(null) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Repository input
        OutlinedTextField(
            value = repository,
            onValueChange = { repository = it },
            label = { Text("Repository") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            val criteria = SearchCriteria.Builder()
                                .repository(repository)
                                .build()
                            components = withContext(Dispatchers.IO) {
                                service.searchComponents(criteria, false)
                            }
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && repository.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("List")
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        isLoading = true
                        error = null
                        try {
                            val criteria = SearchCriteria.Builder()
                                .repository(repository)
                                .build()
                            components = withContext(Dispatchers.IO) {
                                service.searchComponents(criteria, true)
                            }
                        } catch (e: Exception) {
                            error = e.message
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && repository.isNotBlank()
            ) {
                Text("Refresh")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error display
        error?.let {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Component count
        if (components.isNotEmpty()) {
            Text(
                text = "${components.size} components",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Components list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(components) { component ->
                ComponentCard(
                    component = component,
                    onClick = { selectedComponent = component },
                    onDelete = { showDeleteConfirm = component }
                )
            }
        }
    }

    // Component details dialog
    selectedComponent?.let { component ->
        AlertDialog(
            onDismissRequest = { selectedComponent = null },
            title = { Text("Component Details") },
            text = {
                Column {
                    DetailRow("Path", component.path())
                    DetailRow("Size", formatFileSize(component.fileSize()))
                    component.contentType()?.let { DetailRow("Content Type", it) }
                    component.format()?.let { DetailRow("Format", it) }
                    component.createdDate()?.let { DetailRow("Created", it.toString()) }
                    component.lastModified()?.let { DetailRow("Modified", it.toString()) }
                    component.checksum()?.let { DetailRow("Checksum", it) }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedComponent = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete confirmation dialog
    showDeleteConfirm?.let { component ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("Delete Component") },
            text = { Text("Are you sure you want to delete ${component.path()}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    app.httpClient.deleteComponent(component.id())
                                }
                                components = components.filter { it.id() != component.id() }
                                showDeleteConfirm = null
                            } catch (e: Exception) {
                                error = "Delete failed: ${e.message}"
                                showDeleteConfirm = null
                            }
                        }
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ComponentCard(
    component: ComponentMetadata,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.path(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatFileSize(component.fileSize()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(text = value)
    }
}

fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
        bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
        bytes >= 1_000 -> String.format("%.2f KB", bytes / 1_000.0)
        else -> "$bytes bytes"
    }
}
