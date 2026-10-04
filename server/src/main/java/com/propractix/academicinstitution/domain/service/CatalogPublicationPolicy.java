package com.propractix.academicinstitution.domain.service;

import com.propractix.academicinstitution.domain.event.InstitutionCatalogPublished;
import com.propractix.academicinstitution.domain.model.CatalogArtifactHash;
import com.propractix.academicinstitution.domain.model.CatalogImport;
import java.util.Objects;
import java.util.Optional;

public final class CatalogPublicationPolicy {

    public PublicationDecision evaluate(
            Optional<CatalogImport> currentlyPublishedImport,
            CatalogArtifactHash requestedArtifactHash) {
        Objects.requireNonNull(currentlyPublishedImport, "currentlyPublishedImport must not be null");
        Objects.requireNonNull(requestedArtifactHash, "requestedArtifactHash must not be null");

        if (currentlyPublishedImport.isEmpty()) {
            return PublicationDecision.PUBLISH_NEW_SNAPSHOT;
        }

        CatalogImport activeImport = currentlyPublishedImport.orElseThrow();
        if (activeImport.status() != CatalogImport.Status.PUBLISHED) {
            throw new IllegalArgumentException("currentlyPublishedImport must have PUBLISHED status");
        }

        if (activeImport.metadata().artifactHash().equals(requestedArtifactHash)) {
            return PublicationDecision.ACTIVE_ARTIFACT_UNCHANGED;
        }
        return PublicationDecision.PUBLISH_NEW_SNAPSHOT;
    }

    public Optional<InstitutionCatalogPublished> eventFor(
            PublicationDecision decision,
            Optional<CatalogImport> resultingImport) {
        Objects.requireNonNull(decision, "decision must not be null");
        Objects.requireNonNull(resultingImport, "resultingImport must not be null");

        if (!decision.emitsEvent()) {
            if (resultingImport.isPresent()) {
                throw new IllegalArgumentException("an unchanged artifact must not create a catalog import");
            }
            return Optional.empty();
        }
        CatalogImport publishedImport = resultingImport.orElseThrow(
                () -> new IllegalArgumentException("a changed snapshot requires a catalog import"));
        if (publishedImport.status() != CatalogImport.Status.PUBLISHED) {
            throw new IllegalArgumentException("an event requires a newly published catalog import");
        }

        return Optional.of(new InstitutionCatalogPublished(
                publishedImport.id(),
                publishedImport.previousPublishedImportId(),
                publishedImport.metadata().artifactHash(),
                publishedImport.publishedAt().orElseThrow()));
    }

    public enum PublicationDecision {
        ACTIVE_ARTIFACT_UNCHANGED(false, false, false),
        PUBLISH_NEW_SNAPSHOT(true, true, true);

        private final boolean createsImport;
        private final boolean changesSnapshot;
        private final boolean emitsEvent;

        PublicationDecision(boolean createsImport, boolean changesSnapshot, boolean emitsEvent) {
            this.createsImport = createsImport;
            this.changesSnapshot = changesSnapshot;
            this.emitsEvent = emitsEvent;
        }

        public boolean createsImport() {
            return createsImport;
        }

        public boolean changesSnapshot() {
            return changesSnapshot;
        }

        public boolean emitsEvent() {
            return emitsEvent;
        }
    }
}
