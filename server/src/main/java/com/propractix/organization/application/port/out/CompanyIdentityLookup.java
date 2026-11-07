package com.propractix.organization.application.port.out;

import com.propractix.organization.domain.model.CountryCode;
import java.util.List;

/** Restricted global anti-duplicate index: never returns another tenant's ID or data. */
public interface CompanyIdentityLookup {
    boolean exists(CountryCode country, String taxType, List<TaxIdentityProtectionPort.Fingerprint> fingerprints);
}
