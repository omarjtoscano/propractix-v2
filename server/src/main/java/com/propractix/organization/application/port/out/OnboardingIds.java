package com.propractix.organization.application.port.out;

import com.propractix.organization.domain.model.OnboardingId;
import com.propractix.organization.domain.model.TenantId;
import java.util.UUID;

/** Server-generated RFC 4122 UUID v4, separate namespaces for operation and resources. */
public interface OnboardingIds {
    UUID nextOperationId();
    OnboardingId nextOnboardingId();
    TenantId nextTenantId();
    UUID nextEventId();
}
