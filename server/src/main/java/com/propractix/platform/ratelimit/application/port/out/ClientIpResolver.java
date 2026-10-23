package com.propractix.platform.ratelimit.application.port.out;

import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import java.util.List;

public interface ClientIpResolver {
    ClientNetworkSubject resolve(String remoteAddress, List<String> xForwardedFor, List<String> forwarded);
}
