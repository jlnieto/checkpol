alter table app_users add column ses_connection_test_status varchar(40);
alter table app_users add column ses_connection_tested_at timestamp with time zone;
alter table app_users add column ses_connection_owner_message varchar(300);
alter table app_users add column ses_connection_admin_message varchar(1500);
