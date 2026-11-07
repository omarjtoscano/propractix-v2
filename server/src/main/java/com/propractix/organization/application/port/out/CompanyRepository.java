package com.propractix.organization.application.port.out;

import com.propractix.organization.domain.model.Company;
import com.propractix.organization.domain.model.TenantId;
import java.util.Objects;
import java.util.Optional;

/** Internal, trusted tenant scope only. No unscoped get or caller-supplied body authority. */
public interface CompanyRepository {
    Optional<ScopedCompany> find(TenantId tenantId);
    void insert(ScopedCompany company);

    /** Rejects a cross-tenant write at the application boundary before reaching an adapter. */
    record ScopedCompany(TenantId tenantId, Company company,
            TaxIdentityProtectionPort.ProtectedTaxIdentity protectedTaxIdentity) {
        public ScopedCompany {
            Objects.requireNonNull(tenantId, "tenantId must not be null");
            Objects.requireNonNull(company, "company must not be null");
            Objects.requireNonNull(protectedTaxIdentity, "protectedTaxIdentity must not be null");
            if (!tenantId.equals(company.id())) {
                throw new IllegalArgumentException("company scope mismatch");
            }
        }

        @Override
        public String toString() { return "ScopedCompany[redacted]"; }
    }
}
