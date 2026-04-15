create table municipality_import_records (
    id bigserial primary key,
    source varchar(40) not null,
    source_version varchar(80) not null,
    municipalities_url varchar(500) not null,
    postal_mappings_url varchar(500) not null,
    status varchar(20) not null,
    imported_municipalities integer not null,
    deactivated_municipalities integer not null,
    imported_postal_mappings integer not null,
    deactivated_postal_mappings integer not null,
    triggered_by_username varchar(80) not null,
    error_message varchar(1000),
    created_at timestamp with time zone not null
);

create index idx_municipality_import_records_created_at
    on municipality_import_records (created_at desc);
