//
//  SettingsView.swift
//  JNexus
//
//  Settings screen for credentials and configuration
//

import SwiftUI

struct SettingsView: View {
    @EnvironmentObject var appState: AppState

    @State private var url = ""
    @State private var username = ""
    @State private var password = ""
    @State private var showPassword = false

    @State private var repositoriesText = ""
    @State private var defaultRepository = ""
    @State private var defaultRegex = ""
    @State private var defaultDryRun = true

    @State private var httpTimeout = 30

    @State private var message: String?
    @State private var isError = false

    var body: some View {
        NavigationView {
            Form {
                // Connection section
                Section {
                    TextField("Nexus URL", text: $url)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)

                    TextField("Username", text: $username)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()

                    HStack {
                        if showPassword {
                            TextField("Password", text: $password)
                                .textInputAutocapitalization(.never)
                        } else {
                            SecureField("Password", text: $password)
                        }

                        Button(action: { showPassword.toggle() }) {
                            Image(systemName: showPassword ? "eye.slash" : "eye")
                                .foregroundColor(.secondary)
                        }
                    }
                } header: {
                    Text("Connection")
                }

                // Repositories section
                Section {
                    TextField("Repositories (comma-separated)", text: $repositoriesText, axis: .vertical)
                        .textInputAutocapitalization(.never)
                        .lineLimit(3...6)

                    TextField("Default Repository", text: $defaultRepository)
                        .textInputAutocapitalization(.never)

                    TextField("Default Regex", text: $defaultRegex)
                        .textInputAutocapitalization(.never)

                    Toggle("Default Dry Run", isOn: $defaultDryRun)
                } header: {
                    Text("Repositories")
                }

                // Advanced section
                Section {
                    Stepper("HTTP Timeout: \(httpTimeout)s", value: $httpTimeout, in: 10...300)
                } header: {
                    Text("Advanced")
                }

                // Security info
                Section {
                    Text("Credentials are encrypted and stored securely in the iOS Keychain using hardware-backed encryption when available.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                } header: {
                    Text("Security")
                }

                // Actions
                Section {
                    Button("Save Settings") {
                        saveSettings()
                    }
                    .frame(maxWidth: .infinity)

                    Button("Clear All Settings", role: .destructive) {
                        clearSettings()
                    }
                    .frame(maxWidth: .infinity)
                }

                // Message display
                if let msg = message {
                    Section {
                        Text(msg)
                            .foregroundColor(isError ? .red : .green)
                    }
                }
            }
            .navigationTitle("Settings")
            .onAppear(perform: loadSettings)
        }
    }

    private func loadSettings() {
        guard let credentials = appState.credentials as? CredentialsKeychain else { return }

        url = credentials.url ?? ""
        username = credentials.user ?? ""
        password = credentials.password ?? ""

        repositoriesText = credentials.repositories.joined(separator: ", ")
        defaultRepository = credentials.defaultRepository
        defaultRegex = credentials.defaultRegex
        defaultDryRun = credentials.defaultDryRun

        httpTimeout = credentials.httpTimeoutSeconds
    }

    private func saveSettings() {
        guard let credentials = appState.credentials as? CredentialsKeychain else {
            message = "Invalid credentials provider"
            isError = true
            return
        }

        do {
            try credentials.saveCredentials(url: url, user: username, password: password)

            let repos = repositoriesText.split(separator: ",").map { $0.trimmingCharacters(in: .whitespaces) }
            try credentials.saveRepositories(repos)

            try credentials.saveDefaults(repository: defaultRepository, regex: defaultRegex, dryRun: defaultDryRun)
            try credentials.saveHttpTimeout(httpTimeout)

            appState.reinitializeServices()

            message = "Settings saved successfully"
            isError = false
        } catch {
            message = "Failed to save: \(error.localizedDescription)"
            isError = true
        }
    }

    private func clearSettings() {
        guard let credentials = appState.credentials as? CredentialsKeychain else { return }

        do {
            try credentials.clearAll()
            loadSettings()
            appState.reinitializeServices()

            message = "All settings cleared"
            isError = false
        } catch {
            message = "Failed to clear: \(error.localizedDescription)"
            isError = true
        }
    }
}
