-- Complete user-relevant inbox events. Apply after 202611250005.
begin;
alter table public.organization_notifications
    drop constraint organization_notifications_kind_check,
    add constraint organization_notifications_kind_check check (kind in (
        'post', 'post_update', 'recruitment', 'organization', 'application_review', 'application_decision',
        'registration_review', 'registration_decision', 'organization_deleted', 'user_update')),
    drop constraint organization_notifications_registration_scope_check,
    add constraint organization_notifications_registration_scope_check check (
        case when kind in ('organization_deleted', 'user_update') then
            recipient_id is not null and organization_id is null and registration_id is null
                and post_id is null and application_id is null
        when kind in ('registration_review', 'registration_decision') then
            registration_id is not null and organization_id is null and post_id is null and application_id is null
                and recipient_id is null
        else registration_id is null and organization_id is not null and recipient_id is null end
    );
create index organization_notifications_recipient_idx
    on public.organization_notifications (recipient_id, created_at desc) where recipient_id is not null;

-- No client can call this helper or write notices on behalf of another user.
create function public.stagecall_notify_user(p_user uuid, p_title text, p_message text)
returns void language sql security definer set search_path = '' as $$
    insert into public.organization_notifications (recipient_id, kind, title, message)
    values (p_user, 'user_update', p_title, p_message);
$$;
revoke all on function public.stagecall_notify_user(uuid, text, text) from public, anon, authenticated;

create function public.stagecall_notify_submission_receipt()
returns trigger language plpgsql security definer set search_path = '' as $$
declare v_name text;
begin
    if tg_table_name = 'membership_applications' then
        select name into v_name from public.organizations where id = new.organization_id;
        perform public.stagecall_notify_user(new.user_id, 'Application submitted',
            'Your application to ' || v_name || ' was submitted for review.');
    else
        perform public.stagecall_notify_user(new.applicant_id, 'Registration submitted',
            'Your registration for ' || new.name || ' was submitted for admin review.');
    end if;
    return new;
end;
$$;
create trigger stagecall_application_receipt after insert on public.membership_applications
    for each row when (new.status = 'Pending') execute function public.stagecall_notify_submission_receipt();
create trigger stagecall_registration_receipt after insert on public.organization_registrations
    for each row when (new.status = 'Pending') execute function public.stagecall_notify_submission_receipt();

create function public.stagecall_notify_membership_change()
returns trigger language plpgsql security definer set search_path = '' as $$
declare
    v_org text;
    v_user uuid;
    v_name text;
    v_title text;
    v_message text;
begin
    if tg_op = 'DELETE' then v_org := old.organization_id; v_user := old.user_id;
    else v_org := new.organization_id; v_user := new.user_id; end if;
    select name into v_name from public.organizations where id = v_org;
    -- Organization cascades have a separate deletion notice with the admin's reason.
    if not found or not exists (select 1 from auth.users where id = v_user) then return null; end if;
    if tg_op = 'UPDATE' then
        if old.role is not distinct from new.role then return null; end if;
        v_title := 'Organization role updated';
        v_message := 'Your role in ' || v_name || ' is now ' || new.role || '.';
    elsif tg_op = 'INSERT' then
        v_title := 'Organization membership added';
        v_message := 'You are now a ' || new.role || ' of ' || v_name || '.';
    else
        v_title := 'Organization membership ended';
        v_message := case when auth.uid() = v_user then 'You left ' else 'You were removed from ' end || v_name || '.';
    end if;
    perform public.stagecall_notify_user(v_user, v_title, v_message);
    insert into public.organization_notifications (recipient_id, kind, title, message)
    select m.user_id, 'user_update', 'Membership updated: ' || v_name,
        case tg_op when 'INSERT' then 'A member joined your organization.'
            when 'DELETE' then 'A member left or was removed from your organization.'
            else 'A member''s organization role changed.' end
    from public.organization_memberships m
    where m.organization_id = v_org and m.role = 'President' and m.user_id <> v_user;
    return null;
end;
$$;
create trigger stagecall_membership_notification after insert or delete or update of role
    on public.organization_memberships for each row execute function public.stagecall_notify_membership_change();

create function public.stagecall_notify_profile_change()
returns trigger language plpgsql security definer set search_path = '' as $$
declare v_fields text[] := '{}';
begin
    if coalesce(old.bio, '') is distinct from new.bio then v_fields := array_append(v_fields, 'bio'); end if;
    if coalesce(old.student_number, '') is distinct from new.student_number then v_fields := array_append(v_fields, 'student ID'); end if;
    if old.year_level is distinct from new.year_level then v_fields := array_append(v_fields, 'year level'); end if;
    if coalesce(old.dark_theme, false) is distinct from new.dark_theme then v_fields := array_append(v_fields, 'theme preference'); end if;
    if coalesce(old.bookmarks, '{}'::text[]) is distinct from new.bookmarks then v_fields := array_append(v_fields, 'organization bookmarks'); end if;
    if cardinality(v_fields) > 0 then
        perform public.stagecall_notify_user(new.user_id, 'Profile and preferences updated',
            'Updated your ' || array_to_string(v_fields, ', ') || '.');
    end if;
    return new;
end;
$$;
create trigger stagecall_profile_notification after insert or update on public.user_profiles
    for each row execute function public.stagecall_notify_profile_change();

create function public.stagecall_notify_admin_role()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
    if tg_op = 'INSERT' then
        perform public.stagecall_notify_user(new.user_id, 'Admin access granted', 'You now have access to the admin panel.');
    elsif exists (select 1 from auth.users where id = old.user_id) then
        perform public.stagecall_notify_user(old.user_id, 'Admin access removed', 'Your admin access has been removed.');
    end if;
    return null;
end;
$$;
create trigger stagecall_admin_role_notification after insert or delete on public.stagecall_admins
    for each row execute function public.stagecall_notify_admin_role();

-- Compare content, not automatic membership counts. Unchanged saves stay silent.
create or replace function public.stagecall_notify_organization_update() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    if old.is_recruiting is distinct from new.is_recruiting then
        insert into public.organization_notifications (organization_id, kind, title, message)
        values (new.id, 'recruitment', case when new.is_recruiting then 'Applications are open' else 'Applications are closed' end,
            new.name || case when new.is_recruiting then ' is accepting applications.' else ' is no longer accepting applications.' end);
    end if;
    if (to_jsonb(old) - 'member_count' - 'is_recruiting') is distinct from
       (to_jsonb(new) - 'member_count' - 'is_recruiting') then
        insert into public.organization_notifications (organization_id, kind, title, message)
        values (new.id, 'organization', 'Organization details updated', new.name || ' updated its information, requirements, or images.');
    end if;
    return new;
end;
$$;

create function public.stagecall_notify_post_change() returns trigger
language plpgsql security definer set search_path = '' as $$
begin
    if tg_op = 'UPDATE' then
        if to_jsonb(old) is not distinct from to_jsonb(new) then return new; end if;
        insert into public.organization_notifications (organization_id, post_id, kind, title, message)
        values (new.organization_id, new.id, 'post_update', 'Post updated: ' || new.title,
            new.message || case when new.schedule <> '' then E'\nSchedule: ' || new.schedule else '' end);
        return new;
    end if;
    -- Preserve a removal notice without a reference to the deleted post.
    insert into public.organization_notifications (recipient_id, kind, title, message)
    select u.id, 'user_update', 'Post removed: ' || old.title, o.name || ' removed this post.'
    from public.organizations o cross join auth.users u
    where o.id = old.organization_id
        and (not old.members_only or exists (select 1 from public.organization_memberships m
            where m.organization_id = o.id and m.user_id = u.id))
        and (exists (select 1 from public.organization_memberships m where m.organization_id = o.id and m.user_id = u.id)
            or exists (select 1 from public.user_profiles p where p.user_id = u.id and o.id = any(p.bookmarks))
            or exists (select 1 from public.membership_applications a
                where a.user_id = u.id and a.organization_id = o.id and a.status = 'Pending'));
    return old;
end;
$$;
create trigger stagecall_post_change_notification after update or delete on public.campus_posts
    for each row execute function public.stagecall_notify_post_change();

revoke all on function public.stagecall_notify_submission_receipt() from public, anon, authenticated;
revoke all on function public.stagecall_notify_membership_change() from public, anon, authenticated;
revoke all on function public.stagecall_notify_profile_change() from public, anon, authenticated;
revoke all on function public.stagecall_notify_admin_role() from public, anon, authenticated;
revoke all on function public.stagecall_notify_post_change() from public, anon, authenticated;
create or replace function public.stagecall_admin_delete_organization(p_id text, p_note text)
returns void language plpgsql security definer set search_path = '' as $$
declare
    v_name text;
begin
    perform 1 from public.stagecall_admins where user_id = auth.uid() for share;
    if not found then raise exception 'Administrator access required' using errcode = '42501'; end if;
    if p_note is null or char_length(btrim(p_note)) not between 1 and 1000 then
        raise exception 'A deletion reason of 1 to 1000 characters is required' using errcode = '22023';
    end if;
    select name into v_name from public.organizations where id = p_id for update;
    if not found then raise exception 'Organization no longer exists'; end if;
    -- Snapshot the recipient and name before deletion clears registration.organization_id.
    -- The original accepted registration identifies its creator, even after leadership changes.
    insert into public.organization_notifications (recipient_id, kind, title, message)
    select distinct recipient, 'organization_deleted', 'Organization deleted: ' || v_name, btrim(p_note)
    from (
        select applicant_id as recipient from public.organization_registrations
        where organization_id = p_id and status = 'Accepted'
        union
        select user_id from public.organization_memberships where organization_id = p_id
        union
        select user_id from public.membership_applications where organization_id = p_id and status = 'Pending'
        union
        select user_id from public.user_profiles where p_id = any(bookmarks)
    ) recipients;
    delete from public.membership_applications where organization_id = p_id;
    delete from public.organizations where id = p_id;
end;
$$;
alter policy "Read visible organization notifications" on public.organization_notifications
    using (
        case when kind in ('organization_deleted', 'user_update') then recipient_id = (select auth.uid()) when kind = 'application_decision' then exists (
            select 1 from public.membership_applications a
            where a.id = organization_notifications.application_id and a.user_id = (select auth.uid())
        ) when kind = 'application_review' then exists (
            select 1 from public.organization_memberships m
            where m.organization_id = organization_notifications.organization_id
                and m.user_id = (select auth.uid()) and m.role = 'President'
        ) when kind = 'registration_review' then exists (
            select 1 from public.stagecall_admins a where a.user_id = (select auth.uid())
        ) when kind = 'registration_decision' then exists (
            select 1 from public.organization_registrations r
            where r.id = organization_notifications.registration_id and r.applicant_id = (select auth.uid())
        ) else (
            (post_id is null or exists (select 1 from public.campus_posts p where p.id = post_id))
            and (exists (select 1 from public.organization_memberships m
                where m.organization_id = organization_notifications.organization_id and m.user_id = (select auth.uid()))
                or exists (select 1 from public.user_profiles p
                    where p.user_id = (select auth.uid()) and organization_notifications.organization_id = any(p.bookmarks))
                or exists (select 1 from public.membership_applications a
                    where a.organization_id = organization_notifications.organization_id and a.user_id = (select auth.uid())
                        and a.status = 'Pending'))
        ) end
    );
commit;
