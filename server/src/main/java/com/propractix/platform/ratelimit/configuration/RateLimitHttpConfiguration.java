package com.propractix.platform.ratelimit.configuration;

import com.propractix.platform.ratelimit.adapter.in.http.HttpRateLimitFilter;
import com.propractix.platform.ratelimit.adapter.in.http.RateLimitedHttpOperation;
import com.propractix.platform.ratelimit.adapter.out.observability.MeteredRateLimitAdmission;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.port.out.ClientIpResolver;
import com.propractix.platform.ratelimit.application.port.out.RateLimitPolicyProvider;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.server.autoconfigure.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import tools.jackson.databind.json.JsonMapper;

@Configuration(proxyBeanMethods = false)
public class RateLimitHttpConfiguration {
    @Bean
    RateLimitedHttpOperation rateLimitedHttpOperation(RateLimitPolicyProvider policy, ServerProperties server) {
        // The resolver must see the socket peer, never a value rewritten by Tomcat or ForwardedHeaderFilter.
        if (server.getForwardHeadersStrategy() != ServerProperties.ForwardHeadersStrategy.NONE) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
        }
        return new RateLimitedHttpOperation(policy.policy().operation());
    }

    @Bean
    FilterRegistrationBean<HttpRateLimitFilter> httpRateLimitFilter(RateLimitedHttpOperation operation,
            ClientIpResolver resolver, MeteredRateLimitAdmission admission, JsonMapper json) {
        var registration = new FilterRegistrationBean<>(
                new HttpRateLimitFilter(operation, resolver, admission, json, admission::error));
        registration.setName("httpRateLimitFilter");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 1);
        registration.setDispatcherTypes(DispatcherType.REQUEST);
        return registration;
    }
}
