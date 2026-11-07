package com.propractix.organization.application.port.out;

import com.propractix.organization.domain.model.CountryCode;
import com.propractix.organization.domain.model.TaxIdentifier;
import java.util.List;
import java.util.Objects;

/** Encrypt with a separate key; compute blind indexes for ALL active ordered HMAC versions. */
public interface TaxIdentityProtectionPort {
    ProtectedTaxIdentity protect(CountryCode country, TaxIdentifier taxIdentifier);

    record Fingerprint(String keyVersion, String value) {
        public Fingerprint {
            requireText(keyVersion);
            requireText(value);
        }

        @Override
        public String toString() { return "Fingerprint[redacted]"; }
    }

    record ProtectedTaxIdentity(String encryptionKeyVersion, String ciphertext, List<Fingerprint> fingerprints) {
        public ProtectedTaxIdentity {
            requireText(encryptionKeyVersion);
            requireText(ciphertext);
            fingerprints = List.copyOf(fingerprints);
            if (fingerprints.isEmpty() || fingerprints.stream().map(Fingerprint::keyVersion).distinct().count()
                    != fingerprints.size()) {
                throw new IllegalArgumentException("active fiscal fingerprints must be present and distinct");
            }
        }

        @Override
        public String toString() { return "ProtectedTaxIdentity[redacted]"; }
    }

    private static void requireText(String value) {
        Objects.requireNonNull(value, "protected fiscal value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("protected fiscal value must not be blank");
        }
    }
}
