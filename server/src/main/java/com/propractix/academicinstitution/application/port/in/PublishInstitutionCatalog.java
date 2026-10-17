package com.propractix.academicinstitution.application.port.in;

import com.propractix.academicinstitution.application.command.PublishInstitutionCatalogCommand;

public interface PublishInstitutionCatalog {

    CatalogPublicationResult publish(PublishInstitutionCatalogCommand command);
}
