package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.adapter.out.artifact.StrictCsvCatalogArtifactParser;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.port.out.AcademicInstitutionIdGenerator;
import com.propractix.academicinstitution.application.port.out.ApplicationClock;
import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import com.propractix.academicinstitution.application.port.out.CatalogImportIdGenerator;
import com.propractix.academicinstitution.application.port.out.CatalogPublicationRepository;
import com.propractix.academicinstitution.application.service.PublishInstitutionCatalogService;
import com.propractix.academicinstitution.domain.model.AcademicInstitutionId;
import com.propractix.academicinstitution.domain.model.CatalogImportId;
import com.propractix.academicinstitution.domain.service.CatalogPublicationPolicy;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AcademicInstitutionConfiguration {

    @Bean
    CatalogPublicationPolicy catalogPublicationPolicy() {
        return new CatalogPublicationPolicy();
    }

    @Bean
    CatalogArtifactParser catalogArtifactParser(CatalogImportProperties properties) {
        return new StrictCsvCatalogArtifactParser(
                properties.allowedSourceSystems(), properties.allowedCountries());
    }

    @Bean
    ApplicationClock catalogApplicationClock() {
        return Instant::now;
    }

    @Bean
    AcademicInstitutionIdGenerator academicInstitutionIdGenerator() {
        return () -> new AcademicInstitutionId(UUID.randomUUID());
    }

    @Bean
    CatalogImportIdGenerator catalogImportIdGenerator() {
        return () -> new CatalogImportId(UUID.randomUUID());
    }

    @Bean
    PublishInstitutionCatalog publishInstitutionCatalog(
            CatalogArtifactParser parser,
            CatalogPublicationRepository repository,
            ApplicationClock clock,
            AcademicInstitutionIdGenerator institutionIdGenerator,
            CatalogImportIdGenerator importIdGenerator) {
        return new PublishInstitutionCatalogService(
                parser, repository, clock, institutionIdGenerator, importIdGenerator);
    }
}
