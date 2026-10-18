package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.adapter.in.commandline.CatalogImportCommandLineAdapter;
import com.propractix.academicinstitution.adapter.in.commandline.CatalogImportCommandLineAdapter.Operation;
import com.propractix.academicinstitution.adapter.out.artifact.StrictCsvCatalogArtifactParser;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import java.io.IOException;
import java.util.Optional;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

public final class CatalogImportCommandLineApplication {

    private CatalogImportCommandLineApplication() {}

    static void run(Operation operation) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        System.setProperty("spring.devtools.add-properties", "false");
        System.setProperty("debug", "false");
        System.setProperty("logging.config", "classpath:logback-catalog-import.xml");
        System.setProperty("logging.level.root", "OFF");
        int exitCode = operation == Operation.DRY_RUN
                ? executeDryRun(operation)
                : executePublication(operation);
        System.exit(exitCode);
    }

    private static int executeDryRun(Operation operation) {
        try {
            CatalogImportProperties properties = loadImportProperties();
            CatalogArtifactParser parser = new StrictCsvCatalogArtifactParser(
                    properties.allowedSourceSystems(), properties.allowedCountries());
            return new CatalogImportCommandLineAdapter(properties.toDefinition(), parser, Optional.empty())
                    .execute(operation, System.out);
        } catch (IOException | RuntimeException exception) {
            System.out.println("catalog_import_result code=catalog_import_configuration_invalid");
            return 3;
        }
    }

    private static int executePublication(Operation operation) {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(CatalogImportPublicationConfiguration.class)
                .web(WebApplicationType.NONE)
                .bannerMode(org.springframework.boot.Banner.Mode.OFF)
                .logStartupInfo(false)
                .run()) {
            CatalogImportProperties properties = context.getBean(CatalogImportProperties.class);
            CatalogArtifactParser parser = context.getBean(CatalogArtifactParser.class);
            PublishInstitutionCatalog publisher = context.getBean(PublishInstitutionCatalog.class);
            return new CatalogImportCommandLineAdapter(properties.toDefinition(), parser, Optional.of(publisher))
                    .execute(operation, System.out);
        } catch (RuntimeException exception) {
            System.out.println("catalog_import_result code=catalog_publication_failed");
            return 6;
        }
    }

    private static CatalogImportProperties loadImportProperties() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length != 1) {
            throw new IllegalStateException("catalog import requires exactly one active profile");
        }
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        loader.load("catalog-import-profile", new ClassPathResource("application-" + profiles[0] + ".yaml"))
                .forEach(environment.getPropertySources()::addLast);
        loader.load("catalog-import-base", new ClassPathResource("application.yaml"))
                .forEach(environment.getPropertySources()::addLast);
        return Binder.get(environment)
                .bind(
                        "propractix.academic-institution.catalog-import",
                        Bindable.of(CatalogImportProperties.class))
                .orElseThrow(() -> new IllegalStateException("catalog import configuration is absent"));
    }
}
