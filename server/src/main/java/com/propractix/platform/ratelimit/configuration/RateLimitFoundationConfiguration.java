package com.propractix.platform.ratelimit.configuration;

import com.propractix.platform.ratelimit.adapter.out.crypto.HmacClientIpFingerprint;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.port.out.ClientIpFingerprint;
import com.propractix.platform.ratelimit.application.port.out.ClientIpResolver;
import com.propractix.platform.ratelimit.application.port.out.RateLimitPolicyProvider;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.context.properties.bind.BindHandler;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.bind.handler.NoUnboundElementsBindHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
public class RateLimitFoundationConfiguration {
    @Bean
    RateLimitPolicyProvider rateLimitPolicyProvider() { return new CanonicalRateLimitPolicyLoader(); }

    @Bean
    RateLimitProperties rateLimitProperties(Environment environment) {
        try {
            return Binder.get(environment).bind("propractix.rate-limit", Bindable.of(RateLimitProperties.class),
                    new NoUnboundElementsBindHandler(BindHandler.DEFAULT)).orElseThrow(IllegalArgumentException::new);
        } catch (RuntimeException ex) {
            // Binding errors can include the raw rejected IP/header/secret value. Discard the entire cause.
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
        }
    }

    @Bean
    ClientIpResolver clientIpResolver(RateLimitProperties properties) {
        return new ConfiguredClientIpResolver(properties);
    }

    @Bean
    ClientIpFingerprint clientIpFingerprint(RateLimitPolicyProvider provider, Environment environment) {
        Map<String, byte[]> material = new LinkedHashMap<>();
        try {
            for (String version : provider.policy().hmac().readVersions()) {
                String value = environment.getProperty("rate-limit-hmac-" + version);
                if (value == null || value.isBlank()) throw new IllegalArgumentException();
                material.put(version, value.getBytes(StandardCharsets.UTF_8));
            }
            return new HmacClientIpFingerprint(provider.policy().hmac(), material);
        } catch (RuntimeException ex) {
            throw new RateLimitFailure(RateLimitFailure.ErrorClass.KEY);
        } finally {
            material.values().forEach(bytes -> Arrays.fill(bytes, (byte) 0));
        }
    }
}
