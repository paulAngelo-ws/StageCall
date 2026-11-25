-- Run as the database owner after 202611250001_employee_email_admins.sql.
-- All fixture accounts and role changes are rolled back.
begin;

do $$
declare
    employee uuid := gen_random_uuid();
    student uuid := gen_random_uuid();
    manual_admin uuid := gen_random_uuid();
    fixture_name text := 'stagecall-test-' || gen_random_uuid()::text;
begin
    insert into auth.users (id, email, email_confirmed_at) values
        (employee, fixture_name || '@E.UBAGUIO.EDU', null),
        (student, fixture_name || '@s.ubaguio.edu', now()),
        (manual_admin, fixture_name || '-manual@s.ubaguio.edu', now());

    if exists (select 1 from public.stagecall_admins where user_id in (employee, student)) then
        raise exception 'Unverified employee or student unexpectedly granted admin';
    end if;

    update auth.users set email_confirmed_at = now() where id = employee;
    if not exists (select 1 from public.stagecall_admins where user_id = employee and granted_by_employee_email) then
        raise exception 'Verified employee did not receive admin';
    end if;

    update auth.users set email = fixture_name || '-changed@s.ubaguio.edu' where id = employee;
    if exists (select 1 from public.stagecall_admins where user_id = employee) then
        raise exception 'Automatic grant survived a change to a student email';
    end if;

    -- A subdomain of the employee domain must not match the exact employee rule.
    update auth.users set email = fixture_name || '@sub.e.ubaguio.edu' where id = employee;
    if exists (select 1 from public.stagecall_admins where user_id = employee) then
        raise exception 'Employee subdomain unexpectedly granted admin';
    end if;

    insert into public.stagecall_admins (user_id) values (manual_admin);
    update auth.users set email = fixture_name || '-manual@e.ubaguio.edu' where id = manual_admin;
    update auth.users set email = fixture_name || '-manual@s.ubaguio.edu' where id = manual_admin;
    if not exists (select 1 from public.stagecall_admins where user_id = manual_admin and not granted_by_employee_email) then
        raise exception 'Manual admin grant was modified or removed';
    end if;

    -- A verified account created directly (e.g. Google) receives access immediately.
    delete from auth.users where id = employee;
    insert into auth.users (id, email, email_confirmed_at)
    values (employee, fixture_name || '@e.ubaguio.edu', now());
    if not exists (select 1 from public.stagecall_admins where user_id = employee and granted_by_employee_email) then
        raise exception 'Verified new employee did not receive admin';
    end if;
end;
$$;

rollback;
