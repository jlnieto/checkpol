create table generated_communications (
    id bigserial primary key,
    booking_id bigint not null references bookings (id),
    generated_at timestamp with time zone not null,
    xml_content text not null
);

create index idx_generated_communications_booking_id on generated_communications (booking_id);
create index idx_generated_communications_generated_at on generated_communications (generated_at);
