//
//  RepoRecord.swift
//  JNexus
//
//  Basic repository component record
//  Mirrors the Java RepoRecord from jnexus-core
//

import Foundation

/// Basic component record with essential fields
struct RepoRecord: Codable, Identifiable, Hashable {
    /// Unique component identifier
    let id: String

    /// File size in bytes
    let fileSize: Int64

    /// Component path in repository
    let path: String

    /// CodingKeys for JSON serialization
    enum CodingKeys: String, CodingKey {
        case id
        case fileSize
        case path
    }
}
