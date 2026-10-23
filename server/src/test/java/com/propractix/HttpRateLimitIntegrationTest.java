package com.propractix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.propractix.platform.ratelimit.adapter.in.http.HttpRateLimitFilter;
import com.propractix.platform.ratelimit.adapter.out.observability.MeteredRateLimitAdmission;
import com.propractix.platform.ratelimit.application.model.ClientNetworkSubject;
import com.propractix.platform.ratelimit.application.model.RateLimitDecision;
import com.propractix.platform.ratelimit.application.model.RateLimitFailure;
import com.propractix.platform.ratelimit.application.port.out.ClientIpResolver;
import com.propractix.platform.ratelimit.application.port.out.RateLimitBucketStore;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

@Import({TestcontainersConfiguration.class, HttpRateLimitIntegrationTest.ProbeConfiguration.class})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "propractix.rate-limit.ip-mode=X_FORWARDED_FOR",
        "propractix.rate-limit.trusted-proxy-cidrs[0]=10.0.0.0/8",
        "propractix.rate-limit.trusted-proxy-cidrs[1]=127.0.0.1/32",
        "propractix.rate-limit.caddy-sanitization-configured-and-tested=true"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class HttpRateLimitIntegrationTest {
    private static final String PATH = "/api/v1/academic-institutions";
    private static final String OPERATION = "searchAcademicInstitutions";
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JsonMapper json;
    @Autowired ProbeController controller;
    @Autowired FilterChainProxy security;
    @Autowired FilterRegistrationBean<HttpRateLimitFilter> registration;
    @MockitoSpyBean MeteredRateLimitAdmission admission;
    @MockitoSpyBean ClientIpResolver resolver;
    @MockitoSpyBean RateLimitBucketStore store;
    @LocalServerPort int port;

    @BeforeEach
    void resetState() {
        jdbc.execute("TRUNCATE platform_rate_limit_bucket");
        controller.calls.set(0);
    }

    @Test
    void oneAdmissionBeforeSecurityAndOneControllerExecutionPerRequest() throws Exception {
        mvc.perform(direct().header("X-Correlation-ID", "e5c-admitted")).andExpect(status().isOk());
        verify(admission).admit(eq(OPERATION), any());
        assertThat(controller.calls.get()).isEqualTo(1);
        assertThat(controller.correlation).isEqualTo("e5c-admitted");
        assertThat(security.getFilterChains()).allSatisfy(chain ->
                assertThat(chain.getFilters()).noneMatch(HttpRateLimitFilter.class::isInstance));
        assertThat(jdbc.queryForObject("SELECT sum(request_count) FROM platform_rate_limit_bucket", Long.class))
                .isEqualTo(2L);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    void queryCursorLimitAndSpoofedHeadersNeverCreateANewNetworkQuota() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(direct().queryParam("query", "search-" + i).queryParam("cursor", "cursor-" + i)
                    .queryParam("limit", Integer.toString(i + 1))
                    .header("X-Forwarded-For", "198.51.100." + (i + 1))
                    .header("Forwarded", "for=spoof-" + i)).andExpect(status().isOk());
        }
        assertThat(jdbc.queryForObject("SELECT count(DISTINCT subject_fingerprint) FROM platform_rate_limit_bucket", Long.class))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject("SELECT sum(request_count) FROM platform_rate_limit_bucket", Long.class))
                .isEqualTo(6L);
    }

    @Test
    void rejectedWindowsUseDecisionRetryAfterAndPreserveCorrelationWithoutExecutingController() throws Exception {
        doReturn(new RateLimitDecision(false, Duration.ofSeconds(37), Set.of("burst", "sustained")))
                .when(admission).admit(eq(OPERATION), any());
        MvcResult result = mvc.perform(direct().header("X-Correlation-ID", "e5c-limited")).andReturn();
        assertProblem(result, 429, "rate_limited", "37", "e5c-limited");
        assertThat(controller.calls.get()).isZero();
        assertThat(MDC.get("correlationId")).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"configuration", "key", "storage"})
    void unavailablePolicyKeyOrStorageFailsClosed(String kind) throws Exception {
        doThrow(new RateLimitFailure(RateLimitFailure.ErrorClass.valueOf(kind.toUpperCase(java.util.Locale.ROOT))))
                .when(admission).admit(eq(OPERATION), any());
        assertProblem(mvc.perform(direct()).andReturn(), 503, "rate_limiter_unavailable", "1", null);
        assertThat(controller.calls.get()).isZero();
    }

    @Test
    void actualStorageFailureClosesOperationButPreservesHealthBehavior() throws Exception {
        jdbc.execute("ALTER TABLE platform_rate_limit_bucket RENAME TO platform_rate_limit_bucket_e5c_test");
        try {
            assertProblem(mvc.perform(direct()).andReturn(), 503, "rate_limiter_unavailable", "1", null);
            assertThat(controller.calls.get()).isZero();
            mvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
            MvcResult readiness = mvc.perform(get("/actuator/health/readiness")).andExpect(status().isServiceUnavailable()).andReturn();
            assertThat(readiness.getResponse().getContentAsString()).isEqualTo("{\"status\":\"DOWN\"}");
            assertThat(readiness.getResponse().getHeader("Retry-After")).isNull();
        } finally {
            jdbc.execute("ALTER TABLE platform_rate_limit_bucket_e5c_test RENAME TO platform_rate_limit_bucket");
        }
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mvc.perform(get("/actuator/health/readiness")).andExpect(status().isOk());
        verify(admission).admit(eq(OPERATION), any());
    }

    @Test
    void timeoutAndUnexpectedProviderMessagesNeverEscapeIntoResponsesOrDiagnostics(CapturedOutput output) throws Exception {
        String sensitive = "SYNTHETIC-SECRET-query-cursor-jdbc-password-192.0.2.9";
        doThrow(new IllegalStateException(sensitive, new java.sql.SQLTimeoutException(sensitive)))
                .when(store).claim(any(), any());
        MvcResult result = mvc.perform(direct().queryParam("query", sensitive).queryParam("cursor", sensitive)
                .header("Authorization", "Bearer " + sensitive).header("Cookie", "session=" + sensitive)
                .header("X-Correlation-ID", "invalid " + sensitive)).andReturn();
        assertProblem(result, 503, "rate_limiter_unavailable", "1", null);
        assertThat(result.getResponse().getContentAsString() + result.getResponse().getHeaderNames() + output.getAll())
                .doesNotContain(sensitive, "192.0.2.9", "c0000209", "SQLTimeoutException", "IllegalStateException");
        assertThat(controller.calls.get()).isZero();
    }

    @Test
    void trustedChainStripsOnlyConfiguredHopsFromTheRight() throws Exception {
        mvc.perform(proxied().header("X-Forwarded-For", "198.51.100.7, 192.0.2.9, 10.2.3.4"))
                .andExpect(status().isOk());
        var subject = ArgumentCaptor.forClass(ClientNetworkSubject.class);
        verify(admission).admit(eq(OPERATION), subject.capture());
        assertThat(subject.getValue().canonicalBytes()).isEqualTo(
                new ClientNetworkSubject(new byte[]{(byte) 192, 0, 2, 9}).canonicalBytes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "unknown", "192.0.2.9,", "invalid, 192.0.2.9", "10.1.1.1, 10.2.2.2",
            "192.00.2.9", "192.0.2.9:80", "\"192.0.2.9\"", "192.0.2.9\r\nInjected: secret"})
    void malformedOrAllTrustedChainsRejectBeforeAdmission(String chain) throws Exception {
        assertProblem(mvc.perform(proxied().header("X-Forwarded-For", chain)).andReturn(),
                503, "rate_limiter_unavailable", "1", null);
        verify(admission, never()).admit(any(), any());
        assertThat(controller.calls.get()).isZero();
    }

    @Test
    void missingDuplicateConflictingAndOversizedProxyHeadersRejectBeforeAdmission(CapturedOutput output) throws Exception {
        for (var request : List.of(proxied(),
                proxied().header("X-Forwarded-For", "192.0.2.9", "198.51.100.1"),
                proxied().header("X-Forwarded-For", "192.0.2.9").header("Forwarded", "for=SENSITIVE-SPOOF"),
                proxied().header("X-Forwarded-For", "1".repeat(8193)))) {
            assertProblem(mvc.perform(request).andReturn(), 503, "rate_limiter_unavailable", "1", null);
        }
        verify(admission, never()).admit(any(), any());
        assertThat(controller.calls.get()).isZero();
        assertThat(output.getAll()).doesNotContain("192.0.2.9", "198.51.100.1", "SENSITIVE-SPOOF");
    }

    @Test
    void unknownRoutesAndOtherMethodsRemainDeniedWithoutConsumingQuota() throws Exception {
        for (String method : List.of("POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS")) {
            mvc.perform(request(HttpMethod.valueOf(method), PATH)).andExpect(status().isForbidden());
        }
        mvc.perform(get("/api/v1/not-implemented")).andExpect(status().isForbidden());
        mvc.perform(get(PATH + "/")).andExpect(status().isForbidden());
        verify(admission, never()).admit(any(), any());
        assertThat(controller.calls.get()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/v1/%61cademic-institutions", "/api/v1/academic-institution%73",
            "/api/v1/academic-institutions/", "/api/v1/academic-institutions;param=x"})
    void realServletEncodedAndAlternatePathsCannotBypassAdmission(String path) throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isIn(400, 403, 404);
        assertThat(controller.calls.get()).isZero();
        verify(admission, never()).admit(any(), any());
    }

    @Test
    void realServletFilterRegistrationRunsOnceAndUsesTheSocketInsteadOfForwardedHeaders() throws Exception {
        var response = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + PATH))
                .header("X-Forwarded-For", "192.0.2.9").header("X-Correlation-ID", "e5c-socket")
                .GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(controller.calls.get()).isEqualTo(1);
        verify(resolver).resolve(eq("127.0.0.1"), eq(List.of("192.0.2.9")), eq(List.of()));
        verify(admission).admit(eq(OPERATION), any());
        assertThat(response.headers().firstValue("X-Correlation-ID")).contains("e5c-socket");
        assertThat(registration.getOrder()).isEqualTo(org.springframework.core.Ordered.HIGHEST_PRECEDENCE + 1);
    }

    @Test
    void caddyRebuildsOnlyObservedPeerAndCannotBeSpoofedThroughEitherHeaderFamily(CapturedOutput output) throws Exception {
        String caddyfile = java.nio.file.Files.readString(java.nio.file.Path.of("../client/Caddyfile"))
                .replace("server:8080", "host.testcontainers.internal:" + port);
        // Use the production Caddyfile and pinned image; only the test upstream address changes.
        String image = java.nio.file.Files.readAllLines(java.nio.file.Path.of("../client/Dockerfile")).stream()
                .filter(line -> line.startsWith("FROM caddy:")).findFirst().orElseThrow().substring(5);
        org.testcontainers.Testcontainers.exposeHostPorts(port);
        try (var caddy = new org.testcontainers.containers.GenericContainer<>(org.testcontainers.utility.DockerImageName.parse(image))
                .withEnv("PROPRACTIX_SITE_ADDRESS", ":8080")
                .withCopyToContainer(org.testcontainers.images.builder.Transferable.of(caddyfile), "/etc/caddy/Caddyfile")
                .withExposedPorts(8080)
                .waitingFor(org.testcontainers.containers.wait.strategy.Wait.forHttp("/healthz").forPort(8080))) {
            caddy.start();
            String base = "http://" + caddy.getHost() + ":" + caddy.getMappedPort(8080);
            var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
            for (int i = 0; i < 2; i++) {
                var response = http.send(HttpRequest.newBuilder(URI.create(base + PATH + "?query=SENSITIVE-SEARCH-" + i))
                        .header("X-Forwarded-For", "198.51.100." + (i + 1) + ", 10.1.2.3")
                        .header("X-Forwarded-For", "203.0.113.9")
                        .header("Forwarded", "for=SENSITIVE-SPOOF-" + i)
                        .header("X-Correlation-ID", "e5c-caddy-" + i).GET().build(), HttpResponse.BodyHandlers.ofString());
                assertThat(response.statusCode()).isEqualTo(200);
                assertThat(response.headers().firstValue("X-Correlation-ID")).contains("e5c-caddy-" + i);
            }
            ArgumentCaptor<List<String>> headers = ArgumentCaptor.captor();
            verify(resolver, org.mockito.Mockito.times(2)).resolve(any(), headers.capture(), eq(List.of()));
            assertThat(headers.getAllValues().getFirst()).hasSize(1);
            assertThat(headers.getAllValues().getFirst()).isEqualTo(headers.getAllValues().getLast());
            assertThat(headers.getAllValues().getFirst().getFirst().toString())
                    .doesNotContain("198.51.100.", "203.0.113.9", "10.1.2.3", ",");
            assertThat(controller.calls.get()).isEqualTo(2);

            doReturn(new RateLimitDecision(false, Duration.ofSeconds(11), Set.of("burst")))
                    .when(admission).admit(eq(OPERATION), any());
            var limited = http.send(HttpRequest.newBuilder(URI.create(base + PATH))
                    .header("Forwarded", "for=SENSITIVE-SPOOF-LIMITED").GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(limited.statusCode()).isEqualTo(429);
            assertThat(limited.headers().firstValue("Retry-After")).contains("11");
            assertThat(json.readTree(limited.body()).path("correlationId").asText())
                    .isEqualTo(limited.headers().firstValue("X-Correlation-ID").orElseThrow());

            doThrow(new RateLimitFailure(RateLimitFailure.ErrorClass.STORAGE)).when(admission).admit(eq(OPERATION), any());
            var unavailable = http.send(HttpRequest.newBuilder(URI.create(base + PATH)).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(unavailable.statusCode()).isEqualTo(503);
            assertThat(unavailable.headers().firstValue("Retry-After")).contains("1");
            assertThat(json.readTree(unavailable.body()).path("correlationId").asText())
                    .isEqualTo(unavailable.headers().firstValue("X-Correlation-ID").orElseThrow());
            assertThat(controller.calls.get()).isEqualTo(2);
            assertThat(http.send(HttpRequest.newBuilder(URI.create(base + "/healthz")).GET().build(),
                    HttpResponse.BodyHandlers.ofString()).body()).isEqualTo("ok");
            var ready = http.send(HttpRequest.newBuilder(URI.create(base + "/readyz")).GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(ready.statusCode()).isEqualTo(200);
            assertThat(ready.body()).isEqualTo("{\"status\":\"UP\"}");
            verify(admission, org.mockito.Mockito.times(4)).admit(eq(OPERATION), any());
            // Even Caddy transport-error diagnostics must not retain the request or its sensitive values.
            caddy.copyFileToContainer(org.testcontainers.images.builder.Transferable.of(
                    caddyfile.replace("host.testcontainers.internal:" + port, "127.0.0.1:1")), "/etc/caddy/Caddyfile");
            assertThat(caddy.execInContainer("caddy", "reload", "--config", "/etc/caddy/Caddyfile").getExitCode()).isZero();
            var transportError = http.send(HttpRequest.newBuilder(URI.create(base + PATH + "?query=SENSITIVE-SEARCH-ERROR"))
                    .header("Forwarded", "for=SENSITIVE-SPOOF-ERROR").GET().build(), HttpResponse.BodyHandlers.ofString());
            assertThat(transportError.statusCode()).isEqualTo(502);
            String caddyLogs = caddy.getLogs();
            assertThat(caddyLogs).contains("http.log.error").doesNotContain("remote_ip", "client_ip", "SENSITIVE-SEARCH-ERROR");
            assertThat(caddyLogs + output.getAll() + limited.body() + unavailable.body())
                    .doesNotContain("SENSITIVE-SPOOF", "SENSITIVE-SEARCH", "198.51.100.", "203.0.113.9");
        }
    }

    private MockHttpServletRequestBuilder direct() {
        return get(PATH).with(request -> { request.setRemoteAddr("192.0.2.9"); return request; });
    }

    private MockHttpServletRequestBuilder proxied() {
        return get(PATH).with(request -> { request.setRemoteAddr("10.0.0.1"); return request; });
    }

    private void assertProblem(MvcResult result, int status, String code, String retry, String correlation) throws Exception {
        var response = result.getResponse();
        assertThat(response.getStatus()).isEqualTo(status);
        assertThat(response.getContentType()).isEqualTo("application/problem+json");
        assertThat(response.getHeader("Retry-After")).isEqualTo(retry);
        String actualCorrelation = response.getHeader("X-Correlation-ID");
        assertThat(actualCorrelation).matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}");
        if (correlation != null) assertThat(actualCorrelation).isEqualTo(correlation);
        var body = json.readTree(response.getContentAsString());
        assertThat(body.path("type").asText()).isEqualTo("urn:propractix:problem:" + code);
        assertThat(body.path("title").asText()).isEqualTo(code);
        assertThat(body.path("detail").asText()).isEqualTo(code);
        assertThat(body.path("status").asInt()).isEqualTo(status);
        assertThat(body.path("code").asText()).isEqualTo(code);
        assertThat(body.path("instance").asText()).isEqualTo(PATH);
        assertThat(body.path("correlationId").asText()).isEqualTo(actualCorrelation);
        assertThat(body.path("violations").isEmpty()).isTrue();
        assertThat(response.getContentAsString()).doesNotContain("192.0.2.9", "10.0.0.1", "fingerprint", "jdbc", "stackTrace");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {
        @Bean ProbeController rateLimitProbeController() { return new ProbeController(); }
    }

    /** Deliberately test-only: E6 owns the functional catalog controller. */
    @org.springframework.boot.test.context.TestComponent
    @RestController
    static class ProbeController {
        final AtomicInteger calls = new AtomicInteger();
        volatile String correlation;
        @GetMapping(PATH)
        String probe() {
            calls.incrementAndGet();
            correlation = MDC.get("correlationId");
            return "test-only";
        }
    }
}
