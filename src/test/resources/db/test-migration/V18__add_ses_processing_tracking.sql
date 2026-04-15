alter table generated_communications add column ses_communication_code varchar(36);
alter table generated_communications add column ses_last_status_checked_at timestamp with time zone;
alter table generated_communications add column ses_processing_state_code integer;
alter table generated_communications add column ses_processing_state_description varchar(200);
alter table generated_communications add column ses_processing_error_type varchar(5);
alter table generated_communications add column ses_processing_error_description varchar(200);
