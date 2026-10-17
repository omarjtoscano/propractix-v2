package com.propractix.academicinstitution.application.service;

import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutions;
import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutionsResult;
import com.propractix.academicinstitution.application.port.out.PublishedCatalogSnapshotMetadata;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionQueryRepository;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionSearchRow;
import com.propractix.academicinstitution.application.query.AcademicInstitutionSearchItem;
import com.propractix.academicinstitution.application.query.AcademicInstitutionSearchPage;
import com.propractix.academicinstitution.application.query.InstitutionSearchCursor;
import com.propractix.academicinstitution.application.query.SearchPublishedInstitutionsQuery;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class SearchPublishedInstitutionsService implements SearchPublishedInstitutions {

    private final PublishedInstitutionQueryRepository queryRepository;

    public SearchPublishedInstitutionsService(PublishedInstitutionQueryRepository queryRepository) {
        this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository must not be null");
    }

    @Override
    public SearchPublishedInstitutionsResult search(SearchPublishedInstitutionsQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        PublishedInstitutionQueryRepository.SearchOutcome outcome = queryRepository.search(
                new PublishedInstitutionQueryRepository.SearchRequest(
                        query.term(), query.cursor(), query.limit() + 1));

        if (outcome instanceof PublishedInstitutionQueryRepository.SnapshotChanged) {
            return new SearchPublishedInstitutionsResult.SnapshotChanged();
        }
        if (outcome instanceof PublishedInstitutionQueryRepository.CatalogUnavailable unavailable) {
            SearchPublishedInstitutionsResult.Failure failure =
                    unavailable.failure() == PublishedInstitutionQueryRepository.Failure.NO_PUBLISHED_CATALOG
                            ? SearchPublishedInstitutionsResult.Failure.NO_PUBLISHED_CATALOG
                            : SearchPublishedInstitutionsResult.Failure.READ_FAILED;
            return new SearchPublishedInstitutionsResult.CatalogUnavailable(failure);
        }

        PublishedInstitutionQueryRepository.Page repositoryPage =
                (PublishedInstitutionQueryRepository.Page) outcome;
        if (repositoryPage.rows().size() > query.limit() + 1) {
            throw new IllegalStateException("query repository returned more rows than requested");
        }
        boolean hasNextPage = repositoryPage.rows().size() > query.limit();
        List<PublishedInstitutionSearchRow> visibleRows = repositoryPage.rows().stream()
                .limit(query.limit())
                .toList();
        Optional<InstitutionSearchCursor> nextCursor = nextCursor(
                hasNextPage, visibleRows, repositoryPage.snapshot(), query.queryFingerprint());
        List<AcademicInstitutionSearchItem> items = visibleRows.stream()
                .map(row -> new AcademicInstitutionSearchItem(
                        row.id(), row.officialName(), row.countryCode()))
                .toList();

        PublishedCatalogSnapshotMetadata snapshot = repositoryPage.snapshot();
        return new SearchPublishedInstitutionsResult.Found(new AcademicInstitutionSearchPage(
                items,
                nextCursor,
                snapshot.retrievedAt(),
                snapshot.sourceUrl(),
                snapshot.sourceAttribution()));
    }

    private static Optional<InstitutionSearchCursor> nextCursor(
            boolean hasNextPage,
            List<PublishedInstitutionSearchRow> visibleRows,
            PublishedCatalogSnapshotMetadata snapshot,
            String queryFingerprint) {
        if (!hasNextPage) {
            return Optional.empty();
        }
        PublishedInstitutionSearchRow lastVisibleRow = visibleRows.getLast();
        return Optional.of(InstitutionSearchCursor.after(
                snapshot.snapshotId(),
                queryFingerprint,
                lastVisibleRow.searchNameNormalized(),
                lastVisibleRow.id()));
    }
}
