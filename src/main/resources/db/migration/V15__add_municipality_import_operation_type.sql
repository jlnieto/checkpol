alter table municipality_import_records
    add column operation_type varchar(20) not null default 'IMPORT';
