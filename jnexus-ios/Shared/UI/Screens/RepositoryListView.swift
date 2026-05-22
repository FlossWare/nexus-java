//
//  RepositoryListView.swift
//  JNexus
//
//  Repository component list screen with list/refresh/delete operations
//

import SwiftUI

struct RepositoryListView: View {
    @EnvironmentObject var appState: AppState
    @Environment(\.horizontalSizeClass) var horizontalSizeClass

    @State private var repository = ""
    @State private var components: [ComponentMetadata] = []
    @State private var isLoading = false
    @State private var errorMessage: String?
    @State private var selectedComponent: ComponentMetadata?
    @State private var componentToDelete: ComponentMetadata?
    @State private var showingDeleteConfirmation = false

    var body: some View {
        NavigationView {
            VStack {
                // Repository input
                TextField("Repository", text: $repository)
                    .textFieldStyle(.roundedBorder)
                    .padding()

                // List/Refresh buttons
                HStack {
                    Button(action: { fetchComponents(forceRefresh: false) }) {
                        HStack {
                            if isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                            }
                            Text("List Components")
                        }
                        .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(isLoading || repository.isEmpty)

                    Button(action: { fetchComponents(forceRefresh: true) }) {
                        Text("Refresh")
                            .frame(maxWidth: .infinity)
                    }
                    .buttonStyle(.bordered)
                    .disabled(isLoading || repository.isEmpty)
                }
                .padding(.horizontal)

                // Error display
                if let error = errorMessage {
                    Text(error)
                        .foregroundColor(.red)
                        .padding()
                }

                // Components list with adaptive layout
                if horizontalSizeClass == .regular {
                    // iPad landscape: Split view
                    HSplitView {
                        componentList
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
                    componentList
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
            .navigationTitle("Repository Components")
            .alert("Delete Component", isPresented: $showingDeleteConfirmation, presenting: componentToDelete) { component in
                Button("Cancel", role: .cancel) {}
                Button("Delete", role: .destructive) {
                    deleteComponent(component)
                }
            } message: { component in
                Text("Are you sure you want to delete \(component.path)?")
            }
        }
    }

    var componentList: some View {
        List {
            ForEach(components) { component in
                ComponentRow(component: component)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        selectedComponent = component
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            componentToDelete = component
                            showingDeleteConfirmation = true
                        } label: {
                            Label("Delete", systemImage: "trash")
                        }
                    }
            }
        }
        .listStyle(.plain)
    }

    private func fetchComponents(forceRefresh: Bool) {
        guard let service = appState.service else {
            errorMessage = "Service not initialized. Please configure credentials in Settings."
            return
        }

        isLoading = true
        errorMessage = nil

        Task {
            do {
                let criteria = SearchCriteria.Builder()
                    .repository(repository)
                    .build()

                let results = try await service.searchComponents(criteria: criteria, forceRefresh: forceRefresh)

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

    private func deleteComponent(_ component: ComponentMetadata) {
        guard let httpClient = appState.httpClient else { return }

        Task {
            do {
                try await httpClient.deleteComponent(componentId: component.id)
                await MainActor.run {
                    components.removeAll { $0.id == component.id }
                }
            } catch {
                await MainActor.run {
                    errorMessage = "Delete failed: \(error.localizedDescription)"
                }
            }
        }
    }
}

// MARK: - Component Row

struct ComponentRow: View {
    let component: ComponentMetadata

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(component.path)
                .font(.body)

            HStack {
                Text("Size: \(ByteCountFormatter.string(fromByteCount: component.fileSize, countStyle: .file))")
                    .font(.caption)
                    .foregroundColor(.secondary)

                if let created = component.createdDate {
                    Text("• Created: \(created, style: .date)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Component Detail View

struct ComponentDetailView: View {
    let component: ComponentMetadata

    var body: some View {
        Form {
            Section("Basic Information") {
                LabeledContent("ID", value: component.id)
                LabeledContent("Path", value: component.path)
                LabeledContent("File Size", value: ByteCountFormatter.string(fromByteCount: component.fileSize, countStyle: .file))
            }

            if component.contentType != nil || component.format != nil {
                Section("Type Information") {
                    if let contentType = component.contentType {
                        LabeledContent("Content Type", value: contentType)
                    }
                    if let format = component.format {
                        LabeledContent("Format", value: format)
                    }
                }
            }

            if component.createdDate != nil || component.lastModified != nil {
                Section("Dates") {
                    if let created = component.createdDate {
                        LabeledContent("Created", value: created, format: .dateTime)
                    }
                    if let modified = component.lastModified {
                        LabeledContent("Last Modified", value: modified, format: .dateTime)
                    }
                }
            }

            if let checksum = component.checksum {
                Section("Integrity") {
                    LabeledContent("Checksum", value: checksum)
                        .font(.system(.caption, design: .monospaced))
                }
            }
        }
    }
}
