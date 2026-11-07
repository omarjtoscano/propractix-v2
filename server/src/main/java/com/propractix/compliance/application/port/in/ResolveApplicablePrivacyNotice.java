package com.propractix.compliance.application.port.in;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Read-only server resolution. No personal data or client reference enters the query. */
public interface ResolveApplicablePrivacyNotice {
    Optional<Notice> resolve(Query query);

    enum Purpose { COMPANY_ONBOARDING, STUDENT_ONBOARDING }
    enum Status { APPROVED, TEST_ONLY, SUPERSEDED, SUSPENDED }
    enum Scope { OPERATIONAL, LOCAL_TEST }

    record Jurisdiction(String value) {
        public Jurisdiction {
            Objects.requireNonNull(value, "jurisdiction must not be null");
            if (value.isBlank()) {
                throw new IllegalArgumentException("jurisdiction must not be blank");
            }
        }
    }

    /** Public notice identifier; supports the planned local notice without a UUID alias. */
    record Reference(String noticeId, String version) {
        public Reference {
            Objects.requireNonNull(noticeId, "noticeId must not be null");
            Objects.requireNonNull(version, "version must not be null");
            if (noticeId.isBlank() || version.isBlank()) {
                throw new IllegalArgumentException("notice ID and version must not be blank");
            }
        }
    }

    /** SHA-256 of the notice content supplied by compliance, independent of the client reference. */
    record ContentHash(String sha256) {
        public ContentHash {
            Objects.requireNonNull(sha256, "content hash must not be null");
            if (!sha256.matches("[0-9a-f]{64}")) {
                throw new IllegalArgumentException("content hash must be canonical SHA-256");
            }
        }
    }

    record Query(Purpose purpose, Jurisdiction jurisdiction, Instant at) {
        public Query {
            Objects.requireNonNull(purpose, "purpose must not be null");
            Objects.requireNonNull(jurisdiction, "jurisdiction must not be null");
            Objects.requireNonNull(at, "at must not be null");
        }
    }

    record Notice(Reference reference, ContentHash contentHash, Purpose purpose, Jurisdiction jurisdiction,
            Status status, Scope scope, Instant validFrom, Instant validUntil) {
        public Notice {
            Objects.requireNonNull(reference, "reference must not be null");
            Objects.requireNonNull(contentHash, "contentHash must not be null");
            Objects.requireNonNull(purpose, "purpose must not be null");
            Objects.requireNonNull(jurisdiction, "jurisdiction must not be null");
            Objects.requireNonNull(status, "status must not be null");
            Objects.requireNonNull(scope, "scope must not be null");
            Objects.requireNonNull(validFrom, "validFrom must not be null");
            Objects.requireNonNull(validUntil, "validUntil must not be null");
            if (!validFrom.isBefore(validUntil)) {
                throw new IllegalArgumentException("notice validity must be positive");
            }
        }

        public boolean appliesTo(Query query, Scope executionScope) {
            boolean approved = status == Status.APPROVED && scope == Scope.OPERATIONAL;
            boolean synthetic = executionScope == Scope.LOCAL_TEST
                    && status == Status.TEST_ONLY && scope == Scope.LOCAL_TEST;
            return (approved || synthetic) && purpose == query.purpose()
                    && jurisdiction.equals(query.jurisdiction())
                    && !query.at().isBefore(validFrom) && query.at().isBefore(validUntil);
        }
    }
}
