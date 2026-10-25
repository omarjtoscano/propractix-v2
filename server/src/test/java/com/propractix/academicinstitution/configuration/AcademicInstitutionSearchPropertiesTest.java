package com.propractix.academicinstitution.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class AcademicInstitutionSearchPropertiesTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner().withUserConfiguration(Binding.class);
    private static final String PREFIX = "propractix.academic-institution.search.";

    @Test
    void bindsConfiguredValuesAndRejectsMissingOutOfRangeInconsistentAndUnknownSettings() {
        runner.withPropertyValues(PREFIX + "default-limit=7", PREFIX + "max-limit=12").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(AcademicInstitutionSearchProperties.class))
                    .isEqualTo(new AcademicInstitutionSearchProperties(7, 12));
        });
        for (String[] values : new String[][]{
                {}, {PREFIX + "default-limit=20"}, {PREFIX + "default-limit=0", PREFIX + "max-limit=50"},
                {PREFIX + "default-limit=20", PREFIX + "max-limit=51"},
                {PREFIX + "default-limit=20", PREFIX + "max-limit=10"},
                {PREFIX + "default-limit=20", PREFIX + "max-limit=50", PREFIX + "offset=0"}}) {
            runner.withPropertyValues(values).run(context -> assertThat(context).hasFailed());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AcademicInstitutionSearchProperties.class)
    static class Binding {}
}
