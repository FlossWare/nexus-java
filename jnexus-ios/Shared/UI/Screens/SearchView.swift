//
//  SearchView.swift
//  JNexus
//
//  Advanced search screen with filters
//

import SwiftUI

struct SearchView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.horizontalSizeClass) var horizontalSizeClass

    @State private var repository = ""
    @State private var showFilters = false

    // Filters
    @State private var minSize = ""
    @State private var maxSize = ""
    @State private var createdAfter = Date()
    @State private var createdBefore = Date()
    @State private var useCreatedAfter = false
    @State private var useCreatedBefore = false
    @State private var fileExtension = ""
    @State private var regexPattern = ""

    @State private var components: [ComponentMetadata] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var selectedComponent: ComponentMetadata?

    var body: some View {
        NavigationView {
            VStack {
                // Repository input
                TextField("Repository", text: $repository)
                    .textFieldStyle(.roundedBorder)
                    .padding()

                // Filters toggle
                DisclosureGroup("Filters", isExpanded: $showFilters) {
                    VStack(spacing: 12) {
                        // Size range
                        HStack {
                            TextField("Min Size (bytes)", text: $minSize)
                                .textFieldStyle(.roundedBorder)
                                .keyboardType(.numberPad)
                            TextField("Max Size (bytes)", text: $maxSize)
                                .textFieldStyle(.roundedBorder)
                                .keyboardType(.numberPad)
                        }

                        // Date range
                        Toggle("Created After", isOn: $useCreatedAfter)
                        if useCreatedAfter {
                            DatePicker("Date", selection: $createdAfter, displayedComponents: [.date])
                                .labelsHidden()
                        }

                        Toggle("Created Before", isOn: $useCreatedBefore)
                        if useCreatedBefore {
                            DatePicker("Date", selection: $createdBefore, displayedComponents: [.date])
                                .labelsHidden()
                        }

                        // Extension and regex
                        TextField("File Extension (e.g., .jar)", text: $fileExtension)
                            .textFieldStyle(.roundedBorder)
                            .autocapitalization(.none)

                        TextField("Regex Pattern", text: $regexPattern)
                            .textFieldStyle(.roundedBorder)
                            .autocapitalization(.none)

                        // Clear filters button
                        Button("Clear Filters") {
                            clearFilters()
                        }
                        .buttonStyle(.bordered)
                    }
                    .padding()
                }
                .padding(.horizontal)

                // Search button
                Button(action: search) {
                    HStack {
                        if isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle())
                        }
                        Text("Search")
                    }
                    .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .disabled(isLoading || repository.isEmpty)
                .padding(.horizontal)

                // Error display
                if let error = errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .padding()
                }

                // Results list
                if horizontalSizeClass == .regular {
                    // iPad landscape: Split view
                    HSplitView {
                        resultsList
                            .frame(minWidth: 400)

                        Divider()

                        if let selected = selectedComponent {
                            ComponentDetailView(component: selected)
                        } else {
                            Text("Select a component")
                                .foregroundColor(.secondary)
                                .frame(maxWidth: .infinity, maxHeight: .infinity)
                        }
                    }
                } else {
                    // iPhone or iPad portrait: Single column
                    resultsList
                        .sheet(item: $selectedComponent) { component in
                            NavigationView {
                                ComponentDetailView(component: component)
                                    .navigationTitle("Component Details")
                                    .navigationBarTitleDisplayMode(.inline)
                                    .toolbar {
                                        ToolbarItem(placement: .navigationBarTrailing) {
                                            Button("Done") {
                                                selectedComponent = nil
                                            }
                                        }
                                    }
                            }
                        }
                }
            }
            .navigationTitle("Search Components")
        }
    }

    var resultsList: some View {
        List {
            ForEach(components) { component in
                ComponentRow(component: component)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        selectedComponent = component
                    }
            }
        }
        .listStyle(.plain)
        .overlay {
            if !isLoading && components.isEmpty && errorMessage == nil {
                Text("No results")
                    .foregroundColor(.secondary)
            }
        }
    }

    private func search() {
        guard let service = appState.service else {
            errorMessage = "Service not initialized. Please configure credentials in Settings."
            return
        }

        isLoading = true
        errorMessage = nil

        Task {
            do {
                var builder = SearchCriteria.Builder()
                    .repository(repository)

                // Apply filters
                if !regexPattern.isEmpty {
                    builder = builder.regexFilter(regexPattern)
                }

                if let min = Int64(minSize) {
                    builder = builder.minSize(min)
                }

                if let max = Int64(maxSize) {
                    builder = builder.maxSize(max)
                }

                if useCreatedAfter {
                    builder = builder.createdAfter(createdAfter)
                }

                if useCreatedBefore {
                    builder = builder.createdBefore(createdBefore)
                }

                if !fileExtension.isEmpty {
                    builder = builder.fileExtension(fileExtension)
                }

                let criteria = builder.build()
                let results = try await service.searchComponents(criteria: criteria, forceRefresh: true)

                await MainActor.run {
                    components = results
                    isLoading = false
                }
            } catch {
                await MainActor.run {
                    errorMessage = error.localizedDescription
                    isLoading = false
                }
            }
        }
    }

    private func clearFilters() {
        minSize = ""
        maxSize = ""
        createdAfter = Date()
        createdBefore = Date()
        useCreatedAfter = false
        useCreatedBefore = false
        fileExtension = ""
        regexPattern = ""
    }
}
