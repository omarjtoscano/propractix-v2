package com.propractix.organization.domain.model;

import static com.propractix.organization.domain.model.SyntheticCompany.*;
import static org.junit.jupiter.api.Assertions.*;

import com.propractix.organization.domain.service.CompanyEmailAdmissionPolicy;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CompanyOnboardingTest {
    @Test
    void startsSubmittedWithFixedOwnerRoleAndNoTenantOrRouteInference() {
        var onboarding = submitted();
        assertEquals(CompanyOnboarding.Status.SUBMITTED, onboarding.status());
        assertEquals(CompanyOnboarding.InitialRole.COMPANY_OWNER, onboarding.initialRole());
        assertEquals(ONBOARDING, onboarding.id());
        assertEquals(profile(), onboarding.profile());
        assertEquals("Persona+qa@empresa.test", onboarding.administratorEmail().value());
        assertEquals(SPANISH, onboarding.initialLocale());
        assertEquals(admission().reference(), onboarding.admissionPolicy());
        assertEquals(Optional.empty(), onboarding.tenantId());
        assertEquals(Optional.empty(), onboarding.routingResult());
        assertEquals(AT, onboarding.submittedAt());
        assertEquals(AT.plus(VALIDITY), onboarding.expiresAt());
        assertEquals(0, onboarding.version());
    }

    @Test
    void movesOnlyThroughTheAuthorizedPendingStates() {
        var onboarding = submitted();
        assertThrows(IllegalStateException.class, () -> onboarding.awaitEmailVerification(company(), AT));
        assertEquals(0, onboarding.version());
        onboarding.awaitIdentity(AT);
        assertEquals(CompanyOnboarding.Status.IDENTITY_PENDING, onboarding.status());
        assertThrows(IllegalStateException.class, () -> onboarding.awaitIdentity(AT));
        onboarding.awaitEmailVerification(company(), AT.plusSeconds(1));
        assertEquals(CompanyOnboarding.Status.EMAIL_PENDING, onboarding.status());
        assertEquals(Optional.of(TENANT), onboarding.tenantId());
        assertEquals(2, onboarding.version());
        assertThrows(IllegalStateException.class, () -> onboarding.awaitIdentity(AT.plusSeconds(2)));
        assertThrows(IllegalStateException.class, () -> onboarding.awaitEmailVerification(company(), AT.plusSeconds(2)));
        assertEquals(AT.plusSeconds(1), onboarding.lastChangedAt());
        assertEquals(Company.Status.SELF_DECLARED, company().status());
    }

    @Test
    void cannotSubstituteAnotherCompanyOrRelinkAnAlreadyBoundOnboarding() {
        var onboarding = submitted();
        onboarding.awaitIdentity(AT);
        var otherProfile = new CompanyProfile(new CompanyName("Otra empresa sintética"), Optional.empty(), DE,
                new TaxIdentifier("SYNTHETIC", "SYNTHETIC-E1-0002"), Optional.empty(), new CompanyTimeZone("Europe/Berlin"));
        var otherId = new TenantId(UUID.fromString("123e4567-e89b-42d3-a456-426614174002"));
        var otherCompany = Company.selfDeclared(otherId, otherProfile, registration());
        assertThrows(IllegalArgumentException.class, () -> onboarding.awaitEmailVerification(otherCompany, AT));
        assertEquals(Optional.empty(), onboarding.tenantId());
        assertEquals(CompanyOnboarding.Status.IDENTITY_PENDING, onboarding.status());
        onboarding.awaitEmailVerification(company(), AT);
        assertThrows(IllegalStateException.class, () -> onboarding.awaitEmailVerification(otherCompany, AT));
        assertEquals(Optional.of(TENANT), onboarding.tenantId());
        assertEquals(Company.Status.SELF_DECLARED, otherCompany.status());
        // This is aggregate consistency, not proof of application/SQL tenant authorization (E2/E3).
    }

    @ParameterizedTest
    @EnumSource(EmailDomainRoutingResult.class)
    void routeResultsNeverRejectOrActivateTheOnboarding(EmailDomainRoutingResult result) {
        var onboarding = submitted();
        onboarding.recordRoutingResult(result, AT);
        assertEquals(CompanyOnboarding.Status.SUBMITTED, onboarding.status());
        onboarding.awaitIdentity(AT);
        onboarding.awaitEmailVerification(company(), AT);
        assertEquals(CompanyOnboarding.Status.EMAIL_PENDING, onboarding.status());
        assertEquals(Optional.of(result), onboarding.routingResult());
        assertEquals(Company.Status.SELF_DECLARED, company().status());
        assertEquals(3, onboarding.version());
    }

    @Test
    void recordsRecoverableRouteChangesWithoutTurningThemIntoBusinessVerification() {
        var onboarding = submitted();
        onboarding.awaitIdentity(AT);
        onboarding.awaitEmailVerification(company(), AT);
        onboarding.recordRoutingResult(EmailDomainRoutingResult.INDETERMINATE, AT.plusSeconds(1));
        onboarding.recordRoutingResult(EmailDomainRoutingResult.NO_MAIL_ROUTE, AT.plusSeconds(2));
        onboarding.recordRoutingResult(EmailDomainRoutingResult.MAIL_CAPABLE, AT.plusSeconds(3));
        assertEquals(Optional.of(EmailDomainRoutingResult.MAIL_CAPABLE), onboarding.routingResult());
        assertEquals(CompanyOnboarding.Status.EMAIL_PENDING, onboarding.status());
        assertEquals(Company.Status.SELF_DECLARED, company().status());
        assertEquals(AT.plusSeconds(3), onboarding.lastChangedAt());
    }

    @Test
    void substitutedPolicyAffectsNewSubmissionsAndPreservesPreviouslyEvaluatedVersion() {
        var old = submitted();
        var replacement = new CompanyEmailAdmissionPolicy(new PolicyReference("synthetic-admission", "local-test-2"),
                Set.of(new EmailDomain("empresa.test")));
        assertThrows(IllegalArgumentException.class, () -> CompanyOnboarding.submit(ONBOARDING, profile(),
                old.administratorEmail(), replacement, registration(), AT, VALIDITY));
        var allowed = CompanyOnboarding.submit(ONBOARDING, profile(), new EmailAddress("persona@bloqueado.test"),
                replacement, registration(), AT, VALIDITY);
        assertEquals(admission().reference(), old.admissionPolicy());
        assertEquals(replacement.reference(), allowed.admissionPolicy());
        assertEquals(CompanyOnboarding.Status.SUBMITTED, old.status());
    }

    @Test
    void requiresAdmissionAndCatalogBeforeConstructingAnOnboarding() {
        assertThrows(NullPointerException.class, () -> CompanyOnboarding.submit(ONBOARDING, profile(),
                new EmailAddress("a@empresa.test"), null, registration(), AT, VALIDITY));
        assertThrows(NullPointerException.class, () -> CompanyOnboarding.submit(ONBOARDING, profile(),
                new EmailAddress("a@empresa.test"), admission(), null, AT, VALIDITY));
        var error = assertThrows(IllegalArgumentException.class, () -> CompanyOnboarding.submit(ONBOARDING, profile(),
                new EmailAddress("a@bloqueado.test"), admission(), registration(), AT, VALIDITY));
        assertEquals("company_email_domain_not_allowed", error.getMessage());
    }

    @Test
    void usesExplicitClockAndSuppliedDurationInsteadOfSystemTimeOrSevenDayConstant() {
        Clock fixed = Clock.fixed(AT, ZoneOffset.UTC);
        var shortLived = CompanyOnboarding.submit(ONBOARDING, profile(), new EmailAddress("a@empresa.test"),
                admission(), registration(), fixed.instant(), Duration.ofNanos(1));
        assertEquals(AT.plusNanos(1), shortLived.expiresAt());
        shortLived.awaitIdentity(fixed.instant());
        shortLived.expire(Clock.offset(fixed, Duration.ofNanos(1)).instant());
        assertEquals(CompanyOnboarding.Status.EXPIRED, shortLived.status());
    }

    @Test
    void acceptsTheLastNanosecondAndExpiresExactlyAtTheSevenDayBoundary() {
        var onboarding = submitted();
        Instant boundary = AT.plus(Duration.parse("P7D"));
        assertThrows(IllegalStateException.class, () -> onboarding.expire(boundary.minusNanos(1)));
        onboarding.awaitIdentity(boundary.minusNanos(1));
        onboarding.awaitEmailVerification(company(), boundary.minusNanos(1));
        onboarding.recordRoutingResult(EmailDomainRoutingResult.MAIL_CAPABLE, boundary.minusNanos(1));
        assertThrows(IllegalStateException.class,
                () -> onboarding.recordRoutingResult(EmailDomainRoutingResult.MAIL_CAPABLE, boundary));
        assertThrows(IllegalStateException.class, () -> onboarding.fail(boundary));
        assertEquals(CompanyOnboarding.Status.EMAIL_PENDING, onboarding.status());
        assertEquals(boundary.minusNanos(1), onboarding.lastChangedAt());
        onboarding.expire(boundary);
        assertEquals(CompanyOnboarding.Status.EXPIRED, onboarding.status());
        assertThrows(IllegalStateException.class, () -> onboarding.awaitEmailVerification(company(), boundary));
        assertThrows(IllegalStateException.class, () -> onboarding.expire(boundary.plusSeconds(1)));
        assertEquals(Optional.of(TENANT), onboarding.tenantId());
    }

    @Test
    void alsoExpiresStalledSubmittedAndIdentityPendingStates() {
        var submitted = submitted();
        submitted.expire(AT.plus(VALIDITY).plusSeconds(1));
        assertEquals(CompanyOnboarding.Status.EXPIRED, submitted.status());
        var identityPending = submitted();
        identityPending.awaitIdentity(AT);
        assertThrows(IllegalStateException.class,
                () -> identityPending.awaitEmailVerification(company(), AT.plus(VALIDITY)));
        identityPending.expire(AT.plus(VALIDITY));
        assertEquals(CompanyOnboarding.Status.EXPIRED, identityPending.status());
    }

    @Test
    void terminalFailureCannotResumeOrBecomeExpired() {
        for (int stage = 0; stage < 3; stage++) {
            var onboarding = submitted();
            if (stage >= 1) onboarding.awaitIdentity(AT);
            if (stage >= 2) onboarding.awaitEmailVerification(company(), AT);
            onboarding.fail(AT.plusSeconds(1));
            assertEquals(CompanyOnboarding.Status.FAILED, onboarding.status());
            long version = onboarding.version();
            assertThrows(IllegalStateException.class, () -> onboarding.awaitIdentity(AT.plusSeconds(2)));
            assertThrows(IllegalStateException.class, () -> onboarding.awaitEmailVerification(company(), AT.plusSeconds(2)));
            assertThrows(IllegalStateException.class,
                    () -> onboarding.recordRoutingResult(EmailDomainRoutingResult.MAIL_CAPABLE, AT.plusSeconds(2)));
            assertThrows(IllegalStateException.class, () -> onboarding.fail(AT.plusSeconds(2)));
            assertThrows(IllegalStateException.class, () -> onboarding.expire(AT.plus(VALIDITY)));
            assertEquals(version, onboarding.version());
        }
    }

    @Test
    void timeCannotGoBackwardsAndRejectedChangesLeaveNoPartialMutation() {
        var onboarding = submitted();
        assertThrows(IllegalArgumentException.class, () -> onboarding.awaitIdentity(AT.minusNanos(1)));
        onboarding.awaitIdentity(AT.plusSeconds(1));
        assertThrows(IllegalArgumentException.class, () -> onboarding.awaitEmailVerification(company(), AT));
        assertThrows(NullPointerException.class, () -> onboarding.recordRoutingResult(null, AT.plusSeconds(2)));
        assertThrows(NullPointerException.class, () -> onboarding.awaitEmailVerification(null, AT.plusSeconds(2)));
        assertThrows(NullPointerException.class, () -> onboarding.expire(null));
        assertThrows(NullPointerException.class, () -> onboarding.fail(null));
        assertEquals(CompanyOnboarding.Status.IDENTITY_PENDING, onboarding.status());
        assertEquals(AT.plusSeconds(1), onboarding.lastChangedAt());
        assertEquals(1, onboarding.version());
        assertEquals(Optional.empty(), onboarding.tenantId());
    }

    @Test
    void rejectsZeroNegativeMissingOrUnrepresentableExpiration() {
        var email = new EmailAddress("a@empresa.test");
        assertThrows(IllegalArgumentException.class, () -> CompanyOnboarding.submit(ONBOARDING, profile(), email,
                admission(), registration(), AT, Duration.ZERO));
        assertThrows(IllegalArgumentException.class, () -> CompanyOnboarding.submit(ONBOARDING, profile(), email,
                admission(), registration(), AT, Duration.ofNanos(-1)));
        assertThrows(NullPointerException.class, () -> CompanyOnboarding.submit(ONBOARDING, profile(), email,
                admission(), registration(), AT, null));
        assertThrows(NullPointerException.class, () -> CompanyOnboarding.submit(ONBOARDING, profile(), email,
                admission(), registration(), null, VALIDITY));
        assertThrows(DateTimeException.class, () -> CompanyOnboarding.submit(ONBOARDING, profile(), email,
                admission(), registration(), Instant.MAX, VALIDITY));
    }
}
