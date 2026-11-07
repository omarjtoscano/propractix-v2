package com.propractix.organization.domain.model;

import com.propractix.organization.domain.service.CompanyRegistrationPolicy;
import java.util.Objects;

/** Immutable self-declared company/tenant. Business verification and memberships are outside E1. */
public final class Company {
    public enum Status {
        SELF_DECLARED
    }

    private final TenantId id;
    private final CompanyProfile profile;
    private final LocaleCode initialLocale;

    private Company(TenantId id, CompanyProfile profile, LocaleCode initialLocale) {
        this.id = id;
        this.profile = profile;
        this.initialLocale = initialLocale;
    }

    public static Company selfDeclared(TenantId id, CompanyProfile profile, CompanyRegistrationPolicy policy) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(profile, "profile must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        return new Company(id, profile, policy.initialLocale(profile));
    }

    public TenantId id() {
        return id;
    }

    public CompanyProfile profile() {
        return profile;
    }

    public LocaleCode initialLocale() {
        return initialLocale;
    }

    public Status status() {
        return Status.SELF_DECLARED;
    }

}
