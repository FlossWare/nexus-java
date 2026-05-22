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

/**
 * Repository list screen for browsing components.
 * <p>
 * Features:
 * - Repository selection from configured list
 * - List/Refresh buttons with cache control
 * - Component list with file sizes
 * - Delete functionality with confirmation
 * - Component metadata dialog on tap
 * </p>
 */
@Composable
fun RepositoryListScreen(app: NexusApplication) {
    val credentials = app.getCredentials()
    val service = app.getService()

    var repository by remember { mutableStateOf(credentials.getDefaultRepository()) }
    var components by remember { mutableStateOf<List<ComponentMetadata>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedComponent by remember { mutableStateOf<ComponentMetadata?>(null) }
    val scope = rememberCoroutineScope()

    // Check if service is initialized
    if (service == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Configure",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Please configure credentials in Settings",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Repository Components",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Repository selector
        val repositories = credentials.getRepositories()
        if (repositories.isNotEmpty()) {
            var expanded by remember { mutableStateOf(false) }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = repository,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Repository") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    repositories.forEach { repo ->
                        DropdownMenuItem(
                            text = { Text(repo) },
                            onClick = {
                                repository = repo
                                expanded = false
                            }
                        )
                    }
                }
            }
        } else {
            OutlinedTextField(
                value = repository,
                onValueChange = { repository = it },
                label = { Text("Repository") },
                modifier = Modifier.fillMaxWidth()
            )
        }

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
                                service.searchComponents(criteria, false) // Use cache
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Unknown error"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && repository.isNotEmpty()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.List, "List", modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("List")
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
                                service.searchComponents(criteria, true) // Force refresh
                            }
                        } catch (e: Exception) {
                            error = e.message ?: "Unknown error"
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && repository.isNotEmpty()
            ) {
                Icon(Icons.Default.Refresh, "Refresh", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
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
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Component count
        if (components.isNotEmpty()) {
            Text(
                "${components.size} components",
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
                    onDelete = {
                        scope.launch {
                            try {
                                withContext(Dispatchers.IO) {
                                    app.getHttpClient()?.deleteComponent(component.id())
                                }
                                // Remove from list
                                components = components.filter { it.id() != component.id() }
                            } catch (e: Exception) {
                                error = "Delete failed: ${e.message}"
                            }
                        }
                    }
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
                    DetailRow("Content Type", component.contentType() ?: "N/A")
                    DetailRow("Format", component.format() ?: "N/A")
                    component.createdDate()?.let {
                        DetailRow("Created", it.toString())
                    }
                    component.lastModified()?.let {
                        DetailRow("Modified", it.toString())
                    }
                    component.checksum()?.let {
                        DetailRow("Checksum", it.take(16) + "...")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedComponent = null }) {
                    Text("Close")
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
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = component.path().split("/").lastOrNull() ?: component.path(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatFileSize(component.fileSize()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                component.createdDate()?.let {
                    Text(
                        text = "Created: ${it.toString().take(10)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = { showDeleteDialog = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Confirm Delete") },
            text = { Text("Delete ${component.path()}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            "$label:",
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(100.dp)
        )
        Text(value, modifier = Modifier.weight(1f))
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
