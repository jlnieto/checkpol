alter table app_users
    add column ses_connection_test_status varchar(40),
    add column ses_connection_tested_at timestamp with time zone,
    add column ses_connection_owner_message varchar(300),
    add column ses_connection_admin_message varchar(1500);
