update generated_communications
set dispatch_status = 'SES_CANCELLED',
    ses_response_code = 0,
    ses_response_description = 'Anulación aceptada por SES.',
    ses_cancelled_at = coalesce(ses_cancelled_at, now())
where dispatch_status <> 'SES_CANCELLED'
  and ses_cancellation_raw_response is not null
  and ses_cancellation_raw_response like '%anulacionLoteResponse%'
  and ses_cancellation_raw_response like '%<codigo>0</codigo>%';
