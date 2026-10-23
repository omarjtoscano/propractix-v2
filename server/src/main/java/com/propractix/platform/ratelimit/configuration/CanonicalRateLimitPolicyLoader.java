package com.propractix.platform.ratelimit.configuration;

import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.model.RateLimitPolicy;
import com.propractix.platform.ratelimit.application.port.out.RateLimitPolicyProvider;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/** Loads the immutable approved S0 artifact; the digest is an integrity anchor, not a second policy. */
public final class CanonicalRateLimitPolicyLoader implements RateLimitPolicyProvider {
    static final String RESOURCE = "policies/public-endpoint-rate-limits-v1.yaml";
    static final String APPROVED_SHA256 = "25d742d159890951d6d8bbe09ba6f221e9a3750a41fbc99ed6cc77156e0c7600";
    private final RateLimitPolicy policy;

    public CanonicalRateLimitPolicyLoader() {
        try (InputStream input = new ClassPathResource(RESOURCE).getInputStream()) {
            policy = load(input);
        } catch (Exception ex) {
            throw invalid();
        }
    }

    @Override public RateLimitPolicy policy() { return policy; }

    RateLimitPolicy load(InputStream input) {
        try {
            byte[] bytes = input.readNBytes(65537);
            if (bytes.length > 65536 || !APPROVED_SHA256.equals(HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(bytes)))) throw invalid();
            LoaderOptions options = new LoaderOptions();
            options.setAllowDuplicateKeys(false);
            options.setMaxAliasesForCollections(0);
            options.setCodePointLimit(65536);
            Map<?, ?> root = new Yaml(new SafeConstructor(options)).load(new java.io.ByteArrayInputStream(bytes));
            require(root, "apiVersion", "propractix.io/v1");
            require(root, "kind", "PublicEndpointRateLimitPolicy");
            Map<?, ?> metadata = map(root, "metadata");
            require(metadata, "status", "CLOSED");
            require(metadata, "approvedBy", "PRODUCT_OWNER");
            require(metadata, "approvedCommit", "fec3b2ed3a5afa04d1c079bda4f2d8981efd5920");
            Map<?, ?> spec = map(root, "spec");
            List<?> operations = (List<?>) spec.get("operations");
            if (operations.size() != 1) throw invalid();
            Map<?, ?> operation = (Map<?, ?>) operations.getFirst();
            require(operation, "operationId", "searchAcademicInstitutions");
            require(operation, "method", "GET");
            require(operation, "pathTemplate", "/api/v1/academic-institutions");
            require(operation, "contractVersion", "v1");
            List<RateLimitPolicy.Rule> rules = ((List<?>) operation.get("rules")).stream().map(value -> {
                Map<?, ?> rule = (Map<?, ?>) value;
                require(rule, "algorithm", "FIXED_WINDOW");
                return new RateLimitPolicy.Rule(text(rule, "ruleId"), (Integer) rule.get("limit"),
                        Duration.parse(text(rule, "window")), Duration.parse(text(rule, "counterTtl")));
            }).toList();
            Map<?, ?> hmac = map(map(spec, "subject"), "hmac");
            Map<?, ?> cleanup = map(map(spec, "storage"), "cleanup");
            Map<?, ?> thresholds = map(cleanup, "expiredRowsIncidentThresholds");
            return new RateLimitPolicy(text(metadata, "policyId"), (Integer) metadata.get("version"),
                    new RateLimitPolicy.Operation(text(operation, "operationId"), text(operation, "method"),
                            text(operation, "pathTemplate"), text(operation, "contractVersion"),
                            Set.copyOf(strings(operation, "queryParametersDoNotCreateNewSubjects")), rules),
                    new RateLimitPolicy.Hmac(text(hmac, "algorithm"), text(hmac, "purpose"),
                            text(hmac, "domainSeparator"), text(hmac, "encoding"), text(hmac, "writeVersion"),
                            strings(hmac, "readVersions")),
                    new RateLimitPolicy.Cleanup(Duration.parse(text(cleanup, "interval")),
                            ((Number) thresholds.get("rowCountGreaterThan")).longValue(),
                            Duration.parse(text(thresholds, "oldestExpiredAgeGreaterThan"))));
        } catch (Exception ex) {
            throw invalid();
        }
    }

    private static Map<?, ?> map(Map<?, ?> source, String name) { return (Map<?, ?>) source.get(name); }
    private static String text(Map<?, ?> source, String name) { return (String) source.get(name); }
    private static List<String> strings(Map<?, ?> source, String name) {
        return ((List<?>) source.get(name)).stream().map(String.class::cast).toList();
    }
    private static void require(Map<?, ?> source, String name, String expected) {
        if (!expected.equals(source.get(name))) throw invalid();
    }
    private static RateLimitFailure invalid() {
        return new RateLimitFailure(RateLimitFailure.ErrorClass.CONFIGURATION);
    }
}
