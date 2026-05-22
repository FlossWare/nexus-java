package org.flossware.jnexus.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.flossware.jnexus.*
import org.flossware.jnexus.android.NexusApplication
import java.time.Instant

/**
 * Advanced search screen with filters.
 * <p>
 * Features:
 * - Size range filters (min/max bytes)
 * - Date range filters (created after/before)
 * - File extension filter
 * - Component name pattern filter
 * - Path regex filter
 * - Collapsible filter panel
 * </p>
 */
@Composable
fun SearchScreen(app: NexusApplication) {
    val credentials = app.getCredentials()
    val service = app.getService()

    var repository by remember { mutableStateOf(credentials.getDefaultRepository()) }
    var showFilters by remember { mutableStateOf(true) }

    // Filter state
    var minSize by remember { mutableStateOf("") }
    var maxSize by remember { mutableStateOf("") }
    var createdAfter by remember { mutableStateOf("") }
    var createdBefore by remember { mutableStateOf("") }
    var extension by remember { mutableStateOf("") }
    var namePattern by remember { mutableStateOf("") }
    var regexFilter by remember { mutableStateOf(credentials.getDefaultRegex()) }

    var components by remember { mutableStateOf<List<ComponentMetadata>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
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
            "Advanced Search",
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

        // Filter toggle button
        OutlinedButton(
            onClick = { showFilters = !showFilters },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                if (showFilters) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (showFilters) "Hide Filters" else "Show Filters"
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (showFilters) "Hide Filters" else "Show Filters")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Filters panel
        if (showFilters) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Filters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Size filters
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minSize,
                            onValueChange = { minSize = it },
                            label = { Text("Min Size (bytes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxSize,
                            onValueChange = { maxSize = it },
                            label = { Text("Max Size (bytes)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Date filters
                    OutlinedTextField(
                        value = createdAfter,
                        onValueChange = { createdAfter = it },
                        label = { Text("Created After (ISO 8601)") },
                        placeholder = { Text("2024-01-01T00:00:00Z") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = createdBefore,
                        onValueChange = { createdBefore = it },
                        label = { Text("Created Before (ISO 8601)") },
                        placeholder = { Text("2024-12-31T23:59:59Z") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Extension and name pattern
                    OutlinedTextField(
                        value = extension,
                        onValueChange = { extension = it },
                        label = { Text("File Extension") },
                        placeholder = { Text(".jar") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = namePattern,
                        onValueChange = { namePattern = it },
                        label = { Text("Component Name Pattern") },
                        placeholder = { Text("my-component") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Regex filter
                    OutlinedTextField(
                        value = regexFilter,
                        onValueChange = { regexFilter = it },
                        label = { Text("Path Regex") },
                        placeholder = { Text(".*SNAPSHOT.*") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Clear filters button
                    OutlinedButton(
                        onClick = {
                            minSize = ""
                            maxSize = ""
                            createdAfter = ""
                            createdBefore = ""
                            extension = ""
                            namePattern = ""
                            regexFilter = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Clear, "Clear")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear Filters")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Search button
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    error = null
                    try {
                        val builder = SearchCriteria.Builder()
                            .repository(repository)

                        // Apply filters
                        minSize.toLongOrNull()?.let { builder.minSize(it) }
                        maxSize.toLongOrNull()?.let { builder.maxSize(it) }
                        createdAfter.takeIf { it.isNotEmpty() }?.let {
                            try {
                                builder.createdAfter(Instant.parse(it))
                            } catch (e: Exception) {
                                error = "Invalid createdAfter date format"
                            }
                        }
                        createdBefore.takeIf { it.isNotEmpty() }?.let {
                            try {
                                builder.createdBefore(Instant.parse(it))
                            } catch (e: Exception) {
                                error = "Invalid createdBefore date format"
                            }
                        }
                        extension.takeIf { it.isNotEmpty() }?.let { builder.fileExtension(it) }
                        namePattern.takeIf { it.isNotEmpty() }?.let { builder.componentNamePattern(it) }
                        regexFilter.takeIf { it.isNotEmpty() }?.let { builder.regexFilter(it) }

                        if (error == null) {
                            val criteria = builder.build()
                            components = withContext(Dispatchers.IO) {
                                service.searchComponents(criteria, true)
                            }
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Search failed"
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && repository.isNotEmpty()
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(Icons.Default.Search, "Search", modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
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

        // Results count
        if (components.isNotEmpty()) {
            Text(
                "${components.size} results",
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
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = component.path().split("/").lastOrNull() ?: component.path(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Size: ${formatFileSize(component.fileSize())}",
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
                }
            }
        }
    }
}
