-- Run as database owner after 202611250004_application_decision_notifications.sql.
begin;
do $$
declare
    applicant uuid := gen_random_uuid();
    stranger uuid := gen_random_uuid();
    admin_id uuid := gen_random_uuid();
    org_id text := 'notification-test-' || gen_random_uuid()::text;
    application integer;
    registration integer;
    decision_id integer;
begin
    insert into auth.users (id, email, email_confirmed_at) values
        (applicant, applicant::text || '@s.ubaguio.edu', now()),
        (stranger, stranger::text || '@s.ubaguio.edu', now()),
        (admin_id, admin_id::text || '@s.ubaguio.edu', now());
    insert into public.stagecall_admins (user_id) values (admin_id);
    insert into public.organizations (id, name, category, description, short_tagline, about_headline, full_description)
        values (org_id, 'Notification test', 'Academic', 'Test', 'Test', 'Test', 'Test');
    foreach application in array array[1, 2] loop
        insert into public.membership_applications
            (user_id, organization_id, applicant_name, applicant_email, student_number, year_level, reason)
        values (case when application = 1 then applicant else stranger end,
            org_id, 'Applicant', 'test@s.ubaguio.edu', '12345678', '1st Year', 'Join')
        returning id into application;
        update public.membership_applications
            set status = case when user_id = applicant then 'Accepted' else 'Rejected' end,
                decision = 'Decision note for applicant'
            where id = application;
        if not exists (select 1 from public.organization_notifications
            where application_id = application and kind = 'application_decision'
                and message = 'Decision note for applicant') then
            raise exception 'Decision notification or note missing';
        end if;
        -- Saving the same status must not create another alert.
        update public.membership_applications set status = status where id = application;
    end loop;
    select n.id into decision_id from public.organization_notifications n
        join public.membership_applications a on a.id = n.application_id
        where a.user_id = applicant and a.organization_id = org_id and n.kind = 'application_decision';
    insert into public.organization_registrations (applicant_id, applicant_email, name, category, tagline, description)
        values (applicant, 'test@s.ubaguio.edu', org_id, 'Academic', 'Test', 'Test') returning id into registration;
    perform set_config('request.jwt.claim.sub', applicant::text, true);
    set local role authenticated;
    if not exists (select 1 from public.organization_notifications where id = decision_id) then
        raise exception 'Applicant cannot see their decision';
    end if;
    if exists (select 1 from public.organization_notifications where registration_id = registration and kind = 'registration_review') then
        raise exception 'Applicant can see admin registration alert';
    end if;
    perform public.mark_organization_notifications_read(decision_id);
    reset role;
    perform set_config('request.jwt.claim.sub', stranger::text, true);
    set local role authenticated;
    if exists (select 1 from public.organization_notifications where id = decision_id) then
        raise exception 'Another applicant can see private decision';
    end if;
    reset role;
    perform set_config('request.jwt.claim.sub', admin_id::text, true);
    set local role authenticated;
    if not exists (select 1 from public.organization_notifications where registration_id = registration and kind = 'registration_review') then
        raise exception 'Admin registration alert missing';
    end if;
    if exists (select 1 from public.organization_notifications where id = decision_id) then
        raise exception 'Admin can see another applicant decision';
    end if;
    reset role;
end;
$$;
rollback;
