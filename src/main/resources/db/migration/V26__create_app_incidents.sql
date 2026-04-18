create table app_incidents (
    id bigserial primary key,
    area varchar(30) not null,
    severity varchar(20) not null,
    status varchar(20) not null,
    message varchar(300) not null,
    technical_detail varchar(2000),
    stack_trace text,
    method varchar(12) not null,
    path varchar(500) not null,
    username varchar(120),
    user_agent varchar(500),
    owner_id bigint,
    booking_id bigint,
    guest_id bigint,
    communication_id bigint,
    created_at timestamp with time zone not null,
    reviewed_at timestamp with time zone,
    resolved_at timestamp with time zone
);

create index idx_app_incidents_status_created_at on app_incidents(status, created_at desc);
create index idx_app_incidents_area_created_at on app_incidents(area, created_at desc);
create index idx_app_incidents_booking_id on app_incidents(booking_id);
