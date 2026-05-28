package org.flossware.nexus;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class SearchCriteriaTest {

    @Test
    void testSearchCriteriaCreation() {
        SearchCriteria criteria = new SearchCriteria(
            "maven-releases",
            ".*SNAPSHOT.*",
            1000L,
            5000L,
            Instant.parse("2024-01-01T00:00:00Z"),
            Instant.parse("2024-12-31T23:59:59Z"),
            ".jar",
            "com.example.*"
        );

        assertEquals("maven-releases", criteria.repository());
        assertEquals(".*SNAPSHOT.*", criteria.regexFilter());
        assertEquals(1000L, criteria.minSize());
        assertEquals(5000L, criteria.maxSize());
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), criteria.createdAfter());
        assertEquals(Instant.parse("2024-12-31T23:59:59Z"), criteria.createdBefore());
        assertEquals(".jar", criteria.fileExtension());
        assertEquals("com.example.*", criteria.componentNamePattern());
    }

    @Test
    void testSearchCriteriaWithNullOptionalFields() {
        SearchCriteria criteria = new SearchCriteria(
            "test-repo",
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals("test-repo", criteria.repository());
        assertNull(criteria.regexFilter());
        assertNull(criteria.minSize());
        assertNull(criteria.maxSize());
        assertNull(criteria.createdAfter());
        assertNull(criteria.createdBefore());
        assertNull(criteria.fileExtension());
        assertNull(criteria.componentNamePattern());
    }

    @Test
    void testSearchCriteriaValidationNullRepository() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SearchCriteria(null, null, null, null, null, null, null, null);
        });
    }

    @Test
    void testSearchCriteriaValidationBlankRepository() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SearchCriteria("", null, null, null, null, null, null, null);
        });
    }

    @Test
    void testSearchCriteriaValidationWhitespaceRepository() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SearchCriteria("   ", null, null, null, null, null, null, null);
        });
    }

    @Test
    void testSearchCriteriaValidationMinGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SearchCriteria("test-repo", null, 5000L, 1000L, null, null, null, null);
        });
    }

    @Test
    void testSearchCriteriaValidationMinEqualsMax() {
        SearchCriteria criteria = new SearchCriteria(
            "test-repo",
            null,
            1000L,
            1000L,
            null,
            null,
            null,
            null
        );

        assertEquals(1000L, criteria.minSize());
        assertEquals(1000L, criteria.maxSize());
    }

    @Test
    void testSearchCriteriaValidationCreatedAfterGreaterThanBefore() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SearchCriteria(
                "test-repo",
                null,
                null,
                null,
                Instant.parse("2024-12-31T00:00:00Z"),
                Instant.parse("2024-01-01T00:00:00Z"),
                null,
                null
            );
        });
    }

    @Test
    void testSearchCriteriaValidationCreatedAfterEqualsBefore() {
        Instant sameTime = Instant.parse("2024-06-15T12:00:00Z");
        SearchCriteria criteria = new SearchCriteria(
            "test-repo",
            null,
            null,
            null,
            sameTime,
            sameTime,
            null,
            null
        );

        assertEquals(sameTime, criteria.createdAfter());
        assertEquals(sameTime, criteria.createdBefore());
    }

    @Test
    void testBuilderMinimalConfiguration() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .build();

        assertEquals("test-repo", criteria.repository());
        assertNull(criteria.regexFilter());
        assertNull(criteria.minSize());
        assertNull(criteria.maxSize());
        assertNull(criteria.createdAfter());
        assertNull(criteria.createdBefore());
        assertNull(criteria.fileExtension());
        assertNull(criteria.componentNamePattern());
    }

    @Test
    void testBuilderFullConfiguration() {
        Instant after = Instant.parse("2024-01-01T00:00:00Z");
        Instant before = Instant.parse("2024-12-31T23:59:59Z");

        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("maven-releases")
            .regexFilter(".*SNAPSHOT.*")
            .minSize(1000L)
            .maxSize(5000L)
            .createdAfter(after)
            .createdBefore(before)
            .fileExtension(".jar")
            .componentNamePattern("com.example.*")
            .build();

        assertEquals("maven-releases", criteria.repository());
        assertEquals(".*SNAPSHOT.*", criteria.regexFilter());
        assertEquals(1000L, criteria.minSize());
        assertEquals(5000L, criteria.maxSize());
        assertEquals(after, criteria.createdAfter());
        assertEquals(before, criteria.createdBefore());
        assertEquals(".jar", criteria.fileExtension());
        assertEquals("com.example.*", criteria.componentNamePattern());
    }

    @Test
    void testBuilderPartialConfiguration() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .regexFilter(".*test.*")
            .minSize(100L)
            .fileExtension(".war")
            .build();

        assertEquals("test-repo", criteria.repository());
        assertEquals(".*test.*", criteria.regexFilter());
        assertEquals(100L, criteria.minSize());
        assertNull(criteria.maxSize());
        assertNull(criteria.createdAfter());
        assertNull(criteria.createdBefore());
        assertEquals(".war", criteria.fileExtension());
        assertNull(criteria.componentNamePattern());
    }

    @Test
    void testBuilderValidationMinGreaterThanMax() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SearchCriteria.Builder()
                .repository("test-repo")
                .minSize(5000L)
                .maxSize(1000L)
                .build();
        });
    }

    @Test
    void testBuilderValidationCreatedAfterGreaterThanBefore() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SearchCriteria.Builder()
                .repository("test-repo")
                .createdAfter(Instant.parse("2024-12-31T00:00:00Z"))
                .createdBefore(Instant.parse("2024-01-01T00:00:00Z"))
                .build();
        });
    }

    @Test
    void testHasFiltersWithNoFilters() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .build();
        assertFalse(criteria.hasFilters());
    }

    @Test
    void testHasFiltersWithRegexFilter() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .regexFilter(".*test.*")
            .build();
        assertTrue(criteria.hasFilters());
    }

    @Test
    void testHasFiltersWithMinSize() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .minSize(1000L)
            .build();
        assertTrue(criteria.hasFilters());
    }

    @Test
    void testHasFiltersWithMaxSize() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .maxSize(5000L)
            .build();
        assertTrue(criteria.hasFilters());
    }

    @Test
    void testHasFiltersWithCreatedAfter() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .createdAfter(Instant.parse("2024-01-01T00:00:00Z"))
            .build();
        assertTrue(criteria.hasFilters());
    }

    @Test
    void testHasFiltersWithCreatedBefore() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .createdBefore(Instant.parse("2024-12-31T23:59:59Z"))
            .build();
        assertTrue(criteria.hasFilters());
    }

    @Test
    void testHasFiltersWithFileExtension() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .fileExtension(".jar")
            .build();
        assertTrue(criteria.hasFilters());
    }

    @Test
    void testHasFiltersWithComponentNamePattern() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .componentNamePattern("com.example.*")
            .build();
        assertTrue(criteria.hasFilters());
    }

    @Test
    void testHasFiltersWithMultipleFilters() {
        SearchCriteria criteria = new SearchCriteria.Builder()
            .repository("test-repo")
            .regexFilter(".*test.*")
            .minSize(1000L)
            .fileExtension(".jar")
            .build();
        assertTrue(criteria.hasFilters());
    }

    @Test
    void testSearchCriteriaEquality() {
        SearchCriteria criteria1 = new SearchCriteria.Builder()
            .repository("test-repo")
            .regexFilter(".*test.*")
            .minSize(1000L)
            .build();

        SearchCriteria criteria2 = new SearchCriteria.Builder()
            .repository("test-repo")
            .regexFilter(".*test.*")
            .minSize(1000L)
            .build();

        assertEquals(criteria1, criteria2);
        assertEquals(criteria1.hashCode(), criteria2.hashCode());
    }

    @Test
    void testSearchCriteriaInequality() {
        SearchCriteria criteria1 = new SearchCriteria.Builder()
            .repository("test-repo")
            .regexFilter(".*test.*")
            .build();

        SearchCriteria criteria2 = new SearchCriteria.Builder()
            .repository("test-repo")
            .regexFilter(".*other.*")
            .build();

        assertNotEquals(criteria1, criteria2);
    }
}
