package com.propractix.platform.ratelimit.adapter.out.crypto;

import com.propractix.platform.ratelimit.application.model.ClientIpFingerprints;
import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;
import com.propractix.platform.ratelimit.application.model.SubjectFingerprint;
import com.propractix.platform.ratelimit.application.port.out.ClientIpFingerprint;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HmacClientIpFingerprint implements ClientIpFingerprint {
    private final RateLimitPolicy.Hmac policy;
    private final Map<String, SecretKeySpec> keys;

    public HmacClientIpFingerprint(RateLimitPolicy.Hmac policy, Map<String, byte[]> material) {
        try {
            this.policy = policy;
            Map<String, SecretKeySpec> validated = new LinkedHashMap<>();
            for (String version : policy.readVersions()) {
                byte[] key = material.get(version);
                if (key == null || key.length < 32) throw new IllegalArgumentException();
                validated.put(version, new SecretKeySpec(key, "HmacSHA256"));
            }
            keys = Map.copyOf(validated);
        } catch (RuntimeException ex) {
            throw invalid();
        }
    }

    @Override public ClientIpFingerprints fingerprint(ClientNetworkSubject subject) {
        byte[] canonical = null;
        try {
            canonical = subject.canonicalBytes();
            Map<String, SubjectFingerprint> result = new LinkedHashMap<>();
            for (String version : policy.readVersions()) {
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(keys.get(version));
                mac.update(policy.domainSeparator().getBytes(StandardCharsets.UTF_8));
                result.put(version, new SubjectFingerprint(Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(mac.doFinal(canonical))));
            }
            return new ClientIpFingerprints(result);
        } catch (Exception ex) {
            throw invalid();
        } finally {
            if (canonical != null) Arrays.fill(canonical, (byte) 0);
        }
    }

    @Override public String toString() { return "HmacClientIpFingerprint[redacted]"; }
    private static RateLimitFailure invalid() { return new RateLimitFailure(RateLimitFailure.ErrorClass.KEY); }
}
