create table pending_signups (
    id bigserial primary key,
    email varchar(160) not null,
    password_hash varchar(255) not null,
    accommodation_quantity integer not null,
    status varchar(30) not null,
    token varchar(36) not null,
    stripe_customer_id varchar(120),
    stripe_checkout_session_id varchar(120),
    checkout_client_secret text,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table pending_signups
    add constraint uk_pending_signups_token unique (token);

alter table pending_signups
    add constraint uk_pending_signups_checkout_session unique (stripe_checkout_session_id);

create index idx_pending_signups_email_status on pending_signups (email, status);

create table billing_accounts (
    id bigserial primary key,
    owner_user_id bigint not null references app_users (id),
    stripe_customer_id varchar(120) not null,
    stripe_subscription_id varchar(120),
    stripe_subscription_item_id varchar(120),
    status varchar(30) not null,
    paid_accommodation_limit integer not null,
    current_period_start timestamp with time zone,
    current_period_end timestamp with time zone,
    cancel_at_period_end boolean not null default false,
    customer_country varchar(2),
    customer_type varchar(30) not null,
    tax_mode varchar(30) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table billing_accounts
    add constraint uk_billing_accounts_owner_user unique (owner_user_id);

alter table billing_accounts
    add constraint uk_billing_accounts_customer unique (stripe_customer_id);

alter table billing_accounts
    add constraint uk_billing_accounts_subscription unique (stripe_subscription_id);

create index idx_billing_accounts_status on billing_accounts (status);

create table billing_invoices (
    id bigserial primary key,
    billing_account_id bigint not null references billing_accounts (id),
    stripe_invoice_id varchar(120) not null,
    stripe_invoice_number varchar(120),
    status varchar(40),
    total_amount bigint,
    currency varchar(10),
    tax_amount bigint,
    tax_country varchar(2),
    tax_behavior varchar(40),
    hosted_invoice_url text,
    invoice_pdf_url text,
    period_start timestamp with time zone,
    period_end timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

alter table billing_invoices
    add constraint uk_billing_invoices_stripe_invoice unique (stripe_invoice_id);

create index idx_billing_invoices_account_created on billing_invoices (billing_account_id, created_at desc);

create table stripe_event_logs (
    id bigserial primary key,
    stripe_event_id varchar(120) not null,
    event_type varchar(120) not null,
    processing_status varchar(30) not null,
    processed_at timestamp with time zone,
    error_message text,
    payload text not null,
    created_at timestamp with time zone not null
);

alter table stripe_event_logs
    add constraint uk_stripe_event_logs_event unique (stripe_event_id);

create index idx_stripe_event_logs_status on stripe_event_logs (processing_status, created_at desc);
