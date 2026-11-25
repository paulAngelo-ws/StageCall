-- Run as the database owner after the admin management migration.
begin;
do $$
declare
    v_user uuid := gen_random_uuid();
    v_id text;
    v_data jsonb := jsonb_build_object(
        'name', 'Admin test ' || gen_random_uuid()::text,
        'category', 'Academic', 'description', 'Summary', 'short_tagline', 'Tagline',
        'about_headline', 'About us', 'full_description', 'Full description',
        'banner_color', 'blue', 'is_recruiting', false, 'requirements', '[]'::jsonb);
begin
    insert into auth.users (id, email, email_confirmed_at)
    values (v_user, 'admin-test-' || v_user::text || '@s.ubaguio.edu', now());
    perform set_config('request.jwt.claim.sub', v_user::text, true);
    perform set_config('request.jwt.claims', jsonb_build_object('sub', v_user)::text, true);
    begin
        perform public.stagecall_admin_save_organization(null, v_data);
        raise exception 'Non-admin create was allowed';
    exception when insufficient_privilege then null;
    end;
    begin
        perform public.stagecall_admin_delete_organization('missing', 'Test deletion');
        raise exception 'Non-admin delete was allowed';
    exception when insufficient_privilege then null;
    end;

    insert into public.stagecall_admins (user_id) values (v_user);
    v_id := public.stagecall_admin_save_organization(null, v_data);
    if not exists (select 1 from public.organizations where id = v_id and name = v_data->>'name') then
        raise exception 'Create failed';
    end if;
    begin
        perform public.stagecall_admin_save_organization(null, v_data);
        raise exception 'Duplicate name was allowed';
    exception when unique_violation then null;
    end;
    begin
        perform public.stagecall_admin_save_organization(v_id, v_data || '{"name":" "}'::jsonb);
        raise exception 'Invalid name was allowed';
    exception when invalid_parameter_value then null;
    end;
    insert into public.organization_memberships (organization_id, user_id, role) values (v_id, v_user, 'President');
    perform public.stagecall_admin_save_organization(v_id, v_data || '{"short_tagline":"Edited","is_recruiting":true,"member_count":999}'::jsonb);
    if not exists (select 1 from public.organizations where id = v_id and short_tagline = 'Edited' and is_recruiting and member_count = 1) then
        raise exception 'Update failed or allowed a derived count override';
    end if;
    insert into public.membership_applications
        (user_id, organization_id, applicant_name, applicant_email, student_number, year_level, reason)
    values (v_user, v_id, 'Test', 'test@s.ubaguio.edu', '12345678', '1st Year', 'Test application');
    insert into public.campus_posts (organization_id, title, message, category)
    values (v_id, 'Test', 'Test post', 'Announcements');
    insert into public.organization_registrations (applicant_id, applicant_email, name, category, tagline, description, status, organization_id)
    values (v_user, 'test@s.ubaguio.edu', v_data->>'name', 'Academic', 'Test', 'Test', 'Accepted', v_id);

    -- Revocation must immediately prevent edits, even with an open client screen.
    delete from public.stagecall_admins where user_id = v_user;
    begin
        perform public.stagecall_admin_save_organization(v_id, v_data);
        raise exception 'Revoked admin update was allowed';
    exception when insufficient_privilege then null;
    end;
    insert into public.stagecall_admins (user_id) values (v_user);
    perform public.stagecall_admin_delete_organization(v_id, 'Test deletion');
    if exists (select 1 from public.organizations where id = v_id)
       or exists (select 1 from public.membership_applications where organization_id = v_id)
       or exists (select 1 from public.organization_memberships where organization_id = v_id)
       or exists (select 1 from public.campus_posts where organization_id = v_id)
       or exists (select 1 from public.organization_notifications where organization_id = v_id) then
        raise exception 'Delete left dependent records';
    end if;
    if not exists (select 1 from public.organization_registrations where applicant_id = v_user and organization_id is null) then
        raise exception 'Registration history was not preserved';
    end if;
end;
$$;
rollback;
