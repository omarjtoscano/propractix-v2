package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.domain.service.CatalogPublicationPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AcademicInstitutionConfiguration {

    @Bean
    CatalogPublicationPolicy catalogPublicationPolicy() {
        return new CatalogPublicationPolicy();
    }
}
