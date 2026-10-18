CREATE TABLE academicinstitution_catalog_import (
    catalog_import_id uuid PRIMARY KEY,
    schema_version varchar(100) NOT NULL,
    source_url text NOT NULL,
    retrieved_at timestamptz NOT NULL,
    source_attribution text NOT NULL,
    artifact_hash char(64) NOT NULL,
    source_download_hash char(64),
    status varchar(20) NOT NULL,
    total_row_count integer NOT NULL,
    published_row_count integer NOT NULL,
    omitted_row_count integer NOT NULL,
    failure_code varchar(100),
    created_at timestamptz NOT NULL,
    published_at timestamptz,
    superseded_at timestamptz,
    previous_published_import_id uuid,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_academicinstitution_catalog_import_previous
        FOREIGN KEY (previous_published_import_id)
        REFERENCES academicinstitution_catalog_import (catalog_import_id),
    CONSTRAINT ck_academicinstitution_catalog_import_uuid_v4
        CHECK (
            substring(catalog_import_id::text FROM 15 FOR 1) = '4'
            AND substring(catalog_import_id::text FROM 20 FOR 1) IN ('8', '9', 'a', 'b')
        ),
    CONSTRAINT ck_academicinstitution_catalog_import_schema_version
        CHECK (btrim(schema_version) <> ''),
    CONSTRAINT ck_academicinstitution_catalog_import_source_url
        CHECK (btrim(source_url) <> ''),
    CONSTRAINT ck_academicinstitution_catalog_import_source_attribution
        CHECK (btrim(source_attribution) <> ''),
    CONSTRAINT ck_academicinstitution_catalog_import_artifact_hash
        CHECK (artifact_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_academicinstitution_catalog_import_source_download_hash
        CHECK (source_download_hash IS NULL OR source_download_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_academicinstitution_catalog_import_status
        CHECK (status IN ('PUBLISHED', 'SUPERSEDED', 'REJECTED')),
    CONSTRAINT ck_academicinstitution_catalog_import_counts
        CHECK (
            total_row_count >= 0
            AND published_row_count >= 0
            AND omitted_row_count >= 0
            AND published_row_count + omitted_row_count <= total_row_count
        ),
    CONSTRAINT ck_academicinstitution_catalog_import_publication_state
        CHECK (
            (
                status = 'PUBLISHED'
                AND published_at IS NOT NULL
                AND superseded_at IS NULL
                AND failure_code IS NULL
                AND published_row_count + omitted_row_count = total_row_count
            )
            OR (
                status = 'SUPERSEDED'
                AND published_at IS NOT NULL
                AND superseded_at IS NOT NULL
                AND superseded_at >= published_at
                AND failure_code IS NULL
                AND published_row_count + omitted_row_count = total_row_count
            )
            OR (
                status = 'REJECTED'
                AND published_at IS NULL
                AND superseded_at IS NULL
                AND failure_code IS NOT NULL
                AND published_row_count = 0
            )
        ),
    CONSTRAINT ck_academicinstitution_catalog_import_previous_not_self
        CHECK (previous_published_import_id IS NULL OR previous_published_import_id <> catalog_import_id),
    CONSTRAINT ck_academicinstitution_catalog_import_version
        CHECK (version >= 0)
);

CREATE UNIQUE INDEX uq_academicinstitution_catalog_import_published
    ON academicinstitution_catalog_import (status)
    WHERE status = 'PUBLISHED';

CREATE INDEX ix_academicinstitution_catalog_import_artifact_hash
    ON academicinstitution_catalog_import (artifact_hash);

CREATE TABLE academicinstitution_institution (
    institution_id uuid PRIMARY KEY,
    source_system varchar(100) NOT NULL,
    source_record_id varchar(200) NOT NULL,
    official_name varchar(300) NOT NULL,
    search_name_normalized varchar(300) NOT NULL,
    country_code char(2) NOT NULL,
    source_status varchar(100),
    published_catalog_import_id uuid,
    created_at timestamptz NOT NULL,
    updated_at timestamptz NOT NULL,
    version bigint NOT NULL DEFAULT 0,
    CONSTRAINT fk_academicinstitution_institution_published_import
        FOREIGN KEY (published_catalog_import_id)
        REFERENCES academicinstitution_catalog_import (catalog_import_id),
    CONSTRAINT uq_academicinstitution_institution_external_reference
        UNIQUE (source_system, source_record_id),
    CONSTRAINT ck_academicinstitution_institution_uuid_v4
        CHECK (
            substring(institution_id::text FROM 15 FOR 1) = '4'
            AND substring(institution_id::text FROM 20 FOR 1) IN ('8', '9', 'a', 'b')
        ),
    CONSTRAINT ck_academicinstitution_institution_source_system
        CHECK (btrim(source_system) <> ''),
    CONSTRAINT ck_academicinstitution_institution_source_record_id
        CHECK (btrim(source_record_id) <> ''),
    CONSTRAINT ck_academicinstitution_institution_official_name
        CHECK (btrim(official_name) <> ''),
    CONSTRAINT ck_academicinstitution_institution_search_name
        CHECK (btrim(search_name_normalized) <> ''),
    CONSTRAINT ck_academicinstitution_institution_country_code
        CHECK (country_code ~ '^[A-Z]{2}$'),
    CONSTRAINT ck_academicinstitution_institution_source_status
        CHECK (source_status IS NULL OR btrim(source_status) <> ''),
    CONSTRAINT ck_academicinstitution_institution_timestamps
        CHECK (updated_at >= created_at),
    CONSTRAINT ck_academicinstitution_institution_version
        CHECK (version >= 0)
);

CREATE INDEX ix_academicinstitution_institution_published_search
    ON academicinstitution_institution (
        published_catalog_import_id,
        search_name_normalized,
        institution_id
    );
