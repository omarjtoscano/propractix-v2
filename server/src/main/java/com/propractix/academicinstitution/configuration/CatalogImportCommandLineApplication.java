package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.adapter.in.commandline.CatalogImportCommandLineAdapter;
import com.propractix.academicinstitution.adapter.in.commandline.CatalogImportCommandLineAdapter.Operation;
import com.propractix.academicinstitution.application.port.in.PublishInstitutionCatalog;
import com.propractix.academicinstitution.application.port.out.CatalogArtifactParser;
import java.io.PrintStream;
import java.util.Optional;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public final class CatalogImportCommandLineApplication {

    private CatalogImportCommandLineApplication() {}

    static void run(Operation operation, String[] args) {
        System.setProperty("spring.devtools.restart.enabled", "false");
        System.setProperty("spring.devtools.add-properties", "false");
        System.setProperty("debug", "false");
        System.setProperty("logging.config", "classpath:logback-catalog-import.xml");
        System.setProperty("logging.level.root", "OFF");
        System.exit(execute(operation, System.out, args));
    }

    static int execute(Operation operation, PrintStream output, String... args) {
        try (ConfigurableApplicationContext context = startContext(operation, args)) {
            CatalogImportProperties properties = context.getBean(CatalogImportProperties.class);
            CatalogArtifactParser parser = context.getBean(CatalogArtifactParser.class);
            Optional<PublishInstitutionCatalog> publisher = operation == Operation.PUBLISH
                    ? Optional.of(context.getBean(PublishInstitutionCatalog.class))
                    : Optional.empty();
            return new CatalogImportCommandLineAdapter(properties.toDefinition(), parser, publisher)
                    .execute(operation, output);
        } catch (RuntimeException exception) {
            output.println(operation == Operation.DRY_RUN
                    ? "catalog_import_result code=catalog_import_configuration_invalid"
                    : "catalog_import_result code=catalog_publication_failed");
            return operation == Operation.DRY_RUN ? 3 : 6;
        }
    }

    static ConfigurableApplicationContext startContext(Operation operation, String... args) {
        Class<?> configuration = operation == Operation.DRY_RUN
                ? CatalogImportConfiguration.class
                : CatalogImportPublicationConfiguration.class;
        return new SpringApplicationBuilder(configuration)
                .web(WebApplicationType.NONE)
                // This isolated administrative process has no server readiness contributors.
                .properties("management.endpoint.health.access=none")
                .bannerMode(org.springframework.boot.Banner.Mode.OFF)
                .logStartupInfo(false)
                .run(args);
    }
}
