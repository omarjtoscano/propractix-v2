package com.propractix.organization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Jurisdiction;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Notice;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Purpose;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Reference;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Scope;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Status;
import com.propractix.organization.application.command.SubmitCompanyOnboardingCommand;
import com.propractix.organization.application.contract.event.v1.CompanyOnboardingEventV1;
import com.propractix.organization.application.contract.event.v1.CompanyOnboardingEventV1.Type;
import com.propractix.organization.application.port.in.SubmitCompanyOnboarding;
import com.propractix.organization.application.port.in.SubmitCompanyOnboarding.Failure;
import com.propractix.organization.application.port.out.CompanyOnboardingPolicyPort.Snapshot;
import com.propractix.organization.application.port.out.CompanyOnboardingRepository;
import com.propractix.organization.application.port.out.EmailDomainRoutingVerificationPort;
import com.propractix.organization.application.port.out.OnboardingAudit;
import com.propractix.organization.application.port.out.OnboardingDependencyUnavailable;
import com.propractix.organization.application.port.out.OnboardingIds;
import com.propractix.organization.application.port.out.OnboardingUnitOfWork;
import com.propractix.organization.application.port.out.TaxIdentityProtectionPort.Fingerprint;
import com.propractix.organization.application.port.out.TaxIdentityProtectionPort.ProtectedTaxIdentity;
import com.propractix.organization.application.service.SubmitCompanyOnboardingService;
import com.propractix.organization.domain.model.CompanyName;
import com.propractix.organization.domain.model.CompanyOnboarding;
import com.propractix.organization.domain.model.CompanyProfile;
import com.propractix.organization.domain.model.CompanyTimeZone;
import com.propractix.organization.domain.model.CountryCode;
import com.propractix.organization.domain.model.EmailAddress;
import com.propractix.organization.domain.model.EmailDomain;
import com.propractix.organization.domain.model.EmailDomainRoutingResult;
import com.propractix.organization.domain.model.LocaleCode;
import com.propractix.organization.domain.model.OnboardingId;
import com.propractix.organization.domain.model.PolicyReference;
import com.propractix.organization.domain.model.TaxIdentifier;
import com.propractix.organization.domain.model.TenantId;
import com.propractix.organization.domain.service.CompanyEmailAdmissionPolicy;
import com.propractix.organization.domain.service.CompanyRegistrationPolicy;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class SubmitCompanyOnboardingServiceTest {
    static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
    static final CountryCode ES = new CountryCode("ES");
    static final CountryCode FR = new CountryCode("FR");
    static final LocaleCode SPANISH = new LocaleCode("es");
    static final LocaleCode ENGLISH = new LocaleCode("en");
    static final Jurisdiction JURISDICTION = new Jurisdiction("SYNTHETIC-ONBOARDING-JURISDICTION");
    static final Reference REFERENCE = new Reference(UUID.fromString("00000000-0000-4000-8000-000000000001"), "test-v1");
    static final ProtectedTaxIdentity PROTECTED = new ProtectedTaxIdentity("synthetic-encryption-v1", "test-ciphertext",
            List.of(new Fingerprint("synthetic-hmac-v2", "test-index-v2"),
                    new Fingerprint("synthetic-hmac-v1", "test-index-v1")));

    @Test
    void storesAuthoritativeEvidenceAndPendingIntentWithoutProvisioningOrTenant() {
        Fixture fixture = new Fixture();
        Reference clientReference = new Reference(REFERENCE.noticeId(), REFERENCE.version());
        var command = command(profile(ES, Optional.of(ENGLISH)), clientReference);

        var result = fixture.service().submit(command);

        assertThat(result).isInstanceOf(SubmitCompanyOnboarding.Received.class);
        var stored = fixture.onlySubmission();
        assertThat(stored.operationId()).isEqualTo(((SubmitCompanyOnboarding.Received) result).operationId());
        assertThat(stored.operationId()).isNotEqualTo(stored.onboarding().id().value());
        assertThat(stored.onboarding().status()).isEqualTo(CompanyOnboarding.Status.IDENTITY_PENDING);
        assertThat(stored.onboarding().tenantId()).isEmpty();
        assertThat(stored.onboarding().profile()).isEqualTo(command.profile());
        assertThat(stored.onboarding().initialLocale()).isEqualTo(ENGLISH);
        assertThat(stored.onboarding().expiresAt()).isEqualTo(NOW.plus(Duration.ofDays(7)));
        assertThat(stored.onboarding().administratorEmail().value()).isEqualTo("Admin.Name+test@company.example");
        assertThat(stored.administratorName()).isEqualTo("Synthetic Administrator");
        assertThat(stored.protectedTaxIdentity()).isEqualTo(PROTECTED);
        assertThat(stored.privacyEvidence().resolvedNotice()).isSameAs(fixture.notice.orElseThrow());
        assertThat(stored.privacyEvidence().resolvedNotice().reference()).isNotSameAs(clientReference);
        assertThat(stored.privacyEvidence().recordedAt()).isEqualTo(NOW);
        assertThat(fixture.calls).containsExactly("policies", "privacy", "protect", "match", "routing", "transaction",
                "repository", "outbox", "outbox", "audit");
        assertThat(fixture.events).extracting(CompanyOnboardingEventV1::type)
                .containsExactly(Type.COMPANY_ONBOARDING_SUBMITTED, Type.ADMINISTRATOR_PROVISIONING_REQUESTED);
        assertThat(fixture.audits).singleElement().satisfies(entry -> {
            assertThat(entry.resourceId()).isEqualTo(stored.onboarding().id());
            assertThat(entry.correlationId()).isEqualTo(command.correlationId());
            assertThat(entry.actor()).isEqualTo("ANONYMOUS_SUBMISSION");
        });
    }

    static Stream<Notice> invalidNotices() {
        return Stream.of(
                notice(Purpose.COMPANY_ONBOARDING, JURISDICTION, Status.TEST_ONLY, Scope.LOCAL_TEST,
                        NOW.minusSeconds(2), NOW),
                notice(Purpose.COMPANY_ONBOARDING, JURISDICTION, Status.TEST_ONLY, Scope.LOCAL_TEST,
                        NOW.plusSeconds(1), NOW.plusSeconds(2)),
                notice(Purpose.STUDENT_ONBOARDING, JURISDICTION, Status.TEST_ONLY, Scope.LOCAL_TEST,
                        NOW.minusSeconds(1), NOW.plusSeconds(2)),
                notice(Purpose.COMPANY_ONBOARDING, new Jurisdiction("SYNTHETIC-OTHER"), Status.TEST_ONLY,
                        Scope.LOCAL_TEST, NOW.minusSeconds(1), NOW.plusSeconds(2)),
                notice(Purpose.COMPANY_ONBOARDING, JURISDICTION, Status.SUPERSEDED, Scope.LOCAL_TEST,
                        NOW.minusSeconds(1), NOW.plusSeconds(2)),
                notice(Purpose.COMPANY_ONBOARDING, JURISDICTION, Status.SUSPENDED, Scope.OPERATIONAL,
                        NOW.minusSeconds(1), NOW.plusSeconds(2)),
                notice(Purpose.COMPANY_ONBOARDING, JURISDICTION, Status.TEST_ONLY, Scope.OPERATIONAL,
                        NOW.minusSeconds(1), NOW.plusSeconds(2)));
    }

    @ParameterizedTest
    @MethodSource("invalidNotices")
    void invalidResolutionStopsBeforeProtectionWritesOrEvents(Notice invalid) {
        Fixture fixture = new Fixture();
        fixture.notice = Optional.of(invalid);
        assertThat(fixture.service().submit(command())).isEqualTo(
                new SubmitCompanyOnboarding.Rejected(Failure.PRIVACY_POLICY_UNAVAILABLE));
        fixture.assertNoEffects();
        assertThat(fixture.calls).containsExactly("policies", "privacy");
    }

    @Test
    void absentNoticeStopsBeforeAnyPersonalDataFingerprintOrWrite() {
        Fixture fixture = new Fixture();
        fixture.notice = Optional.empty();
        assertThat(fixture.service().submit(command())).isEqualTo(
                new SubmitCompanyOnboarding.Rejected(Failure.PRIVACY_POLICY_UNAVAILABLE));
        fixture.assertNoEffects();
        assertThat(fixture.calls).containsExactly("policies", "privacy");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void tamperedIdOrStaleVersionCannotBecomeEvidence(boolean tamperId) {
        Fixture fixture = new Fixture();
        var reference = new Reference(tamperId ? UUID.randomUUID() : REFERENCE.noticeId(),
                tamperId ? REFERENCE.version() : "superseded-test-v0");
        assertThat(fixture.service().submit(command(profile(ES, Optional.empty()), reference))).isEqualTo(
                new SubmitCompanyOnboarding.Rejected(Failure.PRIVACY_NOTICE_CHANGED));
        fixture.assertNoEffects();
        assertThat(fixture.calls).containsExactly("policies", "privacy");
    }

    @Test
    void replacementBetweenPresentationAndSubmitIsNotAutomaticallyAccepted() {
        Fixture fixture = new Fixture();
        fixture.notice = Optional.of(new Notice(new Reference(REFERENCE.noticeId(), "test-v2"),
                Purpose.COMPANY_ONBOARDING, JURISDICTION, Status.TEST_ONLY, Scope.LOCAL_TEST,
                NOW.minusSeconds(1), NOW.plusSeconds(60)));
        assertThat(fixture.service().submit(command())).isEqualTo(
                new SubmitCompanyOnboarding.Rejected(Failure.PRIVACY_NOTICE_CHANGED));
        fixture.assertNoEffects();
    }

    @Test
    void operationalScopeRejectsSyntheticNotice() {
        Fixture fixture = new Fixture();
        fixture.scope = Scope.OPERATIONAL;
        assertThat(fixture.service().submit(command())).isEqualTo(
                new SubmitCompanyOnboarding.Rejected(Failure.PRIVACY_POLICY_UNAVAILABLE));
        fixture.assertNoEffects();
    }

    @Test
    void approvedMetadataIsAllowedAtValidFromWithoutPublishingAnything() {
        Fixture fixture = new Fixture();
        fixture.scope = Scope.OPERATIONAL;
        fixture.notice = Optional.of(notice(Purpose.COMPANY_ONBOARDING, JURISDICTION, Status.APPROVED,
                Scope.OPERATIONAL, NOW, NOW.plusSeconds(60)));
        assertThat(fixture.service().submit(command())).isInstanceOf(SubmitCompanyOnboarding.Received.class);
    }

    @Test
    void absentAdmissionPolicyFailsClosed() {
        Fixture fixture = new Fixture();
        fixture.policy = Optional.empty();
        assertThat(fixture.service().submit(command())).isEqualTo(
                new SubmitCompanyOnboarding.Rejected(Failure.COMPANY_EMAIL_POLICY_UNAVAILABLE));
        fixture.assertNoEffects();
        assertThat(fixture.calls).containsExactly("policies");
    }

    @Test
    void blockedDomainIsRejectedAfterPrivacyButBeforeProtectionAndRouting() {
        Fixture fixture = new Fixture();
        var original = command();
        var blocked = new SubmitCompanyOnboardingCommand(original.profile(), original.administratorName(),
                new EmailAddress("synthetic@blocked.example"), REFERENCE, original.correlationId());
        assertThat(fixture.service().submit(blocked)).isEqualTo(
                new SubmitCompanyOnboarding.Rejected(Failure.COMPANY_EMAIL_DOMAIN_NOT_ALLOWED));
        fixture.assertNoEffects();
        assertThat(fixture.calls).containsExactly("policies", "privacy");
    }

    @ParameterizedTest
    @EnumSource(EmailDomainRoutingResult.class)
    void everyTechnicalRouteRemainsPendingAndNonCapableRoutesScheduleRecheck(EmailDomainRoutingResult route) {
        Fixture fixture = new Fixture();
        fixture.route = route;
        assertThat(fixture.service().submit(command())).isInstanceOf(SubmitCompanyOnboarding.Received.class);
        var stored = fixture.onlySubmission().onboarding();
        assertThat(stored.status()).isEqualTo(CompanyOnboarding.Status.IDENTITY_PENDING);
        assertThat(stored.tenantId()).isEmpty();
        assertThat(stored.routingResult()).contains(route);
        var rechecks = fixture.events.stream().filter(e -> e.type() == Type.EMAIL_DOMAIN_ROUTING_RECHECK_REQUESTED).toList();
        if (route == EmailDomainRoutingResult.MAIL_CAPABLE) {
            assertThat(rechecks).isEmpty();
        } else {
            assertThat(rechecks).singleElement().satisfies(event ->
                    assertThat(event.availableAt()).isEqualTo(NOW.plusSeconds(60)));
        }
    }

    @Test
    void knownResolverFailureBecomesIndeterminateWithRecheck() {
        Fixture fixture = new Fixture();
        fixture.failAt = "routing";
        assertThat(fixture.service().submit(command())).isInstanceOf(SubmitCompanyOnboarding.Received.class);
        assertThat(fixture.onlySubmission().onboarding().routingResult()).contains(EmailDomainRoutingResult.INDETERMINATE);
        assertThat(fixture.events).extracting(CompanyOnboardingEventV1::type)
                .contains(Type.EMAIL_DOMAIN_ROUTING_RECHECK_REQUESTED);
    }

    @Test
    void recheckIsNotScheduledBeyondOnboardingExpiry() {
        Fixture fixture = new Fixture();
        var policy = fixture.policy.orElseThrow();
        fixture.policy = Optional.of(new Snapshot(policy.admission(), policy.registration(), JURISDICTION,
                Duration.ofSeconds(30), Duration.ofMinutes(1)));
        fixture.route = EmailDomainRoutingResult.NO_MAIL_ROUTE;
        fixture.service().submit(command());
        assertThat(fixture.events).extracting(CompanyOnboardingEventV1::type)
                .doesNotContain(Type.EMAIL_DOMAIN_ROUTING_RECHECK_REQUESTED);
        assertThat(fixture.onlySubmission().onboarding().status()).isEqualTo(CompanyOnboarding.Status.IDENTITY_PENDING);
    }

    @Test
    void fiscalMatchHasTheSamePublicShapeAndNeverClaimsExistingCompany() {
        Fixture fresh = new Fixture();
        Fixture matching = new Fixture();
        matching.companyMatch = true;
        assertThat(fresh.service().submit(command())).isInstanceOf(SubmitCompanyOnboarding.Received.class);
        assertThat(matching.service().submit(command())).isInstanceOf(SubmitCompanyOnboarding.Received.class);
        assertThat(matching.onlySubmission().continuation())
                .isEqualTo(CompanyOnboardingRepository.Continuation.COMPANY_REVIEW_REQUIRED);
        assertThat(matching.onlySubmission().onboarding().tenantId()).isEmpty();
        assertThat(matching.events).extracting(CompanyOnboardingEventV1::type)
                .containsExactly(Type.COMPANY_ONBOARDING_SUBMITTED);
    }

    @Test
    void twoSubmissionsWithTheSameAccountAddressDoNotLinkOrReuseAnAccountOrTenant() {
        Fixture fixture = new Fixture();
        var service = fixture.service();
        var first = (SubmitCompanyOnboarding.Received) service.submit(command());
        var second = (SubmitCompanyOnboarding.Received) service.submit(command());
        assertThat(first.operationId()).isNotEqualTo(second.operationId());
        assertThat(fixture.submissions).hasSize(2).allSatisfy(submission -> {
            assertThat(submission.onboarding().tenantId()).isEmpty();
            assertThat(submission.onboarding().status()).isEqualTo(CompanyOnboarding.Status.IDENTITY_PENDING);
        });
        assertThat(fixture.events.stream().filter(e -> e.type() == Type.ADMINISTRATOR_PROVISIONING_REQUESTED))
                .hasSize(2);
        // No identity port is a dependency of submit: matching is deferred, never inferred here.
    }

    @Test
    void concurrentCandidatesRemainSeparatePendingIntentsWithoutCreatingOrLinkingTenants() throws Exception {
        Fixture fixture = new Fixture();
        CountDownLatch bothMatching = new CountDownLatch(2);
        fixture.beforeMatch = () -> {
            bothMatching.countDown();
            try {
                if (!bothMatching.await(5, TimeUnit.SECONDS)) {
                    throw new AssertionError("synthetic concurrent candidates did not meet");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new AssertionError(interrupted);
            }
        };
        var service = fixture.service();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> service.submit(command()));
            var second = executor.submit(() -> service.submit(command()));
            assertThat(first.get(10, TimeUnit.SECONDS)).isInstanceOf(SubmitCompanyOnboarding.Received.class);
            assertThat(second.get(10, TimeUnit.SECONDS)).isInstanceOf(SubmitCompanyOnboarding.Received.class);
        }
        assertThat(fixture.submissions).hasSize(2).allSatisfy(submission -> {
            assertThat(submission.onboarding().tenantId()).isEmpty();
            assertThat(submission.onboarding().status()).isEqualTo(CompanyOnboarding.Status.IDENTITY_PENDING);
        });
        assertThat(fixture.submissions).extracting(s -> s.onboarding().id()).doesNotHaveDuplicates();
        assertThat(fixture.submissions).extracting(s -> s.operationId()).doesNotHaveDuplicates();
        assertThat(fixture.events.stream().filter(e -> e.type() == Type.ADMINISTRATOR_PROVISIONING_REQUESTED)).hasSize(2);
        // Fiscal/account uniqueness under concurrent provisioning belongs to E3/E5, not this double.
    }

    @ParameterizedTest
    @ValueSource(strings = {"policies", "privacy", "protect", "match"})
    void recoverablePreparationFailureHasNoEffectsAndCanBeRetried(String stage) {
        Fixture fixture = new Fixture();
        fixture.failAt = stage;
        assertThat(fixture.service().submit(command())).isEqualTo(new SubmitCompanyOnboarding.TemporarilyUnavailable());
        fixture.assertNoEffects();
        fixture.failAt = "";
        assertThat(fixture.service().submit(command())).isInstanceOf(SubmitCompanyOnboarding.Received.class);
        assertThat(fixture.submissions).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"repository", "outbox", "audit"})
    void knownWriteFailureRollsBackOnboardingEventsAndAuditTogether(String stage) {
        Fixture fixture = new Fixture();
        fixture.failAt = stage;
        assertThat(fixture.service().submit(command())).isEqualTo(new SubmitCompanyOnboarding.TemporarilyUnavailable());
        fixture.assertNoEffects();
        fixture.failAt = "";
        assertThat(fixture.service().submit(command())).isInstanceOf(SubmitCompanyOnboarding.Received.class);
        assertThat(fixture.submissions).hasSize(1);
        assertThat(fixture.events).hasSize(2);
        assertThat(fixture.audits).hasSize(1);
    }

    @Test
    void unknownCommitDoesNotAdvertiseSafeRetryOrExecuteAgain() {
        Fixture fixture = new Fixture();
        fixture.commitOutcome = OnboardingUnitOfWork.CommitOutcome.UNKNOWN;
        var result = (SubmitCompanyOnboarding.Received) fixture.service().submit(command());
        assertThat(fixture.onlySubmission().operationId()).isEqualTo(result.operationId());
        assertThat(fixture.calls.stream().filter("transaction"::equals)).hasSize(1);
    }

    @Test
    void programmingFailureRollsBackAndPropagatesInsteadOfBeingCalledRecoverable() {
        Fixture fixture = new Fixture();
        fixture.programmingFailure = true;
        assertThatThrownBy(() -> fixture.service().submit(command())).isInstanceOf(IllegalStateException.class);
        fixture.assertNoEffects();
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "es", "de"})
    void preservesAdmittedProfileAndResolvesLocaleWithoutJurisdictionFallback(String preferred) {
        Fixture fixture = new Fixture();
        var profile = profile(FR, Optional.of(new LocaleCode(preferred)));
        assertThat(fixture.service().submit(command(profile, REFERENCE)))
                .isInstanceOf(SubmitCompanyOnboarding.Received.class);
        var stored = fixture.onlySubmission().onboarding();
        assertThat(stored.profile()).isEqualTo(profile);
        assertThat(stored.initialLocale().value()).isEqualTo(preferred.equals("de") ? "en" : preferred);
        assertThat(fixture.lastPrivacyQuery.jurisdiction()).isEqualTo(JURISDICTION);
    }

    @Test
    void disabledCountryDoesNotWriteOrDeriveFiscalFingerprints() {
        Fixture fixture = new Fixture();
        assertThat(fixture.service().submit(command(profile(new CountryCode("PT"), Optional.empty()), REFERENCE)))
                .isEqualTo(new SubmitCompanyOnboarding.Rejected(Failure.VALIDATION_ERROR));
        fixture.assertNoEffects();
        assertThat(fixture.calls).containsExactly("policies", "privacy");
    }

    @Test
    void durableEventsAndAuditContainOnlyAllowlistedIdsAndMetadata() {
        Fixture fixture = new Fixture();
        fixture.route = EmailDomainRoutingResult.NO_MAIL_ROUTE;
        var command = command();
        fixture.service().submit(command);
        var stored = fixture.onlySubmission();
        assertThat(fixture.events).hasSize(3).allSatisfy(event -> {
            assertThat(event.eventVersion()).isEqualTo(1);
            assertThat(event.producerContext()).isEqualTo("organization");
            assertThat(event.aggregateType()).isEqualTo("CompanyOnboarding");
            assertThat(event.onboardingId()).isEqualTo(stored.onboarding().id());
            assertThat(event.aggregateVersion()).isEqualTo(stored.onboarding().version());
            assertThat(event.occurredAt()).isEqualTo(NOW);
            assertThat(event.correlationId()).isEqualTo(command.correlationId());
        });
        assertThat(fixture.events).extracting(CompanyOnboardingEventV1::eventId).doesNotHaveDuplicates();
        assertThat(Arrays.stream(CompanyOnboardingEventV1.class.getRecordComponents()).map(c -> c.getName()))
                .containsExactly("eventId", "type", "onboardingId", "aggregateVersion", "occurredAt", "availableAt", "correlationId");
        assertThat(Arrays.stream(OnboardingAudit.Entry.class.getRecordComponents()).map(c -> c.getName()))
                .containsExactly("resourceId", "at", "correlationId");
        assertThat(fixture.events.toString() + fixture.audits + command + stored + PROTECTED)
                .doesNotContain("Synthetic Administrator", "company.example", "SYNTHETIC-FISCAL-001",
                        "test-ciphertext", "test-index-v1");
    }

    static SubmitCompanyOnboardingCommand command() {
        return command(profile(ES, Optional.empty()), REFERENCE);
    }

    static SubmitCompanyOnboardingCommand command(CompanyProfile profile, Reference reference) {
        return new SubmitCompanyOnboardingCommand(profile, "Synthetic Administrator",
                new EmailAddress("Admin.Name+test@company.example"), reference, UUID.randomUUID());
    }

    static CompanyProfile profile(CountryCode country, Optional<LocaleCode> locale) {
        return new CompanyProfile(new CompanyName("Synthetic Company"), Optional.of(new CompanyName("Synthetic Trade")),
                country, new TaxIdentifier("TEST", "SYNTHETIC-FISCAL-001"), locale, new CompanyTimeZone("Europe/Madrid"));
    }

    static Notice notice(Purpose purpose, Jurisdiction jurisdiction, Status status, Scope scope,
            Instant validFrom, Instant validUntil) {
        return new Notice(REFERENCE, purpose, jurisdiction, status, scope, validFrom, validUntil);
    }

    /** All implementations are in tests, in memory, with synthetic values and no cryptographic claim. */
    static final class Fixture implements CompanyOnboardingRepository, OnboardingUnitOfWork, OnboardingIds {
        Optional<Notice> notice = Optional.of(notice(Purpose.COMPANY_ONBOARDING, JURISDICTION, Status.TEST_ONLY,
                Scope.LOCAL_TEST, NOW.minusSeconds(60), NOW.plusSeconds(600)));
        Optional<Snapshot> policy = Optional.of(new Snapshot(
                new CompanyEmailAdmissionPolicy(new PolicyReference("synthetic-admission", "test-v1"),
                        Set.of(new EmailDomain("blocked.example"))),
                new CompanyRegistrationPolicy(Set.of(ES, FR), Set.of(SPANISH, ENGLISH),
                        Map.of(ES, SPANISH, FR, ENGLISH), SPANISH),
                JURISDICTION, Duration.ofDays(7), Duration.ofMinutes(1)));
        Scope scope = Scope.LOCAL_TEST;
        EmailDomainRoutingResult route = EmailDomainRoutingResult.MAIL_CAPABLE;
        ResolveApplicablePrivacyNotice.Query lastPrivacyQuery;
        boolean companyMatch;
        Runnable beforeMatch = () -> { };
        String failAt = "";
        boolean programmingFailure;
        CommitOutcome commitOutcome = CommitOutcome.COMMITTED;
        List<String> calls = Collections.synchronizedList(new ArrayList<>());
        List<PendingSubmission> submissions = new ArrayList<>();
        List<CompanyOnboardingEventV1> events = new ArrayList<>();
        List<OnboardingAudit.Entry> audits = new ArrayList<>();

        SubmitCompanyOnboardingService service() {
            EmailDomainRoutingVerificationPort routing = domain -> {
                call("routing");
                assertThat(domain).isEqualTo(new EmailDomain("company.example"));
                return route;
            };
            return new SubmitCompanyOnboardingService(query -> {
                call("privacy");
                lastPrivacyQuery = query;
                assertThat(query.at()).isEqualTo(NOW);
                assertThat(query.purpose()).isEqualTo(Purpose.COMPANY_ONBOARDING);
                return notice;
            }, (country, at) -> { call("policies"); return policy; },
                    (country, tax) -> { call("protect"); return PROTECTED; },
                    (country, type, fingerprints) -> {
                        call("match");
                        beforeMatch.run();
                        assertThat(fingerprints).containsExactlyElementsOf(PROTECTED.fingerprints());
                        return companyMatch;
                    }, routing, this,
                    event -> { call("outbox"); events.add(event); },
                    entry -> { call("audit"); audits.add(entry); }, this, () -> NOW, this, scope);
        }

        void call(String stage) {
            calls.add(stage);
            if (programmingFailure && stage.equals("outbox")) {
                throw new IllegalStateException("synthetic programmer fault");
            }
            if (stage.equals(failAt)) {
                throw new OnboardingDependencyUnavailable();
            }
        }

        PendingSubmission onlySubmission() {
            assertThat(submissions).hasSize(1);
            return submissions.getFirst();
        }

        void assertNoEffects() {
            assertThat(submissions).isEmpty();
            assertThat(events).isEmpty();
            assertThat(audits).isEmpty();
        }

        @Override
        public synchronized CommitOutcome execute(Runnable writes) {
            call("transaction");
            List<PendingSubmission> previousSubmissions = new ArrayList<>(submissions);
            List<CompanyOnboardingEventV1> previousEvents = new ArrayList<>(events);
            List<OnboardingAudit.Entry> previousAudits = new ArrayList<>(audits);
            try {
                writes.run();
                if (commitOutcome == CommitOutcome.ROLLED_BACK) {
                    restore(previousSubmissions, previousEvents, previousAudits);
                }
                return commitOutcome;
            } catch (RuntimeException failure) {
                restore(previousSubmissions, previousEvents, previousAudits);
                if (failure instanceof OnboardingDependencyUnavailable) {
                    return CommitOutcome.ROLLED_BACK;
                }
                throw failure;
            }
        }

        private void restore(List<PendingSubmission> previousSubmissions,
                List<CompanyOnboardingEventV1> previousEvents, List<OnboardingAudit.Entry> previousAudits) {
            submissions = previousSubmissions;
            events = previousEvents;
            audits = previousAudits;
        }

        @Override
        public void insert(PendingSubmission submission) {
            call("repository");
            submissions.add(submission);
        }

        @Override
        public Optional<PendingSubmission> findForProcess(OnboardingId id) {
            return submissions.stream().filter(s -> s.onboarding().id().equals(id)).findFirst();
        }

        @Override
        public Optional<PendingSubmission> findForTenant(TenantId tenantId, OnboardingId id) {
            return findForProcess(id).filter(s -> s.belongsTo(tenantId));
        }

        @Override
        public UUID nextOperationId() { return UUID.randomUUID(); }
        @Override
        public OnboardingId nextOnboardingId() { return new OnboardingId(UUID.randomUUID()); }
        @Override
        public TenantId nextTenantId() { throw new AssertionError("submit must not allocate tenants"); }
        @Override
        public UUID nextEventId() { return UUID.randomUUID(); }
    }
}
