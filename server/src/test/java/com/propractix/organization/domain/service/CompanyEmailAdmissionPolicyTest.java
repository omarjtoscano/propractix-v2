package com.propractix.organization.domain.service;

import static com.propractix.organization.domain.service.CompanyEmailAdmissionPolicy.Decision.*;
import static org.junit.jupiter.api.Assertions.*;

import com.propractix.organization.domain.model.EmailAddress;
import com.propractix.organization.domain.model.EmailDomain;
import com.propractix.organization.domain.model.PolicyReference;
import com.propractix.organization.domain.model.SyntheticCompany;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CompanyEmailAdmissionPolicyTest {
    @Test
    void rejectsOnlyAnExactDomain() {
        assertEquals(DOMAIN_NOT_ALLOWED, SyntheticCompany.admission().evaluate(new EmailAddress("A+qa@bloqueado.test")));
        assertEquals(DOMAIN_NOT_ALLOWED, SyntheticCompany.admission().evaluate(new EmailAddress("a@bloqueado.test")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"no-bloqueado.test", "sub.bloqueado.test", "bloqueado.test.evil.test", "bloqueado-test.test",
            "empresa.test", "centro.edu", "centro.edu.test"})
    void hasNoSubstringSubdomainEducationalSuffixOrHostingRules(String domain) {
        assertEquals(NOT_LISTED_AS_PUBLIC_PROVIDER,
                SyntheticCompany.admission().evaluate(new EmailAddress("persona@" + domain)));
    }

    @Test
    void changingTheSuppliedVersionChangesAdmissionWithoutChangingTheAggregate() {
        var first = SyntheticCompany.admission();
        var second = new CompanyEmailAdmissionPolicy(new PolicyReference("synthetic-admission", "local-test-2"),
                Set.of(new EmailDomain("empresa.test")));
        assertEquals(DOMAIN_NOT_ALLOWED, first.evaluate(new EmailAddress("persona@bloqueado.test")));
        assertEquals(NOT_LISTED_AS_PUBLIC_PROVIDER, second.evaluate(new EmailAddress("persona@bloqueado.test")));
        assertEquals(DOMAIN_NOT_ALLOWED, second.evaluate(new EmailAddress("persona@empresa.test")));
        assertNotEquals(first.reference(), second.reference());
    }

    @Test
    void makesADefensiveCopyOfCanonicalDomains() {
        var input = new HashSet<>(Set.of(new EmailDomain("bloqueado.test")));
        var policy = new CompanyEmailAdmissionPolicy(new PolicyReference("synthetic", "1"), input);
        input.clear();
        assertEquals(DOMAIN_NOT_ALLOWED, policy.evaluate(new EmailAddress("a@bloqueado.test")));
        assertThrows(UnsupportedOperationException.class, () -> policy.blockedDomains().clear());
    }

    @Test
    void comparesCanonicalALabelsExactlyWithoutExecutingIdna() {
        var policy = new CompanyEmailAdmissionPolicy(new PolicyReference("synthetic", "1"),
                Set.of(new EmailDomain("xn--fa-hia.test")));
        assertEquals(DOMAIN_NOT_ALLOWED, policy.evaluate(new EmailAddress("a@xn--fa-hia.test")));
        assertEquals(NOT_LISTED_AS_PUBLIC_PROVIDER, policy.evaluate(new EmailAddress("a@fass.test")));
    }

    @Test
    void rejectsMissingInvalidAndEmptyPoliciesInsteadOfAllowingEverything() {
        assertThrows(NullPointerException.class, () -> new CompanyEmailAdmissionPolicy(null, Set.of()));
        assertThrows(NullPointerException.class, () -> new CompanyEmailAdmissionPolicy(new PolicyReference("s", "1"), null));
        assertThrows(IllegalArgumentException.class,
                () -> new CompanyEmailAdmissionPolicy(new PolicyReference("s", "1"), Set.of()));
        assertThrows(IllegalArgumentException.class, () -> new PolicyReference(" ", "1"));
        assertThrows(IllegalArgumentException.class, () -> new PolicyReference("s", ""));
        assertThrows(NullPointerException.class, () -> SyntheticCompany.admission().evaluate(null));
    }
}
