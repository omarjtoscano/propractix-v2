package com.propractix.academicinstitution.adapter.out.persistence;

import com.propractix.academicinstitution.application.port.out.PublishedCatalogSnapshotMetadata;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionQueryRepository;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionSearchRow;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public final class PostgresPublishedInstitutionQueryAdapter implements PublishedInstitutionQueryRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresPublishedInstitutionQueryAdapter.class);

    private final PublishedInstitutionQueryPersistenceRepository repository;
    private final TransactionTemplate transactionTemplate;

    public PostgresPublishedInstitutionQueryAdapter(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "dataSource must not be null");
        this.repository = new PublishedInstitutionQueryPersistenceRepository(new JdbcTemplate(dataSource));
        this.transactionTemplate = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        this.transactionTemplate.setReadOnly(true);
        this.transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        this.transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public SearchOutcome search(SearchRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        try {
            SearchOutcome outcome = transactionTemplate.execute(status -> searchConsistentSnapshot(request));
            return Objects.requireNonNull(outcome, "search transaction returned no outcome");
        } catch (DataAccessException
                | TransactionException
                | PersistenceMappingException
                | IllegalArgumentException exception) {
            LOGGER.error("Academic institution catalog query failed with category=storage_failure");
            return new CatalogUnavailable(Failure.READ_FAILED);
        }
    }

    private SearchOutcome searchConsistentSnapshot(SearchRequest request) {
        Optional<PublishedCatalogSnapshotMetadata> activeSnapshot = repository.findPublishedSnapshot();
        if (activeSnapshot.isEmpty()) {
            return new CatalogUnavailable(Failure.NO_PUBLISHED_CATALOG);
        }
        PublishedCatalogSnapshotMetadata snapshot = activeSnapshot.orElseThrow();
        if (request.cursor().filter(cursor -> !cursor.snapshotId().equals(snapshot.snapshotId())).isPresent()) {
            return new SnapshotChanged();
        }
        List<PublishedInstitutionSearchRow> rows = repository.search(
                snapshot.snapshotId(), request.term(), request.cursor(), request.fetchLimit());
        return new Page(snapshot, rows);
    }
}
