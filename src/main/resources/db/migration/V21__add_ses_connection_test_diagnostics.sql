alter table app_users
    add column ses_connection_test_endpoint varchar(300),
    add column ses_connection_test_http_status integer,
    add column ses_connection_test_error_type varchar(80),
    add column ses_connection_test_raw_detail text;
