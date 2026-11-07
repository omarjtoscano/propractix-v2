package com.propractix.organization.application;

import static com.propractix.organization.application.SubmitCompanyOnboardingServiceTest.PROTECTED;
import static com.propractix.organization.application.SubmitCompanyOnboardingServiceTest.command;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propractix.organization.application.port.out.CompanyRepository;
import com.propractix.organization.domain.model.Company;
import com.propractix.organization.domain.model.TenantId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Port-level isolation with doubles; no claim about HTTP authentication or SQL predicates. */
class TenantScopedOnboardingPortsTest {
    @Test
    void companyScopeRejectsForeignWriteBeforeAdapterAndOtherTenantCannotRead() {
        var tenantA = new TenantId(UUID.randomUUID());
        var tenantB = new TenantId(UUID.randomUUID());
        var fixture = new SubmitCompanyOnboardingServiceTest.Fixture();
        fixture.service().submit(command());
        var onboarding = fixture.onlySubmission().onboarding();
        var company = Company.selfDeclared(tenantA, onboarding.profile(), onboarding.initialLocale());
        var repository = new InMemoryCompanies();
        var scoped = new CompanyRepository.ScopedCompany(tenantA, company, PROTECTED);
        repository.insert(scoped);

        assertThat(repository.find(tenantA)).contains(scoped);
        assertThat(repository.find(tenantB)).isEmpty();
        assertThatThrownBy(() -> repository.insert(new CompanyRepository.ScopedCompany(tenantB, company, PROTECTED)))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("company scope mismatch");
        assertThat(repository.companies).hasSize(1);
        assertThat(repository.find(tenantA)).contains(scoped);
        assertThat(repository.find(tenantB)).isEmpty();
    }

    @Test
    void preTenantOnboardingIsNotExposedThroughAnyTenantLookup() {
        var fixture = new SubmitCompanyOnboardingServiceTest.Fixture();
        fixture.service().submit(command());
        var stored = fixture.onlySubmission();
        assertThat(fixture.findForProcess(stored.onboarding().id())).contains(stored);
        assertThat(fixture.findForTenant(new TenantId(UUID.randomUUID()), stored.onboarding().id())).isEmpty();
    }

    @Test
    void processRecordOnceAssociatedIsOnlyVisibleToItsOwnTenant() {
        var fixture = new SubmitCompanyOnboardingServiceTest.Fixture();
        fixture.service().submit(command());
        var stored = fixture.onlySubmission();
        var onboarding = stored.onboarding();
        var tenantA = new TenantId(UUID.randomUUID());
        // Test setup only: the future E5 handler, not submit, may establish this eligibility.
        onboarding.awaitEmailVerification(Company.selfDeclared(tenantA, onboarding.profile(), onboarding.initialLocale()),
                SubmitCompanyOnboardingServiceTest.NOW);
        assertThat(fixture.findForTenant(tenantA, onboarding.id())).contains(stored);
        assertThat(fixture.findForTenant(new TenantId(UUID.randomUUID()), onboarding.id())).isEmpty();
    }

    private static final class InMemoryCompanies implements CompanyRepository {
        final Map<TenantId, ScopedCompany> companies = new HashMap<>();

        @Override
        public Optional<ScopedCompany> find(TenantId tenantId) {
            return Optional.ofNullable(companies.get(tenantId));
        }

        @Override
        public void insert(ScopedCompany company) {
            if (companies.putIfAbsent(company.tenantId(), company) != null) {
                throw new IllegalStateException("synthetic duplicate tenant");
            }
        }
    }
}
