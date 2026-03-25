alter table bookings add column self_service_token varchar(64);
alter table bookings add column self_service_expires_at timestamp with time zone;

create unique index idx_bookings_self_service_token on bookings (self_service_token);
