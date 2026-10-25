package com.propractix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutions;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionQueryRepository;
import com.propractix.academicinstitution.configuration.AcademicInstitutionSearchProperties;
import com.propractix.academicinstitution.adapter.in.rest.AcademicInstitutionSearchMapper;
import com.propractix.academicinstitution.adapter.in.rest.InstitutionSearchCursorCodec;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class AcademicInstitutionRestIntegrationTest {
    private static final String PATH = "/api/v1/academic-institutions";
    @Autowired MockMvc mvc;
    @Autowired JsonMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired PublishInstitutionCatalog publisher;
    @Autowired AcademicInstitutionSearchProperties properties;
    @MockitoSpyBean SearchPublishedInstitutions search;
    @MockitoSpyBean PublishedInstitutionQueryRepository repository;

    @BeforeEach
    void publishSyntheticSnapshot() {
        jdbc.execute("TRUNCATE platform_rate_limit_bucket, academicinstitution_institution, academicinstitution_catalog_import CASCADE");
        SyntheticCatalog.publish(publisher, IntStream.range(0, 55)
                .mapToObj(i -> "Universidad Sintética %02d".formatted(i)).toArray(String[]::new));
    }

    @Test
    void openApiDeclaresOnlyTheApprovedOperationAndMatchesTypedPaginationAndRuntimeProjection() throws Exception {
        assertThat(AcademicInstitutionOpenApiContract.paths().propertyNames()).containsExactly(PATH);
        assertThat(AcademicInstitutionOpenApiContract.paths().path(PATH).propertyNames()).containsExactly("get");
        var operation = AcademicInstitutionOpenApiContract.OPERATION;
        assertThat(operation.path("operationId").asText()).isEqualTo("searchAcademicInstitutions");
        assertThat(operation.path("security").isEmpty()).isTrue();
        assertThat(operation.path("responses").propertyNames()).containsExactlyInAnyOrder("200", "400", "409", "429", "503");
        for (JsonNode parameter : operation.path("parameters")) {
            if (parameter.path("name").asText().equals("query")) {
                assertThat(parameter.at("/schema/maxLength").asInt()).isEqualTo(AcademicInstitutionSearchMapper.MAX_QUERY_CODE_POINTS);
            }
            if (parameter.path("name").asText().equals("cursor")) {
                assertThat(parameter.at("/schema/maxLength").asInt()).isEqualTo(InstitutionSearchCursorCodec.MAX_ENCODED_LENGTH);
            }
            if (parameter.path("name").asText().equals("limit")) {
                assertThat(parameter.at("/schema/default").asInt()).isEqualTo(properties.defaultLimit()).isEqualTo(20);
                assertThat(parameter.at("/schema/maximum").asInt()).isEqualTo(properties.maxLimit()).isEqualTo(50);
            }
        }
        var body = page(mvc.perform(get(PATH)).andExpect(status().isOk()).andReturn());
        assertThat(body.path("items").size()).isEqualTo(properties.defaultLimit());
        assertThat(body.at("/snapshot/retrievedAt").asText()).isEqualTo(SyntheticCatalog.RETRIEVED_AT);
        assertThat(body.at("/snapshot/sourceUrl").asText()).isEqualTo(SyntheticCatalog.SOURCE);
        assertThat(body.at("/snapshot/sourceAttribution").asText()).isEqualTo(SyntheticCatalog.ATTRIBUTION);
        assertThat(body.toString()).doesNotContain("sourceRecordId", "sourceSystem", "sourceStatus", "INTERNAL-REF",
                "INTERNAL-STATUS", "artifactHash", "snapshotId");
        var max = page(mvc.perform(get(PATH).queryParam("limit", "50")).andExpect(status().isOk()).andReturn());
        assertThat(max.path("items").size()).isEqualTo(50);
    }

    @Test
    void keysetPaginationReturnsEachInternalUuidOnceInStableOrderAndEndsWithNullCursor() throws Exception {
        // Two distinct IDs with the same normalized order key exercise the UUID tiebreaker.
        SyntheticCatalog.publish(publisher, "Universidad Ábaco", "Universidad Abaco", "Universidad Beta");
        var expected = jdbc.queryForList("SELECT institution_id::text FROM academicinstitution_institution "
                + "WHERE published_catalog_import_id IS NOT NULL ORDER BY search_name_normalized, institution_id", String.class);
        List<String> actual = new ArrayList<>();
        String cursor = null;
        for (int i = 0; i < 3; i++) {
            var request = get(PATH).queryParam("limit", "1");
            if (cursor != null) request.queryParam("cursor", cursor);
            var body = page(mvc.perform(request).andExpect(status().isOk()).andReturn());
            actual.add(body.at("/items/0/id").asText());
            cursor = body.path("nextCursor").isNull() ? null : body.path("nextCursor").asText();
            assertThat(cursor == null).isEqualTo(i == 2);
        }
        assertThat(actual).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
    }

    @Test
    void defaultPagesHaveNoGapsAndCanChangePageSizeWithoutChangingTheQuery() throws Exception {
        var first = page(mvc.perform(get(PATH)).andExpect(status().isOk()).andReturn());
        var second = page(mvc.perform(get(PATH).queryParam("limit", "50")
                .queryParam("cursor", first.path("nextCursor").asText())).andExpect(status().isOk()).andReturn());
        assertThat(first.path("items").size()).isEqualTo(20);
        assertThat(second.path("items").size()).isEqualTo(35);
        assertThat(second.path("nextCursor").isNull()).isTrue();
        List<String> ids = new ArrayList<>();
        for (var body : List.of(first, second)) for (JsonNode item : body.path("items")) ids.add(item.path("id").asText());
        assertThat(ids).hasSize(55).doesNotHaveDuplicates();
    }

    @Test
    void normalizesUnicodeQueriesPreservesOfficialNamesAndRejectsCursorForAnotherQuery() throws Exception {
        var first = page(mvc.perform(get(PATH).queryParam("query", "\u00a0 SINTÉTICA  ").queryParam("limit", "1"))
                .andExpect(status().isOk()).andReturn());
        assertThat(first.at("/items/0/officialName").asText()).isEqualTo("Universidad Sintética 00");
        String cursor = first.path("nextCursor").asText();
        page(mvc.perform(get(PATH).queryParam("query", "sinte\u0301tica").queryParam("cursor", cursor))
                .andExpect(status().isOk()).andReturn());
        problem(mvc.perform(get(PATH).queryParam("query", "otra").queryParam("cursor", cursor)).andReturn(),
                400, "validation_error", "cursor");
        for (String blank : List.of("", "\u00a0 \u2003")) {
            var body = page(mvc.perform(get(PATH).queryParam("query", blank)).andExpect(status().isOk()).andReturn());
            assertThat(body.path("items").size()).isEqualTo(20);
        }
        var empty = page(mvc.perform(get(PATH).queryParam("query", "no-match")).andExpect(status().isOk()).andReturn());
        assertThat(empty.path("items").isEmpty()).isTrue();
        assertThat(empty.path("nextCursor").isNull()).isTrue();
    }

    @Test
    void searchesContainsAndEscapesSqlWildcardCharactersAsLiterals() throws Exception {
        SyntheticCatalog.publish(publisher, "Universidad 100%", "Universidad _", "Universidad \\");
        for (String query : List.of("100%", "_", "\\")) {
            var body = page(mvc.perform(get(PATH).queryParam("query", query)).andExpect(status().isOk()).andReturn());
            assertThat(body.path("items").size()).isEqualTo(1);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "0", "-1", "51", "1.0", "one", "2147483648", "1e1", "+1", " 1"})
    void invalidLimitNeverCallsTheApplication(String limit) throws Exception {
        problem(mvc.perform(get(PATH).queryParam("limit", limit)).andReturn(), 400, "validation_error", "limit");
        verify(search, never()).search(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "not.a.cursor", "e30", "bnVsbA", "e30=", "W10"})
    void invalidCursorNeverCallsTheApplication(String cursor) throws Exception {
        problem(mvc.perform(get(PATH).queryParam("cursor", cursor)).andReturn(), 400, "validation_error", "cursor");
        verify(search, never()).search(any());
    }

    @Test
    void queryControlsOversizedInputAndDuplicateParametersHaveSafeFieldViolations() throws Exception {
        for (String query : List.of("a".repeat(301), "private\nvalue", "private\u0000value", "private\u2028value", "\u0301")) {
            problem(mvc.perform(get(PATH).queryParam("query", query)).andReturn(), 400, "validation_error", "query");
        }
        for (String field : List.of("query", "cursor", "limit")) {
            problem(mvc.perform(get(PATH).queryParam(field, "1", "2")).andReturn(), 400, "validation_error", field);
        }
        problem(mvc.perform(get(PATH).queryParam("cursor", "a".repeat(4097))).andReturn(),
                400, "validation_error", "cursor");
        verify(search, never()).search(any());
    }

    @Test
    void replacingSnapshotReturnsConflictAndAFirstPageRestartsSuccessfully() throws Exception {
        var first = page(mvc.perform(get(PATH)).andExpect(status().isOk()).andReturn());
        SyntheticCatalog.publish(publisher, "Universidad Nueva");
        problem(mvc.perform(get(PATH).queryParam("cursor", first.path("nextCursor").asText())).andReturn(),
                409, "catalog_snapshot_changed", null);
        var restarted = page(mvc.perform(get(PATH)).andExpect(status().isOk()).andReturn());
        assertThat(restarted.at("/items/0/officialName").asText()).isEqualTo("Universidad Nueva");
    }

    @Test
    void missingSnapshotAndRealStorageFailureMapToTheSamePublicUnavailableCode() throws Exception {
        jdbc.execute("TRUNCATE academicinstitution_institution, academicinstitution_catalog_import CASCADE");
        problem(mvc.perform(get(PATH)).andReturn(), 503, "academic_institution_catalog_unavailable", null);
        SyntheticCatalog.publish(publisher, "Universidad Disponible");
        jdbc.execute("ALTER TABLE academicinstitution_institution RENAME TO academicinstitution_institution_e6_test");
        try {
            problem(mvc.perform(get(PATH)).andReturn(), 503, "academic_institution_catalog_unavailable", null);
        } finally {
            jdbc.execute("ALTER TABLE academicinstitution_institution_e6_test RENAME TO academicinstitution_institution");
        }
        page(mvc.perform(get(PATH)).andExpect(status().isOk()).andReturn());
    }

    @Test
    void unexpectedProviderFailureIsNotMisclassifiedAsValidationAndNeverLeaksDiagnostics(CapturedOutput output) throws Exception {
        String sensitive = "SENSITIVE-query-cursor-jdbc-password-192.0.2.9";
        doThrow(new IllegalArgumentException(sensitive)).when(repository).search(any());
        var result = mvc.perform(get(PATH).queryParam("query", sensitive)
                .header("X-Correlation-ID", "invalid " + sensitive)).andReturn();
        problem(result, 503, "academic_institution_catalog_unavailable", null);
        assertThat(result.getResponse().getContentAsString() + output.getAll()).doesNotContain(sensitive,
                "IllegalArgumentException", "SQLTimeoutException");
    }

    @Test
    void correlationReachesTheApplicationAndMdcIsClearedAfterSuccessAndValidationError() throws Exception {
        doAnswer(invocation -> {
            assertThat(MDC.get("correlationId")).isEqualTo("e6-request");
            return invocation.callRealMethod();
        }).when(search).search(any());
        var success = mvc.perform(get(PATH).header("X-Correlation-ID", "e6-request")).andExpect(status().isOk()).andReturn();
        page(success);
        assertThat(success.getResponse().getHeader("X-Correlation-ID")).isEqualTo("e6-request");
        assertThat(MDC.get("correlationId")).isNull();
        var invalid = mvc.perform(get(PATH).queryParam("limit", "invalid")
                .header("X-Correlation-ID", "invalid correlation")).andReturn();
        problem(invalid, 400, "validation_error", "limit");
        assertThat(invalid.getResponse().getHeader("X-Correlation-ID")).isNotEqualTo("invalid correlation");
        assertThat(MDC.get("correlationId")).isNull();
    }

    private JsonNode page(MvcResult result) throws Exception {
        AcademicInstitutionOpenApiContract.assertResponse(result);
        return json.readTree(result.getResponse().getContentAsString());
    }

    private void problem(MvcResult result, int status, String code, String field) throws Exception {
        assertThat(result.getResponse().getStatus()).isEqualTo(status);
        AcademicInstitutionOpenApiContract.assertResponse(result);
        var body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("code").asText()).isEqualTo(code);
        if (field != null) {
            assertThat(body.at("/violations/0/field").asText()).isEqualTo(field);
            assertThat(body.at("/violations/0/code").asText()).isEqualTo("validation_error");
        } else {
            assertThat(body.path("violations").isEmpty()).isTrue();
        }
        assertThat(body.toString()).doesNotContain("sourceRecordId", "sourceStatus", "stackTrace", "fingerprint", "jdbc:");
        assertThat(MDC.get("correlationId")).isNull();
    }
}
