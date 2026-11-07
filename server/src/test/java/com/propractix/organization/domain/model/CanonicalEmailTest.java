package com.propractix.organization.domain.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CanonicalEmailTest {
    @Test
    void preservesLocalCaseDotsAndPlusWithoutAliases() {
        var email = new EmailAddress("Persona.Qa+tag@empresa.test");
        assertEquals("Persona.Qa+tag@empresa.test", email.value());
        assertEquals(new EmailDomain("empresa.test"), email.domain());
        assertNotEquals(email, new EmailAddress("persona.Qa+tag@empresa.test"));
        assertNotEquals(email, new EmailAddress("PersonaQa+tag@empresa.test"));
        assertNotEquals(email, new EmailAddress("Persona.Qa@empresa.test"));
    }

    @Test
    void acceptsCanonicalAsciiALabelsWithoutClaimingUnicodeEngineConformance() {
        var books = new EmailAddress("persona@xn--bcher-kva.test");
        assertEquals("xn--bcher-kva.test", books.domain().value());
        assertNotEquals(new EmailAddress("persona@xn--fa-hia.test"), new EmailAddress("persona@fass.test"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "sin-arroba", "@empresa.test", "a@", "a@@empresa.test", "a@b@empresa.test",
            " a@empresa.test", "a@empresa.test ", "a b@empresa.test", "a\tb@empresa.test", "a@empresa.test\r\n",
            ".a@empresa.test", "a.@empresa.test", "a..b@empresa.test", "\"a\"@empresa.test", "a(comentario)@empresa.test",
            "á@empresa.test", "persona@bücher.test", "persona@bu\u0308cher.test", "a@EMPRESA.TEST",
            "a@XN--BCHER-KVA.TEST", "a@empresa。test", "a@empresa．test", "a@empresa｡test",
            "a@empresa.test.", "a@.test", "a@empresa..test", "a@-empresa.test", "a@empresa-.test", "a@em_presa.test",
            "a@[127.0.0.1]", "a@127.0.0.1", "a@[IPv6:::1]", "a@ab--cd.test", "a@xn--.test"})
    void rejectsInvalidOrNonCanonicalValuesWithoutRepair(String value) {
        assertThrows(IllegalArgumentException.class, () -> new EmailAddress(value));
    }

    @Test
    void acceptsEveryAsciiDotAtomCharacter() {
        assertDoesNotThrow(() -> new EmailAddress("!#$%&'*+-/=?^_`{|}~.AZaz09@empresa.test"));
    }

    @Test
    void enforcesLocalPartLengthInAsciiOctets() {
        assertDoesNotThrow(() -> new EmailAddress("a".repeat(64) + "@empresa.test"));
        assertThrows(IllegalArgumentException.class, () -> new EmailAddress("a".repeat(65) + "@empresa.test"));
    }

    @Test
    void enforcesLabelAndDomainLengthIndependentlyOfAddressLength() {
        assertDoesNotThrow(() -> new EmailDomain("a".repeat(63) + ".test"));
        assertThrows(IllegalArgumentException.class, () -> new EmailDomain("a".repeat(64) + ".test"));
        String domain253 = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
        assertEquals(253, domain253.length());
        assertDoesNotThrow(() -> new EmailDomain(domain253));
        assertThrows(IllegalArgumentException.class, () -> new EmailDomain(domain253 + "d"));
        assertThrows(IllegalArgumentException.class, () -> new EmailAddress("a@" + domain253));
    }

    @Test
    void enforcesCombinedAddressLimit() {
        String domain = "a".repeat(63) + "." + "b".repeat(63) + "." + "c".repeat(61);
        String maximum = "l".repeat(64) + "@" + domain;
        assertEquals(254, maximum.length());
        assertDoesNotThrow(() -> new EmailAddress(maximum));
        assertThrows(IllegalArgumentException.class, () -> new EmailAddress(maximum + "c"));
        assertDoesNotThrow(() -> new EmailAddress("x@" + "a".repeat(63) + "." + "b".repeat(63)
                + "." + "c".repeat(63) + "." + "d".repeat(60)));
    }

    @Test
    void rejectsNullAndRedactsDiagnostics() {
        assertThrows(NullPointerException.class, () -> new EmailAddress(null));
        assertThrows(NullPointerException.class, () -> new EmailDomain(null));
        assertEquals("EmailAddress[redacted]", new EmailAddress("persona@empresa.test").toString());
        assertEquals("EmailDomain[redacted]", new EmailDomain("empresa.test").toString());
        assertFalse(assertThrows(IllegalArgumentException.class,
                () -> new EmailAddress("sintetico secreto@empresa.test")).getMessage().contains("sintetico"));
    }
}
