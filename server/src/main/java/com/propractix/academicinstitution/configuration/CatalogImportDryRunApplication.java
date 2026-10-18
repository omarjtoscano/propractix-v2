package com.propractix.academicinstitution.configuration;

import com.propractix.academicinstitution.adapter.in.commandline.CatalogImportCommandLineAdapter.Operation;

public final class CatalogImportDryRunApplication {

    private CatalogImportDryRunApplication() {}

    public static void main(String[] args) {
        CatalogImportCommandLineApplication.run(Operation.DRY_RUN);
    }
}
