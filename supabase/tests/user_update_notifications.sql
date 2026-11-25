-- Run as owner after 202611250006. All data is rolled back.
begin;
do $$
declare
    member_id uuid := gen_random_uuid();
    stranger_id uuid := gen_random_uuid();
    watcher_id uuid := gen_random_uuid();
    admin_id uuid := gen_random_uuid();
    org text := 'updates-test-' || gen_random_uuid()::text;
    post integer;
    notice integer;
    before_count integer;
begin
    insert into auth.users(id, email, email_confirmed_at) values
        (member_id, member_id::text || '@s.ubaguio.edu', now()),
        (stranger_id, stranger_id::text || '@s.ubaguio.edu', now()),
        (watcher_id, watcher_id::text || '@s.ubaguio.edu', now()),
        (admin_id, admin_id::text || '@s.ubaguio.edu', now());
    insert into public.stagecall_admins(user_id) values (admin_id);
    insert into public.organizations(id, name, category, description, short_tagline, about_headline, full_description)
        values (org, 'Updates club', 'Academic', 'Test', 'Test', 'Test', 'Test');
    insert into public.organization_memberships(organization_id, user_id) values (org, member_id);
    if not exists (select 1 from public.organization_notifications where recipient_id = member_id
        and title = 'Organization membership added') then raise exception 'Missing membership notice'; end if;
    update public.organization_memberships set role = 'President' where organization_id = org and user_id = member_id;
    if not exists (select 1 from public.organization_notifications where recipient_id = member_id
        and title = 'Organization role updated') then raise exception 'Missing role notice'; end if;
    insert into public.user_profiles(user_id, bookmarks) values (watcher_id, array[org])
        on conflict (user_id) do update set bookmarks = excluded.bookmarks;
    update public.user_profiles set bio = 'Updated bio' where user_id = watcher_id;
    select count(*) into before_count from public.organization_notifications;
    update public.user_profiles set bio = bio where user_id = watcher_id;
    update public.organizations set name = name where id = org;
    if (select count(*) from public.organization_notifications) <> before_count then
        raise exception 'Unchanged save sent a notification'; end if;
    update public.organizations set requirements = '["Attend orientation"]'::jsonb where id = org;
    select id into notice from public.organization_notifications where organization_id = org and kind = 'organization';
    if notice is null then raise exception 'Missing requirements notice'; end if;
    perform set_config('request.jwt.claim.sub', stranger_id::text, true);
    set local role authenticated;
    if exists (select 1 from public.organization_notifications where id = notice or recipient_id = member_id) then
        raise exception 'Unrelated user can read updates'; end if;
    reset role;
    perform set_config('request.jwt.claim.sub', watcher_id::text, true);
    set local role authenticated;
    if not exists (select 1 from public.organization_notifications where id = notice) then
        raise exception 'Bookmark follower cannot read organization update'; end if;
    reset role;
    insert into public.campus_posts(organization_id, title, message, category)
        values (org, 'Original', 'Original message', 'Events') returning id into post;
    perform set_config('request.jwt.claim.sub', member_id::text, true);
    perform public.mark_organization_notifications_read(null);
    update public.campus_posts set title = 'Revised', schedule = 'Tomorrow' where id = post;
    if not exists (select 1 from public.organization_notifications n where n.post_id = post and n.kind = 'post_update'
        and not exists (select 1 from public.organization_notification_reads r where r.notification_id = n.id)) then
        raise exception 'Post edit did not produce a new unread notice'; end if;
    update public.campus_posts set members_only = true where id = post;
    select id into notice from public.organization_notifications where post_id = post and kind = 'post_update'
        order by id desc limit 1;
    perform set_config('request.jwt.claim.sub', watcher_id::text, true);
    set local role authenticated;
    if exists (select 1 from public.organization_notifications where id = notice) then
        raise exception 'Nonmember can read private post edit'; end if;
    reset role;
    delete from public.campus_posts where id = post;
    if not exists (select 1 from public.organization_notifications where recipient_id = member_id
        and title = 'Post removed: Revised') then raise exception 'Missing post removal notice'; end if;
    if exists (select 1 from public.organization_notifications where recipient_id = watcher_id
        and title = 'Post removed: Revised') then raise exception 'Private post removal leaked'; end if;
    perform set_config('request.jwt.claim.sub', admin_id::text, true);
    perform public.stagecall_admin_delete_organization(org, 'Organization closed.');
    if not exists (select 1 from public.organization_notifications where recipient_id = member_id
        and kind = 'organization_deleted' and message = 'Organization closed.') then
        raise exception 'Member deletion notice did not survive'; end if;
end;
$$;
rollback;
