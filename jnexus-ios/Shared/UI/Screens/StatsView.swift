//
//  StatsView.swift
//  JNexus
//
//  Repository statistics and analytics screen
//

import SwiftUI

struct StatsView: View {
    @EnvironmentObject var appState: AppState

    @State private var repository = ""
    @State private var stats: RepositoryStats?
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 16) {
                    // Repository input
                    TextField("Repository", text: $repository)
                        .textFieldStyle(.roundedBorder)
                        .padding()

                    // Calculate button
                    Button(action: calculateStatistics) {
                        HStack {
                            if isLoading {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle())
                            }
                            Text("Calculate Statistics")
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

                    // Statistics display
                    if let stats = stats {
                        VStack(spacing: 16) {
                            overviewSection(stats: stats)
                            sizeDistributionSection(stats: stats)
                            fileTypesSection(stats: stats)
                            ageDistributionSection(stats: stats)
                            largestComponentsSection(stats: stats)
                        }
                        .padding()
                    }
                }
            }
            .navigationTitle("Repository Statistics")
        }
    }

    // MARK: - Overview Section

    private func overviewSection(stats: RepositoryStats) -> some View {
        GroupBox("Overview") {
            VStack(alignment: .leading, spacing: 8) {
                LabeledContent("Total Components", value: "\(stats.totalComponents)")
                LabeledContent("Total Size", value: String(format: "%.2f MB (%.2f GB)", stats.totalSizeMB, stats.totalSizeGB))
                LabeledContent("Average Size", value: String(format: "%.2f MB", stats.averageSizeMB))
                LabeledContent("Median Size", value: String(format: "%.2f MB", stats.medianSizeMB))
            }
        }
    }

    // MARK: - Size Distribution Section

    private func sizeDistributionSection(stats: RepositoryStats) -> some View {
        GroupBox("Size Distribution") {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(["<1MB", "1-10MB", "10-100MB", "100MB-1GB", ">1GB"], id: \.self) { bucket in
                    let count = stats.sizeDistribution[bucket] ?? 0
                    let percentage = stats.totalComponents > 0 ?
                        Double(count) / Double(stats.totalComponents) * 100 : 0

                    HStack {
                        Text(bucket)
                            .frame(width: 100, alignment: .leading)
                        ProgressView(value: percentage, total: 100)
                        Text("\(count) (\(String(format: "%.1f", percentage))%)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
    }

    // MARK: - File Types Section

    private func fileTypesSection(stats: RepositoryStats) -> some View {
        GroupBox("File Types") {
            VStack(alignment: .leading, spacing: 8) {
                let sortedTypes = stats.fileTypeBreakdown.sorted { $0.value > $1.value }.prefix(10)

                if sortedTypes.isEmpty {
                    Text("No data")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(Array(sortedTypes), id: \.key) { ext, size in
                        HStack {
                            Text(ext)
                                .frame(width: 120, alignment: .leading)
                            Spacer()
                            Text(String(format: "%.2f MB", Double(size) / 1_048_576))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                }
            }
        }
    }

    // MARK: - Age Distribution Section

    private func ageDistributionSection(stats: RepositoryStats) -> some View {
        GroupBox("Age Distribution") {
            VStack(alignment: .leading, spacing: 8) {
                ForEach(["Last 7 days", "Last 30 days", "Last 90 days", "Older than 90 days"], id: \.self) { range in
                    let count = stats.ageDistribution[range] ?? 0

                    HStack {
                        Text(range)
                            .frame(width: 150, alignment: .leading)
                        Spacer()
                        Text("\(count)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
    }

    // MARK: - Largest Components Section

    private func largestComponentsSection(stats: RepositoryStats) -> some View {
        GroupBox("Largest Components (Top 10)") {
            VStack(alignment: .leading, spacing: 8) {
                if stats.largestComponents.isEmpty {
                    Text("No data")
                        .foregroundColor(.secondary)
                } else {
                    ForEach(Array(stats.largestComponents.prefix(10))) { component in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(component.path)
                                .font(.caption)
                                .lineLimit(1)
                            Text(ByteCountFormatter.string(fromByteCount: component.fileSize, countStyle: .file))
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 2)
                    }
                }
            }
        }
    }

    // MARK: - Calculate Statistics

    private func calculateStatistics() {
        guard let service = appState.service,
              let httpClient = appState.httpClient else {
            errorMessage = "Service not initialized. Please configure credentials in Settings."
            return
        }

        isLoading = true
        errorMessage = nil

        Task {
            do {
                let components = try await httpClient.listComponentsWithMetadata(
                    repository: repository,
                    forceRefresh: true
                )

                let statistics = service.calculateStatistics(repository: repository, components: components)

                await MainActor.run {
                    stats = statistics
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
}
