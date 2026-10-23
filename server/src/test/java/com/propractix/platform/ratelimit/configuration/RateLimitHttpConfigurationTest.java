package com.propractix.platform.ratelimit.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.propractix.platform.ratelimit.adapter.in.http.RateLimitedHttpOperation;
import com.propractix.platform.ratelimit.adapter.out.observability.MeteredRateLimitAdmission;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.port.out.ClientIpResolver;
import com.propractix.platform.ratelimit.application.port.out.RateLimitPolicyProvider;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import tools.jackson.databind.json.JsonMapper;

class RateLimitHttpConfigurationTest {
    @ParameterizedTest
    @EnumSource(ServerProperties.ForwardHeadersStrategy.class)
    void onlyTheValidatedResolverMayProcessForwardingHeaders(ServerProperties.ForwardHeadersStrategy strategy) {
        var server = new ServerProperties();
        server.setForwardHeadersStrategy(strategy);
        new ApplicationContextRunner().withUserConfiguration(RateLimitHttpConfiguration.class)
                .withBean(ServerProperties.class, () -> server)
                .withBean(RateLimitPolicyProvider.class, CanonicalRateLimitPolicyLoader::new)
                .withBean(ClientIpResolver.class, () -> mock(ClientIpResolver.class))
                .withBean(MeteredRateLimitAdmission.class, () -> mock(MeteredRateLimitAdmission.class))
                .withBean(JsonMapper.class, JsonMapper::new)
                .run(context -> {
                    if (strategy == ServerProperties.ForwardHeadersStrategy.NONE) {
                        assertThat(context).hasNotFailed().hasSingleBean(RateLimitedHttpOperation.class);
                    } else {
                        assertThat(context).hasFailed();
                        assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(RateLimitFailure.class)
                                .hasRootCauseMessage("rate_limiter_unavailable:configuration");
                    }
                });
    }
}
