package com.propractix.academicinstitution.configuration;

import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@EnableConfigurationProperties(CatalogImportProperties.class)
@ComponentScan(basePackages = "com.propractix.academicinstitution.adapter.out.persistence")
@Import(AcademicInstitutionConfiguration.class)
public class CatalogImportPublicationConfiguration {}
