package com.propractix.organization.application.port.out;

import com.propractix.organization.domain.model.EmailDomain;
import com.propractix.organization.domain.model.EmailDomainRoutingResult;

/** Technical result only. Adapters map resolver failures to INDETERMINATE. */
public interface EmailDomainRoutingVerificationPort {
    EmailDomainRoutingResult verify(EmailDomain domain);
}
