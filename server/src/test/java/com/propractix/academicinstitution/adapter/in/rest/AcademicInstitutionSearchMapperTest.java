package com.propractix.academicinstitution.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.propractix.academicinstitution.application.query.InstitutionSearchCursor;
import com.propractix.academicinstitution.application.query.SearchPublishedInstitutionsQuery;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.LinkedMultiValueMap;
import tools.jackson.databind.json.JsonMapper;

class AcademicInstitutionSearchMapperTest {
    private final InstitutionSearchCursorCodec codec = new InstitutionSearchCursorCodec(JsonMapper.builder().build());
    private final AcademicInstitutionSearchMapper mapper = new AcademicInstitutionSearchMapper(7, 12, codec);

    @Test
    void usesConfiguredPaginationAndCodePointsInsteadOfUtf16Length() {
        var parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("query", "𐐨".repeat(300));
        assertThat(mapper.toQuery(parameters).limit()).isEqualTo(7);
        parameters.add("limit", "12");
        assertThat(mapper.toQuery(parameters).limit()).isEqualTo(12);
        parameters.set("limit", "13");
        assertThatThrownBy(() -> mapper.toQuery(parameters)).isInstanceOf(InvalidSearchParameter.class);
        parameters.remove("limit");
        parameters.set("query", "\uD800");
        assertThatThrownBy(() -> mapper.toQuery(parameters)).isInstanceOf(InvalidSearchParameter.class);
    }

    @Test
    void cursorRoundTripPreservesSnapshotOrderingAndQueryBinding() {
        var parameters = new LinkedMultiValueMap<String, String>();
        parameters.add("query", "  SINTÉTICA\u00a0  ");
        var query = mapper.toQuery(parameters);
        var cursor = InstitutionSearchCursor.after(new CatalogImportId(UUID.randomUUID()), query.queryFingerprint(),
                "universidad sintetica", new AcademicInstitutionId(UUID.randomUUID()));
        String encoded = codec.encode(cursor);
        assertThat(encoded).matches("[A-Za-z0-9_-]+");
        assertThat(codec.decode(encoded)).isEqualTo(cursor);
        parameters.add("cursor", encoded);
        assertThat(mapper.toQuery(parameters).cursor()).contains(cursor);
        parameters.set("query", "sinte\u0301tica");
        assertThat(mapper.toQuery(parameters).cursor()).contains(cursor);
        parameters.set("query", "otra");
        assertThatThrownBy(() -> mapper.toQuery(parameters)).isInstanceOf(InvalidSearchParameter.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "=", "not.a.cursor", "a", "e30", "bnVsbA", "W10", "e30=", "_w"})
    void malformedCursorHasOnlyASafeFailure(String input) {
        assertThatThrownBy(() -> codec.decode(input)).isInstanceOf(InvalidSearchParameter.class)
                .hasMessage("Invalid catalog search parameter").hasNoCause();
    }

    @Test
    void rejectsVersionFieldsTypesUuidAndNormalizedOrderKeyTampering() {
        String payload = new String(Base64.getUrlDecoder().decode(codec.encode(InstitutionSearchCursor.after(
                new CatalogImportId(UUID.randomUUID()),
                new SearchPublishedInstitutionsQuery(Optional.empty(), Optional.empty(), 1).queryFingerprint(),
                "universidad", new AcademicInstitutionId(UUID.randomUUID())))), StandardCharsets.UTF_8);
        for (String invalid : new String[]{payload.replace("\"version\":1", "\"version\":2"),
                payload.replace("\"version\":1", "\"version\":4294967297"),
                payload.replace("\"version\":1", "\"version\":\"1\""),
                payload.replace("universidad", "Universidad"), payload.replace("universidad", ""),
                payload.replace("universidad", "\\uD800"),
                payload.replace("\"snapshotId\":\"", "\"snapshotId\":\"bad"),
                payload.replace("\"lastInstitutionId\":\"", "\"lastInstitutionId\":\"bad"),
                payload.replace("\"queryFingerprint\":\"", "\"queryFingerprint\":\"bad"),
                payload.replace("\"version\":1", "\"extra\":true,\"version\":1"),
                payload.replace("\"version\":1", "\"version\":2,\"version\":1"), payload + "null"}) {
            assertThatThrownBy(() -> codec.decode(Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(invalid.getBytes(StandardCharsets.UTF_8))))
                    .isInstanceOf(InvalidSearchParameter.class).hasNoCause();
        }
        assertThatThrownBy(() -> codec.decode("a".repeat(4097))).isInstanceOf(InvalidSearchParameter.class);
    }
}
