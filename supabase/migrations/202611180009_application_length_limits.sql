-- Limit new submissions while allowing historical applications to be reviewed.
begin;
-- The database checks submissions from ApplicationStore.kt even if a client skips the form’s validation.
create or replace function public.check_application_text_limits()
returns trigger language plpgsql set search_path = '' as $$
begin
    if char_length(new.student_number) > 50 or char_length(new.reason) > 1000 then
        raise exception 'Student ID or reason exceeds the character limit' using errcode = '23514';
    end if;
    return new;
end;
$$;
drop trigger if exists application_text_limits on public.membership_applications;
create trigger application_text_limits before insert or update of student_number, reason
    on public.membership_applications for each row execute function public.check_application_text_limits();
commit;
