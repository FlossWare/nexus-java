#!/bin/bash
# Generate code quality summary report

echo "========================================"
echo "JNexus Code Quality Report"
echo "========================================"
echo ""

# Run quality checks
echo "Running code quality analysis..."
mvn checkstyle:checkstyle pmd:pmd pmd:cpd spotbugs:spotbugs -q

echo ""
echo "========================================"
echo "CHECKSTYLE SUMMARY"
echo "========================================"
if [ -f target/checkstyle-result.xml ]; then
    violations=$(grep -c '<error' target/checkstyle-result.xml 2>/dev/null || echo "0")
    echo "Total violations: $violations"
    echo ""
    echo "Top violations by type:"
    grep '<error' target/checkstyle-result.xml | sed 's/.*source="\([^"]*\)".*/\1/' | sort | uniq -c | sort -rn | head -10
else
    echo "No checkstyle report found"
fi

echo ""
echo "========================================"
echo "PMD SUMMARY"
echo "========================================"
if [ -f target/pmd.xml ]; then
    violations=$(grep -c '<violation' target/pmd.xml 2>/dev/null || echo "0")
    echo "Total violations: $violations"
    echo ""
    echo "Top violations by rule:"
    grep 'rule=' target/pmd.xml | sed 's/.*rule="\([^"]*\)".*/\1/' | sort | uniq -c | sort -rn | head -10
else
    echo "No PMD report found"
fi

echo ""
echo "========================================"
echo "CPD (Copy-Paste Detection) SUMMARY"
echo "========================================"
if [ -f target/cpd.xml ]; then
    duplications=$(grep -c '<duplication' target/cpd.xml 2>/dev/null || echo "0")
    echo "Duplicated code blocks: $duplications"
    if [ "$duplications" -gt 0 ]; then
        echo ""
        grep '<duplication' target/cpd.xml | head -5
    fi
else
    echo "No CPD report found"
fi

echo ""
echo "========================================"
echo "SPOTBUGS SUMMARY"
echo "========================================"
if [ -f target/spotbugsXml.xml ]; then
    bugs=$(grep -c '<BugInstance' target/spotbugsXml.xml 2>/dev/null || echo "0")
    echo "Total bugs found: $bugs"
    if [ "$bugs" -gt 0 ]; then
        echo ""
        echo "Bugs by category:"
        grep 'category=' target/spotbugsXml.xml | sed 's/.*category="\([^"]*\)".*/\1/' | sort | uniq -c | sort -rn
    fi
else
    echo "No SpotBugs report found"
fi

echo ""
echo "========================================"
echo "REFACTORING TARGETS"
echo "========================================"
echo "Classes with highest complexity (from Checkstyle):"
grep 'CyclomaticComplexity\|NPathComplexity\|JavaNCSS' target/checkstyle-result.xml 2>/dev/null | \
    sed 's/.*source="\([^"]*\)".*/\1/' | sort | uniq -c | sort -rn | head -10 || echo "No data"

echo ""
echo "========================================"
echo "Full reports available at:"
echo "  - target/checkstyle-result.xml"
echo "  - target/pmd.xml"
echo "  - target/cpd.xml"
echo "  - target/spotbugsXml.xml"
echo "  - target/site/ (HTML reports)"
echo "========================================"
