//
//  ContentView.swift
//  JNexus (macOS)
//
//  macOS main view with sidebar navigation
//

import SwiftUI

struct ContentView: View {
    @EnvironmentObject var appState: AppState
    @State private var selectedScreen: Screen = .list

    enum Screen: String, CaseIterable {
        case list = "List"
        case search = "Search"
        case stats = "Stats"

        var icon: String {
            switch self {
            case .list: return "list.bullet"
            case .search: return "magnifyingglass"
            case .stats: return "chart.bar"
            }
        }
    }

    var body: some View {
        NavigationSplitView {
            // Sidebar
            List(Screen.allCases, id: \.self, selection: $selectedScreen) { screen in
                Label(screen.rawValue, systemImage: screen.icon)
            }
            .listStyle(.sidebar)
            .navigationTitle("JNexus")
        } detail: {
            // Detail view
            switch selectedScreen {
            case .list:
                RepositoryListView()
            case .search:
                SearchView()
            case .stats:
                StatsView()
            }
        }
    }
}
