package com.propractix.platform.ratelimit.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.propractix.platform.ratelimit.application.port.out.ClientIpFingerprint;
import com.propractix.platform.ratelimit.application.port.out.ClientIpResolver;
import com.propractix.platform.ratelimit.application.port.out.RateLimitPolicyProvider;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class RateLimitFoundationConfigurationTest {
    @TempDir Path secrets;

    private ApplicationContextRunner context() {
        return new ApplicationContextRunner().withUserConfiguration(RateLimitFoundationConfiguration.class)
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.config.import=configtree:" + secrets.toAbsolutePath() + "/",
                        "propractix.rate-limit.ip-mode=REMOTE_ADDRESS");
    }

    private void keyFile(String value) throws Exception {
        Files.writeString(secrets.resolve("rate-limit-hmac-rl-ip-v1"), value);
    }

    @Test
    void loadsCanonicalPolicyAndExternalFileSecretWithoutAnyHttpOrStoreBean(CapturedOutput output) throws Exception {
        String secret = "SYNTHETIC-TEST-FILE-ONLY-DO-NOT-LOG-THIS-KEY";
        keyFile(secret);
        context().run(ctx -> {
            assertThat(ctx).hasNotFailed().hasSingleBean(RateLimitProperties.class)
                    .hasSingleBean(ClientIpResolver.class).hasSingleBean(ClientIpFingerprint.class)
                    .hasSingleBean(RateLimitPolicyProvider.class);
            assertThat(ctx.getBeanNamesForType(com.propractix.platform.ratelimit.application.port.in.RateLimitAdmission.class))
                    .isEmpty();
            assertThat(ctx.getBeanNamesForType(com.propractix.platform.ratelimit.application.port.out.RateLimitBucketStore.class))
                    .isEmpty();
            var subject = ctx.getBean(ClientIpResolver.class).resolve("192.0.2.9", null, null);
            assertThat(ctx.getBean(ClientIpFingerprint.class).fingerprint(subject).versions()).hasSize(1);
        });
        assertThat(output.getAll()).doesNotContain(secret, "192.0.2.9", "rl-ip-v1", "c0000209");
    }

    @Test
    void rejectsMissingAndWeakExternalMaterialWithoutLeakingBindingValues(CapturedOutput output) throws Exception {
        context().run(ctx -> assertSafeFailure(ctx.getStartupFailure(), "key", ""));
        String secret = "SHORT-SECRET-DO-NOT-LOG";
        keyFile(secret);
        context().run(ctx -> assertSafeFailure(ctx.getStartupFailure(), "key", secret));
        assertThat(output.getAll()).doesNotContain(secret, "rl-ip-v1");
    }

    @Test
    void rejectsUnknownModesAndFieldsInsideASanitizedBindingBoundary(CapturedOutput output) throws Exception {
        String secret = "SYNTHETIC-CONFIGURATION-TEST-KEY-DO-NOT-LOG";
        keyFile(secret);
        for (String property : List.of("propractix.rate-limit.ip-mode=FORWARDED",
                "propractix.rate-limit.ip-mode=" + secret,
                "propractix.rate-limit.unrecognized=" + secret,
                "propractix.rate-limit.trusted-proxy-cidrs[0]=192.0.2.9/0")) {
            context().withPropertyValues(property).run(ctx -> assertSafeFailure(ctx.getStartupFailure(), "configuration", secret));
        }
        assertThat(output.getAll()).doesNotContain(secret, "192.0.2.9", "rl-ip-v1");
    }

    @Test
    void proxyModeRequiresBothExplicitCidrsAndSanitizationEvidence() throws Exception {
        keyFile("SYNTHETIC-LOCAL-ONLY-NOT-FOR-DEPLOYMENT-KEY");
        context().withPropertyValues("propractix.rate-limit.ip-mode=X_FORWARDED_FOR")
                .run(ctx -> assertThat(ctx).hasFailed());
        context().withPropertyValues("propractix.rate-limit.ip-mode=X_FORWARDED_FOR",
                "propractix.rate-limit.trusted-proxy-cidrs[0]=10.0.0.0/8")
                .run(ctx -> assertThat(ctx).hasFailed());
        context().withPropertyValues("propractix.rate-limit.ip-mode=X_FORWARDED_FOR",
                "propractix.rate-limit.caddy-sanitization-configured-and-tested=true")
                .run(ctx -> assertThat(ctx).hasFailed());
        context().withPropertyValues("propractix.rate-limit.ip-mode=X_FORWARDED_FOR",
                "propractix.rate-limit.trusted-proxy-cidrs[0]=10.0.0.0/8",
                "propractix.rate-limit.caddy-sanitization-configured-and-tested=true")
                .run(ctx -> assertThat(ctx).hasNotFailed());
    }

    private static void assertSafeFailure(Throwable failure, String errorClass, String secret) {
        assertThat(failure).isNotNull();
        StringWriter trace = new StringWriter();
        failure.printStackTrace(new PrintWriter(trace));
        assertThat(trace.toString()).contains("rate_limiter_unavailable:" + errorClass)
                .doesNotContain("192.0.2.9", "rl-ip-v1", "BindException", "UnboundConfigurationPropertiesException");
        if (!secret.isEmpty()) assertThat(trace.toString()).doesNotContain(secret);
    }
}
