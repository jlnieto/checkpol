alter table generated_communications add column version integer;
alter table generated_communications add column last_downloaded_at timestamp with time zone;
alter table generated_communications add column download_count integer;

update generated_communications
set version = 1,
    download_count = 0
where version is null;

alter table generated_communications alter column version set not null;
alter table generated_communications alter column download_count set not null;
