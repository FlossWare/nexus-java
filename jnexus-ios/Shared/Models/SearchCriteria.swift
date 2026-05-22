//
//  SearchCriteria.swift
//  JNexus
//
//  Search criteria with advanced filters
//  Mirrors the Java SearchCriteria from jnexus-core
//

import Foundation

/// Advanced search criteria for component filtering
struct SearchCriteria {
    let repository: String
    let regexFilter: String?
    let minSize: Int64?
    let maxSize: Int64?
    let createdAfter: Date?
    let createdBefore: Date?
    let fileExtension: String?
    let componentNamePattern: String?

    /// Builder for constructing SearchCriteria with fluent API
    struct Builder {
        private var repository: String = ""
        private var regexFilter: String?
        private var minSize: Int64?
        private var maxSize: Int64?
        private var createdAfter: Date?
        private var createdBefore: Date?
        private var fileExtension: String?
        private var componentNamePattern: String?

        func repository(_ value: String) -> Builder {
            var copy = self
            copy.repository = value
            return copy
        }

        func regexFilter(_ value: String?) -> Builder {
            var copy = self
            copy.regexFilter = value
            return copy
        }

        func minSize(_ value: Int64?) -> Builder {
            var copy = self
            copy.minSize = value
            return copy
        }

        func maxSize(_ value: Int64?) -> Builder {
            var copy = self
            copy.maxSize = value
            return copy
        }

        func createdAfter(_ value: Date?) -> Builder {
            var copy = self
            copy.createdAfter = value
            return copy
        }

        func createdBefore(_ value: Date?) -> Builder {
            var copy = self
            copy.createdBefore = value
            return copy
        }

        func fileExtension(_ value: String?) -> Builder {
            var copy = self
            copy.fileExtension = value
            return copy
        }

        func componentNamePattern(_ value: String?) -> Builder {
            var copy = self
            copy.componentNamePattern = value
            return copy
        }

        func build() -> SearchCriteria {
            SearchCriteria(
                repository: repository,
                regexFilter: regexFilter,
                minSize: minSize,
                maxSize: maxSize,
                createdAfter: createdAfter,
                createdBefore: createdBefore,
                fileExtension: fileExtension,
                componentNamePattern: componentNamePattern
            )
        }
    }
}
