package org.flossware.jnexus.android.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.flossware.jnexus.android.CredentialsAndroid
import org.flossware.jnexus.android.NexusApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(app: NexusApplication) {
    val credentials = app.credentials as? CredentialsAndroid

    var url by remember { mutableStateOf(credentials?.url ?: "") }
    var user by remember { mutableStateOf(credentials?.user ?: "") }
    var password by remember { mutableStateOf(credentials?.password ?: "") }
    var passwordVisible by remember { mutableStateOf(false) }
    var repositories by remember { mutableStateOf(credentials?.repositories?.joinToString(", ") ?: "") }
    var defaultRepo by remember { mutableStateOf(credentials?.defaultRepository ?: "") }
    var defaultRegex by remember { mutableStateOf(credentials?.defaultRegex ?: "") }
    var defaultDryRun by remember { mutableStateOf(credentials?.defaultDryRun ?: true) }
    var httpTimeout by remember { mutableStateOf(credentials?.httpTimeoutSeconds?.toString() ?: "30") }

    var saveSuccess by remember { mutableStateOf(false) }
    var saveError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            text = "Nexus Configuration",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Connection settings card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Connection",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Nexus URL") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("https://nexus.example.com") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = user,
                    onValueChange = { user = it },
                    label = { Text("Username") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    singleLine = true
                )
            }
        }

        // Repository settings card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Repositories",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = repositories,
                    onValueChange = { repositories = it },
                    label = { Text("Repository List") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("maven-releases, maven-snapshots") },
                    supportingText = { Text("Comma-separated list") }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = defaultRepo,
                    onValueChange = { defaultRepo = it },
                    label = { Text("Default Repository") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("maven-releases") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = defaultRegex,
                    onValueChange = { defaultRegex = it },
                    label = { Text("Default Regex Filter") },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(".*SNAPSHOT.*") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("Default Dry Run")
                    Switch(
                        checked = defaultDryRun,
                        onCheckedChange = { defaultDryRun = it }
                    )
                }
            }
        }

        // Advanced settings card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Advanced",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = httpTimeout,
                    onValueChange = { httpTimeout = it },
                    label = { Text("HTTP Timeout (seconds)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        }

        // Security info card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Row(modifier = Modifier.padding(16.dp)) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Credentials are encrypted with AES-256-GCM and stored securely using Android Keystore",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Success message
        if (saveSuccess) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    text = "Settings saved successfully",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Error message
        saveError?.let {
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
        }

        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    try {
                        credentials?.saveCredentials(url, user, password)
                        credentials?.saveRepositories(repositories)
                        credentials?.saveDefaults(defaultRepo, defaultRegex, defaultDryRun)
                        credentials?.saveHttpTimeout(httpTimeout.toIntOrNull() ?: 30)
                        saveSuccess = true
                        saveError = null
                    } catch (e: Exception) {
                        saveSuccess = false
                        saveError = "Failed to save: ${e.message}"
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save")
            }

            OutlinedButton(
                onClick = {
                    url = ""
                    user = ""
                    password = ""
                    repositories = ""
                    defaultRepo = ""
                    defaultRegex = ""
                    defaultDryRun = true
                    httpTimeout = "30"
                    credentials?.clearAll()
                    saveSuccess = false
                    saveError = null
                },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Clear")
            }
        }
    }
}
