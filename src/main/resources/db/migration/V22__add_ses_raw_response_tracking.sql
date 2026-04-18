alter table generated_communications
    add column ses_submission_raw_response text,
    add column ses_status_raw_response text,
    add column ses_cancellation_raw_response text;
