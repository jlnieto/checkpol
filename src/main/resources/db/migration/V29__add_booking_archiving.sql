alter table bookings
    add column archived_at timestamp with time zone;

create index idx_bookings_owner_archived_check_in
    on bookings (owner_user_id, archived_at, check_in_date, id);
