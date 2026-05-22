//
//  ComponentMetadata.swift
//  JNexus
//
//  Enhanced component record with full metadata
//  Mirrors the Java ComponentMetadata from jnexus-core
//

import Foundation

/// Component record with detailed metadata
struct ComponentMetadata: Codable, Identifiable, Hashable {
    /// Unique component identifier
    let id: String

    /// File size in bytes
    let fileSize: Int64

    /// Component path in repository
    let path: String

    /// MIME content type (e.g., "application/java-archive")
    let contentType: String?

    /// Repository format (e.g., "maven2", "npm", "docker")
    let format: String?

    /// When component was created/uploaded
    let createdDate: Date?

    /// When component was last modified
    let lastModified: Date?

    /// Checksum (SHA1 preferred, MD5 fallback)
    let checksum: String?

    /// CodingKeys for JSON serialization
    enum CodingKeys: String, CodingKey {
        case id
        case fileSize
        case path
        case contentType
        case format
        case createdDate
        case lastModified
        case checksum
    }
}
