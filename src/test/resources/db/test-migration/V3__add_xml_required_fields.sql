alter table accommodations add column ses_establishment_code varchar(10);

alter table bookings add column contract_date date;
alter table bookings add column payment_type varchar(5);
alter table bookings add column payment_date date;
alter table bookings add column payment_method varchar(50);
alter table bookings add column payment_holder varchar(100);
alter table bookings add column card_expiry varchar(7);
