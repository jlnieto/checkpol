create or replace function checkpol_lob_text(value text)
returns text
language plpgsql
as $$
declare
    decoded text;
begin
    if value is null or value !~ '^[0-9]+$' then
        return value;
    end if;

    begin
        decoded := convert_from(lo_get(value::oid), 'UTF8');
        return decoded;
    exception when others then
        return value;
    end;
end;
$$;

update generated_communications
set xml_content = checkpol_lob_text(xml_content),
    ses_submission_raw_response = checkpol_lob_text(ses_submission_raw_response),
    ses_status_raw_response = checkpol_lob_text(ses_status_raw_response),
    ses_cancellation_raw_response = checkpol_lob_text(ses_cancellation_raw_response);

drop function checkpol_lob_text(text);
