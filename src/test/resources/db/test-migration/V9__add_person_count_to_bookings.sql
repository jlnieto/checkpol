alter table bookings add column person_count integer;
update bookings set person_count = 1 where person_count is null;
alter table bookings alter column person_count set not null;
