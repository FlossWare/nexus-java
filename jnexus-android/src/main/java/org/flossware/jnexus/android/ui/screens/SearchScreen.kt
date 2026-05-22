package org.flossware.jnexus.android.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.flossware.jnexus.*
import org.flossware.jnexus.android.NexusApplication
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(app: NexusApplication) {
    val credentials = app.credentials
    val service = app.service

    var repository by remember { mutableStateOf(credentials.defaultRepository ?: "") }
    var minSize by remember { mutableStateOf("") }
    var maxSize by remember { mutableStateOf("") }
    var createdAfter by remember { mutableStateOf("") }
    var createdBefore by remember { mutableStateOf("") }
    var extension by remember { mutableStateOf("") }
    var regexFilter by remember { mutableStateOf(credentials.defaultRegex ?: "") }
    var showFilters by remember { mutableStateOf(true) }

    var components by remember { mutableStateOf<List<ComponentMetadata>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedComponent by remember { mutableStateOf<ComponentMetadata?>(null) }
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

        // Filter toggle button
        OutlinedButton(
            onClick = { showFilters = !showFilters },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (showFilters) "Hide filters" else "Show filters"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (showFilters) "Hide Filters" else "Show Filters")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filters panel
        AnimatedVisibility(visible = showFilters) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Search Filters",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Size filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minSize,
                            onValueChange = { minSize = it },
                            label = { Text("Min Size (bytes)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = maxSize,
                            onValueChange = { maxSize = it },
                            label = { Text("Max Size (bytes)") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = createdAfter,
                            onValueChange = { createdAfter = it },
                            label = { Text("Created After (ISO)") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("2024-01-01T00:00:00Z") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = createdBefore,
                            onValueChange = { createdBefore = it },
                            label = { Text("Created Before (ISO)") },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("2024-12-31T23:59:59Z") },
                            singleLine = true
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Extension filter
                    OutlinedTextField(
                        value = extension,
                        onValueChange = { extension = it },
                        label = { Text("File Extension") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(".jar") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Regex filter
                    OutlinedTextField(
                        value = regexFilter,
                        onValueChange = { regexFilter = it },
                        label = { Text("Regex Filter") },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text(".*SNAPSHOT.*") },
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Clear filters button
                    OutlinedButton(
                        onClick = {
                            minSize = ""
                            maxSize = ""
                            createdAfter = ""
                            createdBefore = ""
                            extension = ""
                            regexFilter = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Clear Filters")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Search button
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    error = null
                    try {
                        val builder = SearchCriteria.Builder().repository(repository)

                        if (minSize.isNotBlank()) {
                            builder.minSize(minSize.toLongOrNull() ?: 0L)
                        }
                        if (maxSize.isNotBlank()) {
                            builder.maxSize(maxSize.toLongOrNull() ?: Long.MAX_VALUE)
                        }
                        if (createdAfter.isNotBlank()) {
                            builder.createdAfter(Instant.parse(createdAfter))
                        }
                        if (createdBefore.isNotBlank()) {
                            builder.createdBefore(Instant.parse(createdBefore))
                        }
                        if (extension.isNotBlank()) {
                            builder.fileExtension(extension)
                        }
                        if (regexFilter.isNotBlank()) {
                            builder.regexFilter(regexFilter)
                        }

                        components = withContext(Dispatchers.IO) {
                            service.searchComponents(builder.build(), false)
                        }
                    } catch (e: Exception) {
                        error = e.message
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && repository.isNotBlank()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Search")
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

        // Results count
        if (components.isNotEmpty()) {
            Text(
                text = "${components.size} components found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Results list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(components) { component ->
                ComponentCard(
                    component = component,
                    onClick = { selectedComponent = component },
                    onDelete = { /* Delete not available in search view */ }
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
}
