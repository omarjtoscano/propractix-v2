package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.adapter.in.rest.AcademicInstitutionSearchMapper;
import com.propractix.academicinstitution.adapter.in.rest.InstitutionSearchCursorCodec;
import com.propractix.academicinstitution.adapter.out.observability.MeteredInstitutionSearch;
import com.propractix.academicinstitution.application.port.in.SearchPublishedInstitutions;
import com.propractix.academicinstitution.application.port.out.PublishedInstitutionQueryRepository;
import com.propractix.academicinstitution.application.service.SearchPublishedInstitutionsService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;
import io.micrometer.core.instrument.MeterRegistry;

/** HTTP composition is independent of the isolated administrative import processes. */
@Configuration(proxyBeanMethods = false)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(AcademicInstitutionSearchProperties.class)
public class AcademicInstitutionSearchConfiguration {
    @Bean
    SearchPublishedInstitutions searchPublishedInstitutions(PublishedInstitutionQueryRepository repository,
            MeterRegistry registry) {
        return new MeteredInstitutionSearch(new SearchPublishedInstitutionsService(repository), registry);
    }

    @Bean
    AcademicInstitutionSearchMapper academicInstitutionSearchMapper(
            AcademicInstitutionSearchProperties properties, JsonMapper json) {
        return new AcademicInstitutionSearchMapper(properties.defaultLimit(), properties.maxLimit(),
                new InstitutionSearchCursorCodec(json));
    }
}
