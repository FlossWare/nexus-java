//
//  AppState.swift
//  JNexus
//
//  Shared application state and dependency injection
//

import Foundation
import SwiftUI

/// Application state container for dependency injection
/// Provides singleton instances of credentials, HTTP client, and service
class AppState: ObservableObject {
    @Published var credentials: Credentials
    @Published var httpClient: NexusHttpClient?
    @Published var service: NexusService?

    init() {
        self.credentials = CredentialsKeychain()

        if credentials.hasCredentials() {
            self.httpClient = NexusClientURLSession(credentials: credentials)
            self.service = NexusService(client: httpClient!)
        }
    }

    /// Reinitialize services after credentials change
    func reinitializeServices() {
        if credentials.hasCredentials() {
            self.httpClient = NexusClientURLSession(credentials: credentials)
            self.service = NexusService(client: httpClient!)
        } else {
            self.httpClient = nil
            self.service = nil
        }
    }
}
