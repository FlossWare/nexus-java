// swift-tools-version: 5.9
// The swift-tools-version declares the minimum version of Swift Package Manager required to build this package.

import PackageDescription

let package = Package(
    name: "JNexus",
    platforms: [
        .iOS(.v16),
        .macOS(.v13)
    ],
    products: [
        .library(
            name: "JNexus",
            targets: ["JNexus"]
        )
    ],
    targets: [
        .target(
            name: "JNexus",
            path: "Shared"
        ),
        .testTarget(
            name: "JNexusTests",
            dependencies: ["JNexus"],
            path: "Tests/Shared"
        )
    ]
)
