alter table generated_communications
    add column ses_problem_reviewed_at timestamp with time zone,
    add column ses_problem_reviewed_by varchar(120);

create index idx_generated_communications_ses_problem_review
    on generated_communications(dispatch_status, ses_problem_reviewed_at, generated_at desc);
