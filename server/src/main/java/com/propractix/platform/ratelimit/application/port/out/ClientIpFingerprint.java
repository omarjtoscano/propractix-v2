package com.propractix.platform.ratelimit.application.port.out;

import com.propractix.platform.ratelimit.application.model.ClientIpFingerprints;
import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;

public interface ClientIpFingerprint {
    /** Computes all active read versions, including the write version, without partial success. */
    ClientIpFingerprints fingerprint(ClientNetworkSubject subject);
}
