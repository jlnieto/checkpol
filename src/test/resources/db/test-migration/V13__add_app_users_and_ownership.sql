create table app_users (
    id bigserial primary key,
    username varchar(80) not null,
    password_hash varchar(255) not null,
    display_name varchar(120) not null,
    role varchar(20) not null,
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table app_users
    add constraint uk_app_users_username unique (username);

create index idx_app_users_role on app_users (role, active);

alter table accommodations
    add column owner_user_id bigint not null references app_users (id);

create index idx_accommodations_owner_user_id on accommodations (owner_user_id, id);

alter table bookings
    add column owner_user_id bigint not null references app_users (id);

create index idx_bookings_owner_user_id on bookings (owner_user_id, id);
