package com.propractix.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

@AnalyzeClasses(packages = "com.propractix", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    private static final Set<String> BUSINESS_CONTEXTS = Set.of(
            "identity", "student", "organization", "academicinstitution", "jurisdiction",
            "recruitment", "formalization", "internship", "learning", "compliance",
            "documents", "notifications", "administration");

    private static final Map<String, List<String>> ALLOWED_CROSS_CONTEXT_PACKAGES = Map.of(
            "organization", List.of(
                    "com.propractix.identity.application.port.in",
                    "com.propractix.identity.application.contract.event.v1",
                    "com.propractix.compliance.application.port.in"),
            "student", List.of(
                    "com.propractix.identity.application.port.in",
                    "com.propractix.identity.application.contract.event.v1",
                    "com.propractix.compliance.application.port.in",
                    "com.propractix.academicinstitution.application.port.in"),
            "identity", List.of("com.propractix.notifications.application.port.in"));

    @ArchTest
    static final ArchRule domain_is_java_only = domainIsJavaOnly();

    @ArchTest
    static final ArchRule application_does_not_depend_on_adapters_or_persistence = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..adapter..", "..configuration..", "jakarta.persistence..",
                    "org.springframework.data..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule spring_is_confined_outside_application_services = noClasses()
            .that().resideInAPackage("..application..")
            .and().resideOutsideOfPackage("..application.service..")
            .should().dependOnClassesThat().resideInAPackage("org.springframework..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule rest_adapters_are_not_consumed_inward = noClasses()
            .that().resideInAnyPackage("..domain..", "..application..")
            .should().dependOnClassesThat().resideInAPackage("..adapter.in.rest..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule jpa_is_confined_to_persistence_adapters = noClasses()
            .that().resideOutsideOfPackage("..adapter.out.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..", "..adapter.out.persistence..");

    @ArchTest
    static final ArchRule platform_does_not_depend_on_business_contexts = noClasses()
            .that().resideInAPackage("com.propractix.platform..")
            .should().dependOnClassesThat().resideInAnyPackage(BUSINESS_CONTEXTS.stream()
                    .map(context -> "com.propractix." + context + "..")
                    .toArray(String[]::new));

    @ArchTest
    static final ArchRule no_global_technical_layers = noClasses()
            .should().resideInAnyPackage(
                    "com.propractix.controller..", "com.propractix.service..",
                    "com.propractix.repository..", "com.propractix.entity..");

    @ArchTest
    static final ArchRule bounded_contexts_are_free_of_cycles = SlicesRuleDefinition.slices()
            .matching("com.propractix.(*)..")
            .should().beFreeOfCycles();

    @Test
    void applicationServicesUseOnlyTheTransactionalSpringException() {
        JavaClasses classes = productionClasses();

        classes.stream()
                .filter(javaClass -> javaClass.getPackageName().contains(".application.service"))
                .flatMap(javaClass -> javaClass.getDirectDependenciesFromSelf().stream())
                .filter(dependency -> dependency.getTargetClass().getPackageName().startsWith("org.springframework"))
                .forEach(dependency -> assertThat(dependency.getTargetClass().getFullName())
                        .isEqualTo("org.springframework.transaction.annotation.Transactional"));
    }

    @Test
    void crossContextDependenciesMatchTheAcceptedContractMatrix() {
        JavaClasses classes = productionClasses();

        classes.stream().forEach(origin -> origin.getDirectDependenciesFromSelf().stream()
                .filter(dependency -> isCrossContext(origin, dependency.getTargetClass()))
                .forEach(dependency -> assertThat(isAllowed(origin, dependency.getTargetClass()))
                        .as("%s must not depend on unregistered contract %s",
                                origin.getFullName(), dependency.getTargetClass().getFullName())
                        .isTrue()));
    }

    static ArchRule domainIsJavaOnly() {
        return noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework..", "jakarta.persistence..", "com.fasterxml.jackson..",
                        "jakarta.servlet..", "software.amazon.awssdk..", "com.amazonaws..",
                        "..adapter..", "..configuration..")
                .allowEmptyShould(true);
    }

    private static JavaClasses productionClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.propractix");
    }

    private static boolean isCrossContext(JavaClass origin, JavaClass target) {
        String originContext = contextOf(origin);
        String targetContext = contextOf(target);
        return originContext != null && targetContext != null && !originContext.equals(targetContext);
    }

    private static boolean isAllowed(JavaClass origin, JavaClass target) {
        return ALLOWED_CROSS_CONTEXT_PACKAGES.getOrDefault(contextOf(origin), List.of()).stream()
                .anyMatch(prefix -> target.getPackageName().equals(prefix)
                        || target.getPackageName().startsWith(prefix + "."));
    }

    private static String contextOf(JavaClass javaClass) {
        String prefix = "com.propractix.";
        if (!javaClass.getPackageName().startsWith(prefix)) {
            return null;
        }
        String candidate = javaClass.getPackageName().substring(prefix.length()).split("\\.")[0];
        return BUSINESS_CONTEXTS.contains(candidate) ? candidate : null;
    }
}
