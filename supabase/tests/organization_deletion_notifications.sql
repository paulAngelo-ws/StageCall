-- Run as database owner after migration 202611250005. All fixtures roll back.
begin;
do $$
declare
    creator uuid := gen_random_uuid();
    admin_id uuid := gen_random_uuid();
    org_id text := 'deletion-test-' || gen_random_uuid()::text;
    alert_id integer;
begin
    insert into auth.users (id, email, email_confirmed_at) values
        (creator, creator::text || '@s.ubaguio.edu', now()),
        (admin_id, admin_id::text || '@s.ubaguio.edu', now());
    insert into public.stagecall_admins (user_id) values (admin_id);
    insert into public.organizations (id, name, category, description, short_tagline, about_headline, full_description)
        values (org_id, 'Deleted club', 'Academic', 'Test', 'Test', 'Test', 'Test');
    insert into public.organization_registrations
        (applicant_id, applicant_email, name, category, tagline, description, status, organization_id)
        values (creator, 'test@s.ubaguio.edu', 'Deleted club', 'Academic', 'Test', 'Test', 'Accepted', org_id);
    perform set_config('request.jwt.claim.sub', creator::text, true);
    begin
        perform public.stagecall_admin_delete_organization(org_id, 'Unauthorized');
        raise exception 'Non-admin deletion succeeded';
    exception when insufficient_privilege then null;
    end;
    perform set_config('request.jwt.claim.sub', admin_id::text, true);
    begin
        perform public.stagecall_admin_delete_organization(org_id, '   ');
        raise exception 'Blank deletion reason accepted';
    exception when invalid_parameter_value then null;
    end;
    perform public.stagecall_admin_delete_organization(org_id, '  No longer meets registration requirements.  ');
    if exists (select 1 from public.organizations where id = org_id) then
        raise exception 'Organization was not deleted';
    end if;
    select id into alert_id from public.organization_notifications
        where recipient_id = creator and kind = 'organization_deleted'
            and title = 'Organization deleted: Deleted club'
            and message = 'No longer meets registration requirements.';
    if alert_id is null then raise exception 'Deletion notice did not survive deletion'; end if;
    set local role authenticated;
    if exists (select 1 from public.organization_notifications where id = alert_id) then
        raise exception 'Admin can read private creator notice';
    end if;
    reset role;
    perform set_config('request.jwt.claim.sub', creator::text, true);
    set local role authenticated;
    if not exists (select 1 from public.organization_notifications where id = alert_id) then
        raise exception 'Creator cannot read deletion notice';
    end if;
    perform public.mark_organization_notifications_read(alert_id);
    if not exists (select 1 from public.organization_notification_reads where notification_id = alert_id) then
        raise exception 'Read receipt missing';
    end if;
    reset role;
end;
$$;
rollback;
