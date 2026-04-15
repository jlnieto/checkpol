create table admin_settings (
    setting_key varchar(120) primary key,
    setting_value text not null,
    updated_by_username varchar(80) not null,
    updated_at timestamp with time zone not null
);
