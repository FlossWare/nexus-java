//
//  JNexusApp.swift
//  JNexus (iOS)
//
//  iOS application entry point
//

import SwiftUI

@main
struct JNexusApp: App {
    @StateObject private var appState = AppState()

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(appState)
        }
    }
}
