package com.propractix;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import org.springframework.test.web.servlet.MvcResult;
import org.yaml.snakeyaml.Yaml;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Assertions for the response-schema vocabulary used by this design-first contract. */
final class AcademicInstitutionOpenApiContract {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final JsonNode CONTRACT = load();
    static final JsonNode OPERATION = CONTRACT.at("/paths/~1api~1v1~1academic-institutions/get");

    private AcademicInstitutionOpenApiContract() {}

    static void assertResponse(MvcResult result) {
        var response = result.getResponse();
        var spec = OPERATION.path("responses").path(Integer.toString(response.getStatus()));
        assertThat(spec.isMissingNode()).isFalse();
        assertThat(response.getHeader("X-Correlation-ID")).matches(
                CONTRACT.at("/components/schemas/CorrelationId/pattern").asText());
        assertThat(spec.path("headers").has("X-Correlation-ID")).isTrue();
        String media = response.getContentType();
        var schema = spec.path("content").path(media).path("schema");
        assertThat(schema.isMissingNode()).as("documented media type %s", media).isFalse();
        var body = JSON.readTree(response.getContentAsByteArray());
        assertValue(body, schema);
        if (response.getStatus() >= 400) {
            assertThat(body.path("correlationId").asText()).isEqualTo(response.getHeader("X-Correlation-ID"));
            assertThat(body.path("instance").asText()).isEqualTo("/api/v1/academic-institutions");
            assertThat(body.path("type").asText()).isEqualTo("urn:propractix:problem:" + body.path("code").asText());
            if (response.getStatus() == 429 || body.path("code").asText().equals("rate_limiter_unavailable")) {
                assertThat(response.getHeader("Retry-After")).matches("[1-9][0-9]*");
                if (response.getStatus() == 503) assertThat(response.getHeader("Retry-After")).isEqualTo("1");
            } else {
                assertThat(response.getHeader("Retry-After")).isNull();
            }
        }
    }

    static JsonNode paths() { return CONTRACT.path("paths"); }

    private static void assertValue(JsonNode value, JsonNode schema) {
        if (schema.has("$ref")) {
            assertValue(value, CONTRACT.at(schema.path("$ref").asText().substring(1)));
            return;
        }
        for (JsonNode part : schema.path("allOf")) assertValue(value, part);
        if (schema.has("const")) assertThat(value).isEqualTo(schema.path("const"));
        if (schema.has("enum")) assertThat(schema.path("enum")).anySatisfy(item -> assertThat(value).isEqualTo(item));
        if (schema.has("type")) {
            var type = schema.path("type");
            if (type.isArray()) {
                assertThat(type).anySatisfy(item -> assertType(value, item.asText()));
            } else {
                assertType(value, type.asText());
            }
        }
        if (value.isNull()) return;
        if (value.isObject()) {
            for (JsonNode required : schema.path("required")) assertThat(value.has(required.asText())).isTrue();
            var properties = schema.path("properties");
            if (schema.path("additionalProperties").isBoolean() && !schema.path("additionalProperties").asBoolean()) {
                assertThat(value.propertyNames()).isSubsetOf(properties.propertyNames());
            }
            for (String name : value.propertyNames()) {
                if (properties.has(name)) assertValue(value.path(name), properties.path(name));
            }
        }
        if (value.isArray()) {
            if (schema.has("maxItems")) assertThat(value.size()).isLessThanOrEqualTo(schema.path("maxItems").asInt());
            for (JsonNode item : value) assertValue(item, schema.path("items"));
        }
        if (value.isString()) {
            String text = value.asText();
            int length = text.codePointCount(0, text.length());
            if (schema.has("minLength")) assertThat(length).isGreaterThanOrEqualTo(schema.path("minLength").asInt());
            if (schema.has("maxLength")) assertThat(length).isLessThanOrEqualTo(schema.path("maxLength").asInt());
            if (schema.has("pattern")) assertThat(text).matches(schema.path("pattern").asText());
            switch (schema.path("format").asText()) {
                case "uuid" -> assertThat(UUID.fromString(text).version()).isEqualTo(4);
                case "date-time" -> Instant.parse(text);
                case "uri" -> assertThat(URI.create(text).isAbsolute()).isTrue();
                case "uri-reference" -> URI.create(text);
                default -> { }
            }
        }
        if (value.isNumber()) {
            if (schema.has("minimum")) assertThat(value.asDouble()).isGreaterThanOrEqualTo(schema.path("minimum").asDouble());
            if (schema.has("maximum")) assertThat(value.asDouble()).isLessThanOrEqualTo(schema.path("maximum").asDouble());
        }
    }

    private static void assertType(JsonNode value, String type) {
        boolean matches = switch (type) {
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isString();
            case "integer" -> value.isIntegralNumber();
            case "null" -> value.isNull();
            default -> throw new AssertionError("Unsupported contract type: " + type);
        };
        assertThat(matches).as("JSON schema type %s", type).isTrue();
    }

    private static JsonNode load() {
        try (var input = Files.newInputStream(Path.of("../docs/api/openapi.yaml"))) {
            return JSON.valueToTree(new Yaml().load(input));
        } catch (java.io.IOException failure) {
            throw new ExceptionInInitializerError(failure);
        }
    }
}
