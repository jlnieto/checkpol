create table addresses (
    id bigserial primary key,
    booking_id bigint not null references bookings (id),
    address_line varchar(120) not null,
    address_complement varchar(120),
    municipality_code varchar(5),
    municipality_name varchar(80),
    municipality_resolved_name varchar(80),
    municipality_resolution_status varchar(30) not null,
    municipality_resolution_note varchar(255),
    postal_code varchar(12) not null,
    country varchar(3) not null
);

create index idx_addresses_booking_id on addresses (booking_id, id);

insert into addresses (
    booking_id,
    address_line,
    address_complement,
    municipality_code,
    municipality_name,
    municipality_resolved_name,
    municipality_resolution_status,
    municipality_resolution_note,
    postal_code,
    country
)
select distinct
    booking_id,
    address_line,
    address_complement,
    municipality_code,
    municipality_name,
    municipality_resolved_name,
    municipality_resolution_status,
    municipality_resolution_note,
    postal_code,
    country
from guests;

alter table guests add column address_id bigint;

update guests g
set address_id = (
    select a.id
    from addresses a
    where a.booking_id = g.booking_id
      and a.address_line = g.address_line
      and a.address_complement is not distinct from g.address_complement
      and a.municipality_code is not distinct from g.municipality_code
      and a.municipality_name is not distinct from g.municipality_name
      and a.municipality_resolved_name is not distinct from g.municipality_resolved_name
      and a.municipality_resolution_status = g.municipality_resolution_status
      and a.municipality_resolution_note is not distinct from g.municipality_resolution_note
      and a.postal_code = g.postal_code
      and a.country = g.country
    fetch first 1 row only
);

alter table guests alter column address_id set not null;
alter table guests add constraint fk_guests_address_id foreign key (address_id) references addresses (id);
create index idx_guests_address_id on guests (address_id);

alter table guests drop column address_line;
alter table guests drop column address_complement;
alter table guests drop column municipality_code;
alter table guests drop column municipality_name;
alter table guests drop column municipality_resolved_name;
alter table guests drop column municipality_resolution_status;
alter table guests drop column municipality_resolution_note;
alter table guests drop column postal_code;
alter table guests drop column country;
