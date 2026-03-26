alter table guests
    add column municipality_resolved_name varchar(80);

alter table guests
    add column municipality_resolution_status varchar(30) not null default 'EXACT';

alter table guests
    add column municipality_resolution_note varchar(255);

create table municipality_resolution_rules (
    id bigserial primary key,
    country_code varchar(3) not null,
    postal_code_prefix varchar(2),
    municipality_query_normalized varchar(120) not null,
    municipality_query_label varchar(120) not null,
    municipality_code varchar(5) not null,
    municipality_name varchar(80) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_municipality_resolution_rules_lookup
    on municipality_resolution_rules (country_code, postal_code_prefix, municipality_query_normalized);

create table municipality_resolution_issues (
    id bigserial primary key,
    guest_id bigint not null references guests(id),
    country_code varchar(3) not null,
    postal_code varchar(12) not null,
    postal_code_prefix varchar(2),
    municipality_query_normalized varchar(120) not null,
    municipality_query_label varchar(120) not null,
    assigned_municipality_code varchar(5) not null,
    assigned_municipality_name varchar(80),
    resolution_status varchar(30) not null,
    resolution_note varchar(255),
    issue_status varchar(20) not null,
    created_at timestamp with time zone not null,
    resolved_at timestamp with time zone
);

create index idx_municipality_resolution_issues_status
    on municipality_resolution_issues (issue_status, created_at desc);
