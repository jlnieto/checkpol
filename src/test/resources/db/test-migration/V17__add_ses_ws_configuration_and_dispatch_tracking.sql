alter table app_users add column ses_arrendador_code varchar(10);
alter table app_users add column ses_ws_username varchar(50);
alter table app_users add column ses_ws_password_encrypted varchar(500);

alter table generated_communications add column dispatch_mode varchar(30) not null default 'MANUAL_DOWNLOAD';
alter table generated_communications add column dispatch_status varchar(30) not null default 'XML_READY';
alter table generated_communications add column submitted_at timestamp with time zone;
alter table generated_communications add column ses_lote_code varchar(36);
alter table generated_communications add column ses_response_code integer;
alter table generated_communications add column ses_response_description varchar(200);
