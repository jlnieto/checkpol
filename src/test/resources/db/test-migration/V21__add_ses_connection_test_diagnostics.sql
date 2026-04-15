alter table app_users add column ses_connection_test_endpoint varchar(300);
alter table app_users add column ses_connection_test_http_status integer;
alter table app_users add column ses_connection_test_error_type varchar(80);
alter table app_users add column ses_connection_test_raw_detail text;
