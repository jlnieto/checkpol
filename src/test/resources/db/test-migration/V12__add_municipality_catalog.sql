create table municipality_catalog_entries (
    id bigserial primary key,
    country_code varchar(3) not null,
    province_code varchar(2) not null,
    province_name varchar(80) not null,
    municipality_code varchar(5) not null,
    municipality_name varchar(80) not null,
    normalized_municipality_name varchar(120) not null,
    active boolean not null,
    source varchar(40) not null,
    source_version varchar(40) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table municipality_catalog_entries
    add constraint uk_municipality_catalog_entries_code unique (municipality_code);

create index idx_municipality_catalog_entries_name
    on municipality_catalog_entries (normalized_municipality_name);

create index idx_municipality_catalog_entries_province
    on municipality_catalog_entries (province_code);

create table postal_code_municipality_mappings (
    id bigserial primary key,
    postal_code varchar(5) not null,
    municipality_code varchar(5) not null,
    active boolean not null,
    source varchar(40) not null,
    source_version varchar(40) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table postal_code_municipality_mappings
    add constraint uk_postal_code_municipality unique (postal_code, municipality_code);

alter table postal_code_municipality_mappings
    add constraint fk_postal_code_municipality_catalog_code
        foreign key (municipality_code) references municipality_catalog_entries (municipality_code);

create index idx_postal_code_municipality_postal_code
    on postal_code_municipality_mappings (postal_code);

create index idx_postal_code_municipality_code
    on postal_code_municipality_mappings (municipality_code);
