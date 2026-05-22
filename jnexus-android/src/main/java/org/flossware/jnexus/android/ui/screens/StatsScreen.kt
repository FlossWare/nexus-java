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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(app: NexusApplication) {
    val credentials = app.credentials
    val service = app.service

    var repository by remember { mutableStateOf(credentials.defaultRepository ?: "") }
    var statistics by remember { mutableStateOf<RepositoryStats?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
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

        // Calculate statistics button
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
                            service.searchComponents(criteria, false)
                        }
                        statistics = withContext(Dispatchers.IO) {
                            service.calculateStatistics(repository, components)
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
                Text(
                    text = it,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Statistics display
        statistics?.let { stats ->
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Overview section
                item {
                    StatsCard(title = "Overview") {
                        StatRow("Total Components", stats.totalComponents().toString())
                        StatRow("Total Size", String.format("%.2f MB", stats.totalSizeMB))
                        StatRow("Total Size", String.format("%.2f GB", stats.totalSizeGB))
                        StatRow("Average Size", String.format("%.2f MB", stats.averageSizeMB))
                        StatRow("Median Size", String.format("%.2f MB", stats.medianSizeMB))
                    }
                }

                // Size distribution section
                item {
                    StatsCard(title = "Size Distribution") {
                        val distribution = stats.sizeDistribution()
                        StatRow("< 1 MB", distribution.getOrDefault("SIZE_RANGE_UNDER_1MB", 0).toString())
                        StatRow("1-10 MB", distribution.getOrDefault("SIZE_RANGE_1_TO_10MB", 0).toString())
                        StatRow("10-100 MB", distribution.getOrDefault("SIZE_RANGE_10_TO_100MB", 0).toString())
                        StatRow("100MB-1GB", distribution.getOrDefault("SIZE_RANGE_100MB_TO_1GB", 0).toString())
                        StatRow("> 1 GB", distribution.getOrDefault("SIZE_RANGE_OVER_1GB", 0).toString())
                    }
                }

                // File types section
                item {
                    StatsCard(title = "File Types (Top 10)") {
                        val fileTypes = stats.fileTypes().entries
                            .sortedByDescending { it.value }
                            .take(10)

                        if (fileTypes.isEmpty()) {
                            Text(
                                text = "No file types",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column {
                                fileTypes.forEach { (ext, size) ->
                                    StatRow(ext, String.format("%.2f MB", size / 1_000_000.0))
                                }
                            }
                        }
                    }
                }

                // Age distribution section
                item {
                    StatsCard(title = "Age Distribution") {
                        val ageDistribution = stats.ageDistribution()
                        StatRow("Last 7 days", ageDistribution.getOrDefault("AGE_LAST_7_DAYS", 0).toString())
                        StatRow("Last 30 days", ageDistribution.getOrDefault("AGE_LAST_30_DAYS", 0).toString())
                        StatRow("Last 90 days", ageDistribution.getOrDefault("AGE_LAST_90_DAYS", 0).toString())
                        StatRow("Older", ageDistribution.getOrDefault("AGE_OLDER", 0).toString())
                    }
                }

                // Largest components section
                item {
                    StatsCard(title = "Largest Components (Top 10)") {
                        val largest = stats.largestComponents().take(10)
                        if (largest.isEmpty()) {
                            Text(
                                text = "No components",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Column {
                                largest.forEach { component ->
                                    ComponentStatRow(
                                        component.path(),
                                        formatFileSize(component.fileSize())
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Empty state
        if (statistics == null && !isLoading && error == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Enter a repository and calculate statistics",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
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
fun ComponentStatRow(path: String, size: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Text(
            text = path,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 2
        )
        Text(
            text = size,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
    Divider()
}
