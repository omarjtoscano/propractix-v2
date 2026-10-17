package com.propractix.academicinstitution.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutionsResult;
import com.propractix.academicinstitution.application.port.out.PublishedCatalogSnapshotMetadata;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionQueryRepository;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionSearchRow;
import com.propractix.academicinstitution.application.query.AcademicInstitutionSearchItem;
import com.propractix.academicinstitution.application.query.AcademicInstitutionSearchPage;
import com.propractix.academicinstitution.application.query.InstitutionSearchCursor;
import com.propractix.academicinstitution.application.query.NormalizedInstitutionSearchTerm;
import com.propractix.academicinstitution.application.query.SearchPublishedInstitutionsQuery;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.model.CountryCode;
import com.propractix.academicinstitution.domain.model.OfficialInstitutionName;
import java.net.URI;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchPublishedInstitutionsServiceTest {

    private static final CatalogImportId SNAPSHOT_ID =
            CatalogImportId.from("223e4567-e89b-42d3-a456-426614174000");
    private static final Instant RETRIEVED_AT = Instant.parse("2026-10-16T09:15:00Z");
    private static final URI SOURCE_URL = URI.create("https://example.test/catalog.csv");
    private static final String SOURCE_ATTRIBUTION = "Catalog attribution";
    private static final PublishedCatalogSnapshotMetadata SNAPSHOT =
            new PublishedCatalogSnapshotMetadata(
                    SNAPSHOT_ID, RETRIEVED_AT, SOURCE_URL, SOURCE_ATTRIBUTION);

    private StubQueryRepository repository;
    private SearchPublishedInstitutionsService service;

    @BeforeEach
    void setUp() {
        repository = new StubQueryRepository();
        service = new SearchPublishedInstitutionsService(repository);
    }

    @Test
    void sendsEmptyAndNormalizedQueriesToPortWithoutLoadingAWholeCatalog() {
        repository.outcome = new PublishedInstitutionQueryRepository.Page(SNAPSHOT, List.of(row(1, "Alfa")));

        service.search(new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 1));

        assertEquals(Optional.empty(), repository.lastRequest.term());
        assertEquals(2, repository.lastRequest.fetchLimit());

        NormalizedInstitutionSearchTerm term = NormalizedInstitutionSearchTerm.from("Málaga");
        service.search(new SearchPublishedInstitutionsQuery(Optional.of(term), Optional.empty(), 50));

        assertEquals(Optional.of(term), repository.lastRequest.term());
        assertEquals("malaga", repository.lastRequest.term().orElseThrow().value());
        assertEquals(51, repository.lastRequest.fetchLimit());
        assertEquals(2, repository.calls);
    }

    @Test
    void usesLimitPlusOneAndBuildsCursorFromLastVisibleOrderedRow() {
        repository.outcome = new PublishedInstitutionQueryRepository.Page(
                SNAPSHOT,
                List.of(
                        row(1, "Universidad Alfa"),
                        row(2, "Universidad Beta"),
                        row(3, "Universidad Gamma")));
        SearchPublishedInstitutionsQuery query =
                new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 2);

        SearchPublishedInstitutionsResult.Found found = assertInstanceOf(
                SearchPublishedInstitutionsResult.Found.class, service.search(query));
        AcademicInstitutionSearchPage page = found.page();

        assertEquals(3, repository.lastRequest.fetchLimit());
        assertEquals(List.of("Universidad Alfa", "Universidad Beta"), page.items().stream()
                .map(item -> item.officialName().value())
                .toList());
        InstitutionSearchCursor cursor = page.nextCursor().orElseThrow();
        assertEquals(SNAPSHOT_ID, cursor.snapshotId());
        assertEquals(query.queryFingerprint(), cursor.queryFingerprint());
        assertEquals("universidad beta", cursor.lastSearchNameNormalized());
        assertEquals(page.items().getLast().id(), cursor.lastInstitutionId());
        assertEquals(RETRIEVED_AT, page.retrievedAt());
        assertEquals(SOURCE_URL, page.sourceUrl());
        assertEquals(SOURCE_ATTRIBUTION, page.sourceAttribution());
        assertThrows(UnsupportedOperationException.class, () -> page.items().clear());
    }

    @Test
    void preservesRepositoryOrderAndDoesNotCreateCursorWithoutExtraRow() {
        repository.outcome = new PublishedInstitutionQueryRepository.Page(
                SNAPSHOT,
                List.of(row(3, "Gamma"), row(1, "Alfa"), row(2, "Beta")));

        SearchPublishedInstitutionsResult.Found found = assertInstanceOf(
                SearchPublishedInstitutionsResult.Found.class,
                service.search(new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 3)));

        assertEquals(List.of("Gamma", "Alfa", "Beta"), found.page().items().stream()
                .map(item -> item.officialName().value())
                .toList());
        assertTrue(found.page().nextCursor().isEmpty());
    }

    @Test
    void passesTypedCursorToPortAndMapsSnapshotChange() {
        SearchPublishedInstitutionsQuery firstQuery =
                new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 20);
        InstitutionSearchCursor cursor = InstitutionSearchCursor.after(
                SNAPSHOT_ID,
                firstQuery.queryFingerprint(),
                "universidad alfa",
                id(1));
        repository.outcome = new PublishedInstitutionQueryRepository.SnapshotChanged();

        SearchPublishedInstitutionsResult result = service.search(
                new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.of(cursor), 20));

        assertInstanceOf(SearchPublishedInstitutionsResult.SnapshotChanged.class, result);
        assertEquals(Optional.of(cursor), repository.lastRequest.cursor());
    }

    @Test
    void mapsUnavailableCatalogFailuresToTypedApplicationFailures() {
        repository.outcome = new PublishedInstitutionQueryRepository.CatalogUnavailable(
                PublishedInstitutionQueryRepository.Failure.NO_PUBLISHED_CATALOG);

        SearchPublishedInstitutionsResult.CatalogUnavailable absent = assertInstanceOf(
                SearchPublishedInstitutionsResult.CatalogUnavailable.class,
                service.search(new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 20)));
        assertEquals(SearchPublishedInstitutionsResult.Failure.NO_PUBLISHED_CATALOG, absent.failure());

        repository.outcome = new PublishedInstitutionQueryRepository.CatalogUnavailable(
                PublishedInstitutionQueryRepository.Failure.READ_FAILED);
        SearchPublishedInstitutionsResult.CatalogUnavailable failed = assertInstanceOf(
                SearchPublishedInstitutionsResult.CatalogUnavailable.class,
                service.search(new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 20)));
        assertEquals(SearchPublishedInstitutionsResult.Failure.READ_FAILED, failed.failure());
    }

    @Test
    void publicProjectionContainsNoExternalReferenceOrSourceStatus() {
        assertEquals(
                List.of("id", "officialName", "countryCode"),
                Arrays.stream(AcademicInstitutionSearchItem.class.getRecordComponents())
                        .map(component -> component.getName())
                        .toList());
        List<String> pageComponents = Arrays.stream(AcademicInstitutionSearchPage.class.getRecordComponents())
                .map(component -> component.getName())
                .toList();
        assertEquals(
                List.of("items", "nextCursor", "retrievedAt", "sourceUrl", "sourceAttribution"),
                pageComponents);
        assertFalse(pageComponents.contains("sourceStatus"));
        assertFalse(pageComponents.contains("sourceRecordId"));
        assertFalse(pageComponents.contains("sourceSystem"));
    }

    private static PublishedInstitutionSearchRow row(int suffix, String name) {
        return new PublishedInstitutionSearchRow(
                id(suffix),
                new OfficialInstitutionName(name),
                new CountryCode("ES"),
                name.toLowerCase(java.util.Locale.ROOT));
    }

    private static AcademicInstitutionId id(int suffix) {
        return AcademicInstitutionId.from("123e4567-e89b-42d3-a456-42661417400" + suffix);
    }

    private static final class StubQueryRepository implements PublishedInstitutionQueryRepository {

        private int calls;
        private SearchRequest lastRequest;
        private SearchOutcome outcome = new CatalogUnavailable(Failure.NO_PUBLISHED_CATALOG);

        @Override
        public SearchOutcome search(SearchRequest request) {
            calls++;
            lastRequest = request;
            return outcome;
        }
    }
}
