package com.propractix.platform.ratelimit.configuration;

import com.propractix.platform.ratelimit.adapter.in.maintenance.RateLimitCleanupJob;
import com.propractix.platform.ratelimit.adapter.out.observability.MeteredRateLimitAdmission;
import com.propractix.platform.ratelimit.adapter.out.observability.RateLimitHealthIndicator;
import com.propractix.platform.ratelimit.application.port.out.ClientIpFingerprint;
import com.propractix.platform.ratelimit.application.port.out.RateLimitPolicyProvider;
import com.propractix.platform.ratelimit.application.port.out.RateLimitBucketStore;
import com.propractix.platform.ratelimit.application.port.out.RateLimitBucketMaintenance;
import com.propractix.platform.ratelimit.application.service.RateLimitAdmissionService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class RateLimitPersistenceConfiguration {
    @Bean
    MeteredRateLimitAdmission rateLimitAdmission(RateLimitPolicyProvider provider, ClientIpFingerprint fingerprint,
                                                RateLimitBucketStore store, MeterRegistry registry) {
        return new MeteredRateLimitAdmission(new RateLimitAdmissionService(provider, fingerprint, store), provider.policy(), registry);
    }

    @Bean
    RateLimitHealthIndicator rateLimitHealthIndicator(RateLimitBucketMaintenance store, RateLimitPolicyProvider provider) {
        return new RateLimitHealthIndicator(store, provider.policy());
    }

    @Bean
    RateLimitCleanupJob rateLimitCleanupJob(RateLimitBucketMaintenance store, RateLimitPolicyProvider provider,
                                          MeteredRateLimitAdmission metrics) {
        return new RateLimitCleanupJob(store, provider.policy(), metrics::error);
    }
}
