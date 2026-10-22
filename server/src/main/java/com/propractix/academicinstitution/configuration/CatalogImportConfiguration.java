package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.adapter.out.artifact.StrictCsvCatalogArtifactParser;
import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(CatalogImportProperties.class)
public class CatalogImportConfiguration {

    @Bean
    CatalogArtifactParser catalogArtifactParser(CatalogImportProperties properties) {
        return new StrictCsvCatalogArtifactParser(
                properties.allowedSourceSystems(), properties.allowedCountries());
    }
}
