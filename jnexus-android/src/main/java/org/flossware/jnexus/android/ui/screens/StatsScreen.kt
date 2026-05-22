package org.flossware.jnexus.android.ui.screens

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

/**
 * Repository statistics screen.
 * <p>
 * Features:
 * - Total components and size
 * - Size distribution breakdown
 * - File type analysis
 * - Age distribution
 * - Largest components list
 * </p>
 */
@Composable
fun StatsScreen(app: NexusApplication) {
    val credentials = app.getCredentials()
    val service = app.getService()

    var repository by remember { mutableStateOf(credentials.getDefaultRepository()) }
    var stats by remember { mutableStateOf<RepositoryStats?>(null) }
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
            "Repository Statistics",
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

        // Calculate button
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    error = null
                    try {
                        val criteria = SearchCriteria.Builder()
                            .repository(repository)
                            .build()
                        val components = withContext(Dispatchers.IO) {
                            service.searchComponents(criteria, true)
                        }
                        stats = withContext(Dispatchers.IO) {
                            service.calculateStatistics(repository, components)
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Failed to calculate statistics"
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
                Icon(Icons.Default.Analytics, "Calculate", modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text("Calculate Statistics")
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

        // Statistics display
        stats?.let { statistics ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overview
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Overview",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            StatRow("Total Components", "${statistics.totalComponents()}")
                            StatRow("Total Size", String.format("%.2f GB (%.2f MB)",
                                statistics.getTotalSizeGB(), statistics.getTotalSizeMB()))
                            StatRow("Average Size", String.format("%.2f MB",
                                statistics.getAverageSizeMB()))
                            StatRow("Median Size", String.format("%.2f MB",
                                statistics.getMedianSizeMB()))
                        }
                    }
                }

                // Size distribution
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Size Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val dist = statistics.sizeDistribution()
                            val total = statistics.totalComponents()
                            DistributionRow("< 1 MB", dist.getOrDefault("< 1 MB", 0), total)
                            DistributionRow("1-10 MB", dist.getOrDefault("1-10 MB", 0), total)
                            DistributionRow("10-100 MB", dist.getOrDefault("10-100 MB", 0), total)
                            DistributionRow("100 MB - 1 GB", dist.getOrDefault("100 MB - 1 GB", 0), total)
                            DistributionRow("> 1 GB", dist.getOrDefault("> 1 GB", 0), total)
                        }
                    }
                }

                // File types
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "File Types",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            statistics.fileTypes().entries
                                .sortedByDescending { it.value }
                                .take(10)
                                .forEach { (ext, size) ->
                                    StatRow(ext, String.format("%.2f MB", size / 1_000_000.0))
                                }
                        }
                    }
                }

                // Age distribution
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Age Distribution",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            val age = statistics.ageDistribution()
                            val total = statistics.totalComponents()
                            DistributionRow("Last 7 days", age.getOrDefault("last 7 days", 0), total)
                            DistributionRow("Last 30 days", age.getOrDefault("last 30 days", 0), total)
                            DistributionRow("Last 90 days", age.getOrDefault("last 90 days", 0), total)
                            DistributionRow("Older", age.getOrDefault("older", 0), total)
                        }
                    }
                }

                // Largest components
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Largest Components",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            statistics.largestComponents().take(10).forEach { component ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                ) {
                                    Text(
                                        text = component.path().split("/").lastOrNull() ?: component.path(),
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = formatFileSize(component.fileSize()),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun DistributionRow(label: String, count: Int, total: Int) {
    val percentage = if (total > 0) (count * 100.0 / total) else 0.0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(60.dp)
        )
        Text(
            text = String.format("%.1f%%", percentage),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
