package com.propractix.academicinstitution.application.port.out;

import com.propractix.academicinstitution.domain.model.CatalogImportId;

@FunctionalInterface
public interface CatalogImportIdGenerator {

    CatalogImportId nextId();
}
