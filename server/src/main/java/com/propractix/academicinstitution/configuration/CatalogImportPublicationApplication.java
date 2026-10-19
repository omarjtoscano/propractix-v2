package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.adapter.in.commandline.CatalogImportCommandLineAdapter.Operation;

public final class CatalogImportPublicationApplication {

    private CatalogImportPublicationApplication() {}

    public static void main(String[] args) {
        CatalogImportCommandLineApplication.run(Operation.PUBLISH, args);
    }
}
