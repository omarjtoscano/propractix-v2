package com.propractix.organization.application.service;

import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Notice;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Purpose;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Query;
import com.propractix.compliance.application.port.in.ResolveApplicablePrivacyNotice.Scope;
import com.propractix.organization.application.command.SubmitCompanyOnboardingCommand;
import com.propractix.organization.application.contract.event.v1.CompanyOnboardingEventV1;
import com.propractix.organization.application.contract.event.v1.CompanyOnboardingEventV1.Type;
import com.propractix.organization.application.port.in.SubmitCompanyOnboarding;
import com.propractix.organization.application.port.out.CompanyIdentityLookup;
import com.propractix.organization.application.port.out.CompanyOnboardingPolicyPort;
import com.propractix.organization.application.port.out.CompanyOnboardingPolicyPort.Snapshot;
import com.propractix.organization.application.port.out.CompanyOnboardingRepository;
import com.propractix.organization.application.port.out.CompanyOnboardingRepository.Continuation;
import com.propractix.organization.application.port.out.CompanyOnboardingRepository.PendingSubmission;
import com.propractix.organization.application.port.out.CompanyOnboardingRepository.PrivacyEvidence;
import com.propractix.organization.application.port.out.EmailDomainRoutingVerificationPort;
import com.propractix.organization.application.port.out.OnboardingAudit;
import com.propractix.organization.application.port.out.OnboardingClock;
import com.propractix.organization.application.port.out.OnboardingDependencyUnavailable;
import com.propractix.organization.application.port.out.OnboardingIds;
import com.propractix.organization.application.port.out.OnboardingOutbox;
import com.propractix.organization.application.port.out.OnboardingUnitOfWork;
import com.propractix.organization.application.port.out.TaxIdentityProtectionPort;
import com.propractix.organization.domain.model.CompanyOnboarding;
import com.propractix.organization.domain.model.EmailDomainRoutingResult;
import com.propractix.organization.domain.service.CompanyEmailAdmissionPolicy.Decision;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** No framework, transport, identity call, tenant creation or external effect delivery. */
public final class SubmitCompanyOnboardingService implements SubmitCompanyOnboarding {
    private final ResolveApplicablePrivacyNotice privacy;
    private final CompanyOnboardingPolicyPort policies;
    private final TaxIdentityProtectionPort taxProtection;
    private final CompanyIdentityLookup companyIdentity;
    private final EmailDomainRoutingVerificationPort routing;
    private final CompanyOnboardingRepository repository;
    private final OnboardingOutbox outbox;
    private final OnboardingAudit audit;
    private final OnboardingUnitOfWork unitOfWork;
    private final OnboardingClock clock;
    private final OnboardingIds ids;
    private final Scope scope;

    public SubmitCompanyOnboardingService(ResolveApplicablePrivacyNotice privacy,
            CompanyOnboardingPolicyPort policies, TaxIdentityProtectionPort taxProtection,
            CompanyIdentityLookup companyIdentity, EmailDomainRoutingVerificationPort routing,
            CompanyOnboardingRepository repository, OnboardingOutbox outbox, OnboardingAudit audit,
            OnboardingUnitOfWork unitOfWork, OnboardingClock clock, OnboardingIds ids, Scope scope) {
        this.privacy = Objects.requireNonNull(privacy);
        this.policies = Objects.requireNonNull(policies);
        this.taxProtection = Objects.requireNonNull(taxProtection);
        this.companyIdentity = Objects.requireNonNull(companyIdentity);
        this.routing = Objects.requireNonNull(routing);
        this.repository = Objects.requireNonNull(repository);
        this.outbox = Objects.requireNonNull(outbox);
        this.audit = Objects.requireNonNull(audit);
        this.unitOfWork = Objects.requireNonNull(unitOfWork);
        this.clock = Objects.requireNonNull(clock);
        this.ids = Objects.requireNonNull(ids);
        this.scope = Objects.requireNonNull(scope);
    }

    @Override
    public Result submit(SubmitCompanyOnboardingCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        // Read/protection failures are safe to retry only before entering the write boundary.
        Prepared prepared;
        try {
            prepared = prepare(command, clock.now());
        } catch (OnboardingDependencyUnavailable unavailable) {
            return new TemporarilyUnavailable();
        }
        if (prepared.rejection() != null) {
            return prepared.rejection();
        }
        PendingSubmission submission = prepared.submission();
        OnboardingUnitOfWork.CommitOutcome outcome = unitOfWork.execute(() -> {
            repository.insert(submission);
            prepared.events().forEach(outbox::append);
            audit.append(new OnboardingAudit.Entry(submission.onboarding().id(),
                    submission.onboarding().submittedAt(), command.correlationId()));
        });
        Objects.requireNonNull(outcome, "commit outcome must not be null");
        // UNKNOWN is not a license to submit again. The local boundary owns reconciliation.
        return outcome == OnboardingUnitOfWork.CommitOutcome.ROLLED_BACK
                ? new TemporarilyUnavailable() : new Received(submission.operationId());
    }

    private Prepared prepare(SubmitCompanyOnboardingCommand command, Instant at) {
        var selected = policies.current(command.profile().registeredCountry(), at);
        if (selected.isEmpty()) {
            return Prepared.reject(Failure.COMPANY_EMAIL_POLICY_UNAVAILABLE);
        }
        Snapshot policy = selected.orElseThrow();
        Query query = new Query(Purpose.COMPANY_ONBOARDING, policy.privacyJurisdiction(), at);
        var resolved = privacy.resolve(query);
        if (resolved.isEmpty() || !resolved.orElseThrow().appliesTo(query, scope)) {
            return Prepared.reject(Failure.PRIVACY_POLICY_UNAVAILABLE);
        }
        Notice notice = resolved.orElseThrow();
        if (!notice.reference().equals(command.privacyNoticeReference())) {
            return Prepared.reject(Failure.PRIVACY_NOTICE_CHANGED);
        }
        if (policy.admission().evaluate(command.administratorEmail()) != Decision.NOT_LISTED_AS_PUBLIC_PROVIDER) {
            return Prepared.reject(Failure.COMPANY_EMAIL_DOMAIN_NOT_ALLOWED);
        }
        // Validate E1 catalogs before allocating IDs or deriving any protected body fingerprint.
        try {
            policy.registration().initialLocale(command.profile());
        } catch (IllegalArgumentException invalidProfile) {
            return Prepared.reject(Failure.VALIDATION_ERROR);
        }
        var tax = taxProtection.protect(command.profile().registeredCountry(), command.profile().taxIdentifier());
        boolean companyMatch = companyIdentity.exists(command.profile().registeredCountry(),
                command.profile().taxIdentifier().type(), tax.fingerprints());
        CompanyOnboarding onboarding = CompanyOnboarding.submit(ids.nextOnboardingId(), command.profile(),
                command.administratorEmail(), policy.admission(), policy.registration(), at, policy.validity());
        onboarding.awaitIdentity(at);
        EmailDomainRoutingResult route;
        try {
            route = Objects.requireNonNull(routing.verify(command.administratorEmail().domain()));
        } catch (OnboardingDependencyUnavailable unavailable) {
            route = EmailDomainRoutingResult.INDETERMINATE;
        }
        onboarding.recordRoutingResult(route, at);
        UUID operationId = ids.nextOperationId();
        PendingSubmission submission = new PendingSubmission(operationId, onboarding, command.administratorName(),
                tax, new PrivacyEvidence(notice, at),
                companyMatch ? Continuation.COMPANY_REVIEW_REQUIRED : Continuation.AWAIT_IDENTITY);
        List<CompanyOnboardingEventV1> events = new ArrayList<>();
        events.add(event(Type.COMPANY_ONBOARDING_SUBMITTED, onboarding, at, at, command.correlationId()));
        if (!companyMatch) {
            events.add(event(Type.ADMINISTRATOR_PROVISIONING_REQUESTED, onboarding, at, at, command.correlationId()));
        }
        Instant recheckAt = at.plus(policy.routingRetryDelay());
        if (route != EmailDomainRoutingResult.MAIL_CAPABLE && recheckAt.isBefore(onboarding.expiresAt())) {
            events.add(event(Type.EMAIL_DOMAIN_ROUTING_RECHECK_REQUESTED, onboarding, at, recheckAt,
                    command.correlationId()));
        }
        return new Prepared(submission, List.copyOf(events), null);
    }

    private CompanyOnboardingEventV1 event(Type type, CompanyOnboarding onboarding,
            Instant at, Instant availableAt, UUID correlationId) {
        return new CompanyOnboardingEventV1(ids.nextEventId(), type, onboarding.id(),
                onboarding.version(), at, availableAt, correlationId);
    }

    private record Prepared(PendingSubmission submission, List<CompanyOnboardingEventV1> events, Rejected rejection) {
        static Prepared reject(Failure failure) {
            return new Prepared(null, List.of(), new Rejected(failure));
        }
    }
}
