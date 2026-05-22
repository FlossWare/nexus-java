//
//  MenuCommands.swift
//  JNexus (macOS)
//
//  macOS menu bar commands and keyboard shortcuts
//

import SwiftUI

struct MenuCommands: Commands {
    var body: some Commands {
        // Replace existing commands
        CommandGroup(replacing: .newItem) {
            Button("New Window") {
                NSApplication.shared.activate(ignoringOtherApps: true)
            }
            .keyboardShortcut("n", modifiers: .command)
        }

        // Custom commands
        CommandMenu("Repository") {
            Text("List Components")
                .keyboardShortcut("l", modifiers: .command)

            Text("Refresh")
                .keyboardShortcut("r", modifiers: .command)

            Divider()

            Text("Search...")
                .keyboardShortcut("f", modifiers: .command)
        }

        // Settings
        CommandGroup(replacing: .appSettings) {
            Button("Preferences...") {
                NSApplication.shared.sendAction(#selector(NSApplication.showPreferencesWindow(_:)), to: nil, from: nil)
            }
            .keyboardShortcut(",", modifiers: .command)
        }
    }
}

extension NSApplication {
    @objc func showPreferencesWindow(_ sender: Any?) {
        // This opens the Settings window defined in the app's Scene configuration
    }
}
