package com.propractix.organization.domain.service;

import com.propractix.organization.domain.model.EmailAddress;
import com.propractix.organization.domain.model.EmailDomain;
import com.propractix.organization.domain.model.PolicyReference;
import java.util.Objects;
import java.util.Set;

/** Pure exact-match admission snapshot. Loading, publication and effective-version selection are external. */
public record CompanyEmailAdmissionPolicy(PolicyReference reference, Set<EmailDomain> blockedDomains) {
    public enum Decision {
        NOT_LISTED_AS_PUBLIC_PROVIDER,
        DOMAIN_NOT_ALLOWED
    }

    public CompanyEmailAdmissionPolicy {
        Objects.requireNonNull(reference, "reference must not be null");
        blockedDomains = Set.copyOf(blockedDomains);
        if (blockedDomains.isEmpty()) {
            throw new IllegalArgumentException("an unavailable policy must not become an empty permissive policy");
        }
    }

    public Decision evaluate(EmailAddress email) {
        Objects.requireNonNull(email, "email must not be null");
        return blockedDomains.contains(email.domain())
                ? Decision.DOMAIN_NOT_ALLOWED : Decision.NOT_LISTED_AS_PUBLIC_PROVIDER;
    }
}
