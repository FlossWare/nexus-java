package org.flossware.jnexus.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.flossware.jnexus.android.CredentialsAndroid
import org.flossware.jnexus.android.NexusApplication

/**
 * Settings screen for configuring credentials and options.
 * <p>
 * Features:
 * - Nexus server URL
 * - Username and password (encrypted storage)
 * - Repository list configuration
 * - Default values (repository, regex, dry-run)
 * - HTTP timeout configuration
 * - Save/clear functionality
 * </p>
 */
@Composable
fun SettingsScreen(app: NexusApplication) {
    val credentials = app.getCredentials()
    val scrollState = rememberScrollState()

    var url by remember { mutableStateOf(credentials.getUrl() ?: "") }
    var user by remember { mutableStateOf(credentials.getUser() ?: "") }
    var password by remember { mutableStateOf(credentials.getPassword() ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    var repositories by remember {
        mutableStateOf(credentials.getRepositories().joinToString(", "))
    }
    var defaultRepo by remember { mutableStateOf(credentials.getDefaultRepository()) }
    var defaultRegex by remember { mutableStateOf(credentials.getDefaultRegex()) }
    var defaultDryRun by remember { mutableStateOf(credentials.getDefaultDryRun()) }
    var httpTimeout by remember { mutableStateOf(credentials.getHttpTimeoutSeconds().toString()) }

    var showSuccess by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            "Settings",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Connection settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Connection",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Nexus URL") },
                    placeholder = { Text("https://nexus.example.com") },
                    leadingIcon = { Icon(Icons.Default.Link, "URL") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, "User") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, "Password") },
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showPassword) "Hide password" else "Show password"
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = httpTimeout,
                    onValueChange = { httpTimeout = it },
                    label = { Text("HTTP Timeout (seconds)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    leadingIcon = { Icon(Icons.Default.Timer, "Timeout") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Repository settings
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Repositories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = repositories,
                    onValueChange = { repositories = it },
                    label = { Text("Repository List") },
                    placeholder = { Text("maven-releases, maven-snapshots, npm-public") },
                    leadingIcon = { Icon(Icons.Default.Storage, "Repositories") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                Text(
                    "Comma-separated list of repository names",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Default values
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Defaults",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = defaultRepo,
                    onValueChange = { defaultRepo = it },
                    label = { Text("Default Repository") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = defaultRegex,
                    onValueChange = { defaultRegex = it },
                    label = { Text("Default Regex Filter") },
                    placeholder = { Text(".*SNAPSHOT.*") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = defaultDryRun,
                        onCheckedChange = { defaultDryRun = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Default Dry-Run Mode")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Success message
        if (showSuccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Settings saved successfully",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Error message
        showError?.let {
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
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    try {
                        // Validate required fields
                        if (url.isEmpty() || user.isEmpty() || password.isEmpty()) {
                            showError = "URL, username, and password are required"
                            showSuccess = false
                            return@Button
                        }

                        // Save credentials
                        credentials.saveCredentials(url, user, password)
                        credentials.saveRepositories(repositories)
                        credentials.saveDefaults(defaultRepo, defaultRegex, defaultDryRun)
                        credentials.saveHttpTimeout(httpTimeout.toIntOrNull() ?: 30)

                        // Reinitialize services
                        app.reinitializeServices()

                        showSuccess = true
                        showError = null
                    } catch (e: Exception) {
                        showError = "Failed to save: ${e.message}"
                        showSuccess = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Save, "Save", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }

            OutlinedButton(
                onClick = {
                    try {
                        credentials.clearAll()
                        url = ""
                        user = ""
                        password = ""
                        repositories = ""
                        defaultRepo = ""
                        defaultRegex = ""
                        defaultDryRun = true
                        httpTimeout = "30"
                        showSuccess = false
                        showError = null
                    } catch (e: Exception) {
                        showError = "Failed to clear: ${e.message}"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Delete, "Clear", modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Security",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Credentials are encrypted using AES256_GCM and stored securely in Android's EncryptedSharedPreferences.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
