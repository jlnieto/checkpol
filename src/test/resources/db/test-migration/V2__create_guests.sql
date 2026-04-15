create table guests (
    id bigserial primary key,
    booking_id bigint not null references bookings (id),
    first_name varchar(50) not null,
    last_name1 varchar(50) not null,
    last_name2 varchar(50),
    document_type varchar(5),
    document_number varchar(15),
    document_support varchar(9),
    birth_date date not null,
    nationality varchar(3),
    sex varchar(1),
    address_line varchar(120) not null,
    address_complement varchar(120),
    municipality_code varchar(5),
    municipality_name varchar(80),
    postal_code varchar(12) not null,
    country varchar(3) not null,
    phone varchar(20),
    phone2 varchar(20),
    email varchar(250),
    relationship varchar(5)
);

create index idx_guests_booking_id on guests (booking_id);
