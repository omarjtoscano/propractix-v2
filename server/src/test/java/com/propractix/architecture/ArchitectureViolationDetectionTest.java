package com.propractix.architecture;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

class ArchitectureViolationDetectionTest {

    @Test
    void detectsAFrameworkDependencyInsideDomain() {
        var fixture = new ClassFileImporter().importPackages("com.propractix.architecturefixture.domain");

        assertThrows(AssertionError.class, () -> ArchitectureRulesTest.domainIsJavaOnly().check(fixture));
    }
}
