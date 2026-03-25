create table accommodations (
    id bigserial primary key,
    name varchar(120) not null,
    registration_number varchar(40)
);

create table bookings (
    id bigserial primary key,
    accommodation_id bigint not null references accommodations (id),
    reference_code varchar(80) not null,
    channel varchar(20) not null,
    check_in_date date not null,
    check_out_date date not null
);

create index idx_bookings_check_in_date on bookings (check_in_date);
create index idx_bookings_accommodation_id on bookings (accommodation_id);
