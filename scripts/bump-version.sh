#!/usr/bin/env bash
# bump-version.sh - Automate version bumping across all platforms
#
# Usage:
#   ./scripts/bump-version.sh 2.1.0
#   ./scripts/bump-version.sh 2.1.0 "Release notes summary"
#
# This script updates version numbers in all platform files:
# - pom.xml (Desktop Maven)
# - jnexus-core/build.gradle (Shared library)
# - jnexus-android/build.gradle (Android app)
# - jnexus-ios/iOS/Info.plist (iOS app)
# - jnexus-ios/macOS/Info.plist (macOS app)
#
# It does NOT update CHANGELOG.md (you should do that manually)

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Get script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Check arguments
if [ -z "$1" ]; then
    echo -e "${RED}Error: Version number required${NC}"
    echo "Usage: $0 <version> [release-notes]"
    echo "Example: $0 2.1.0"
    exit 1
fi

NEW_VERSION="$1"
RELEASE_NOTES="${2:-Release v${NEW_VERSION}}"

# Validate version format (X.Y.Z or X.Y)
if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
    echo -e "${RED}Error: Invalid version format: $NEW_VERSION${NC}"
    echo "Expected format: X.Y or X.Y.Z (e.g., 2.1.0 or 2.1)"
    exit 1
fi

echo -e "${YELLOW}Bumping version to ${NEW_VERSION}${NC}"
echo ""

# Function to update version in file
update_file() {
    local file="$1"
    local old_version_pattern="$2"
    local new_version_value="$3"
    local file_type="$4"

    if [ ! -f "$file" ]; then
        echo -e "${YELLOW}⚠ Skipping (not found): $file${NC}"
        return 0
    fi

    if grep -q "$old_version_pattern" "$file"; then
        # Create backup
        cp "$file" "$file.bak"

        # Update version
        case "$file_type" in
            "xml")
                sed -i.tmp "s/$old_version_pattern/$new_version_value/" "$file"
                ;;
            "gradle")
                sed -i.tmp "s/$old_version_pattern/$new_version_value/" "$file"
                ;;
            "plist")
                sed -i.tmp "s/$old_version_pattern/$new_version_value/" "$file"
                ;;
        esac
        rm -f "$file.tmp"

        echo -e "${GREEN}✓ Updated: $file${NC}"
        return 0
    else
        echo -e "${YELLOW}⚠ Pattern not found in $file: $old_version_pattern${NC}"
        rm -f "$file.bak"
        return 1
    fi
}

FAILED=0

# 1. Update pom.xml (Desktop)
echo "Updating pom.xml (Desktop)..."
if ! update_file \
    "$PROJECT_ROOT/pom.xml" \
    "<version>[0-9]*\.[0-9]*\(\.[0-9]*\)*</version>" \
    "<version>${NEW_VERSION}</version>" \
    "xml"; then
    FAILED=$((FAILED + 1))
fi
echo ""

# 2. Update jnexus-core/build.gradle
echo "Updating jnexus-core/build.gradle (Shared Core)..."
if ! update_file \
    "$PROJECT_ROOT/jnexus-core/build.gradle" \
    "version = '[0-9]*\.[0-9]*\(\.[0-9]*\)*'" \
    "version = '${NEW_VERSION}'" \
    "gradle"; then
    FAILED=$((FAILED + 1))
fi
echo ""

# 3. Update jnexus-android/build.gradle
echo "Updating jnexus-android/build.gradle (Android)..."
if ! update_file \
    "$PROJECT_ROOT/jnexus-android/build.gradle" \
    "versionName \"[0-9]*\.[0-9]*\(\.[0-9]*\)*\"" \
    "versionName \"${NEW_VERSION}\"" \
    "gradle"; then
    FAILED=$((FAILED + 1))
fi
echo ""

# 4. Update jnexus-ios/iOS/Info.plist
echo "Updating jnexus-ios/iOS/Info.plist (iOS)..."
if [ -f "$PROJECT_ROOT/jnexus-ios/iOS/Info.plist" ]; then
    if ! update_file \
        "$PROJECT_ROOT/jnexus-ios/iOS/Info.plist" \
        "<string>[0-9]*\.[0-9]*\(\.[0-9]*\)*</string>" \
        "<string>${NEW_VERSION}</string>" \
        "plist"; then
        FAILED=$((FAILED + 1))
    fi
else
    echo -e "${YELLOW}⚠ Skipping (not found): jnexus-ios/iOS/Info.plist${NC}"
fi
echo ""

# 5. Update jnexus-ios/macOS/Info.plist
echo "Updating jnexus-ios/macOS/Info.plist (macOS)..."
if [ -f "$PROJECT_ROOT/jnexus-ios/macOS/Info.plist" ]; then
    if ! update_file \
        "$PROJECT_ROOT/jnexus-ios/macOS/Info.plist" \
        "<string>[0-9]*\.[0-9]*\(\.[0-9]*\)*</string>" \
        "<string>${NEW_VERSION}</string>" \
        "plist"; then
        FAILED=$((FAILED + 1))
    fi
else
    echo -e "${YELLOW}⚠ Skipping (not found): jnexus-ios/macOS/Info.plist${NC}"
fi
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}All version files updated successfully!${NC}"
    echo ""
    echo "Next steps:"
    echo "1. Update CHANGELOG.md with release notes for v${NEW_VERSION}"
    echo "2. Verify changes: git diff"
    echo "3. Commit: git add . && git commit -m \"chore: bump version to ${NEW_VERSION}\""
    echo "4. Create tag: git tag v${NEW_VERSION}"
    echo "5. Push: git push origin main && git push origin v${NEW_VERSION}"
    echo ""
    echo "The unified release workflow will automatically build all platforms!"
else
    echo -e "${RED}$FAILED version files failed to update${NC}"
    echo ""
    echo "Manual rollback:"
    for file in \
        "$PROJECT_ROOT/pom.xml" \
        "$PROJECT_ROOT/jnexus-core/build.gradle" \
        "$PROJECT_ROOT/jnexus-android/build.gradle" \
        "$PROJECT_ROOT/jnexus-ios/iOS/Info.plist" \
        "$PROJECT_ROOT/jnexus-ios/macOS/Info.plist"; do
        if [ -f "$file.bak" ]; then
            echo "  Restore: mv $file.bak $file"
        fi
    done
    exit 1
fi
