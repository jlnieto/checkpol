alter table guests add column submission_source varchar(20);
alter table guests add column review_status varchar(20);
alter table guests add column submitted_at timestamp with time zone;

update guests
set submission_source = 'MANUAL',
    review_status = 'REVIEWED'
where submission_source is null;

alter table guests alter column submission_source set not null;
alter table guests alter column review_status set not null;
