//
//  JNexusApp.swift
//  JNexus (macOS)
//
//  macOS application entry point
//

import SwiftUI

@main
struct JNexusApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
                .frame(minWidth: 800, minHeight: 600)
        }
        .commands {
            MenuCommands()
        }
        .windowStyle(.titleBar)
        .windowToolbarStyle(.unified)

        // Settings window (separate)
        Settings {
            SettingsView()
                .environmentObject(appState)
                .frame(width: 500, height: 600)
        }
    }
}
