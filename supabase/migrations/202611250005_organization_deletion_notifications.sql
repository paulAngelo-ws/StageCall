begin;
alter table public.organization_notifications
    add column recipient_id uuid references auth.users(id) on delete cascade,
    drop constraint organization_notifications_kind_check,
    add constraint organization_notifications_kind_check check (kind in (
        'post', 'recruitment', 'organization', 'application_review', 'application_decision',
        'registration_review', 'registration_decision', 'organization_deleted')),
    drop constraint organization_notifications_registration_scope_check,
    add constraint organization_notifications_registration_scope_check check (
        case when kind = 'organization_deleted' then
            recipient_id is not null and organization_id is null and registration_id is null
                and post_id is null and application_id is null
        when kind in ('registration_review', 'registration_decision') then
            registration_id is not null and organization_id is null and post_id is null and application_id is null
                and recipient_id is null
        else registration_id is null and organization_id is not null and recipient_id is null end
    );

-- Older clients must upgrade to provide a reason before deleting.
drop function public.stagecall_admin_delete_organization(text);
create function public.stagecall_admin_delete_organization(p_id text, p_note text)
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
    select distinct applicant_id, 'organization_deleted', 'Organization deleted: ' || v_name, btrim(p_note)
    from public.organization_registrations
    where organization_id = p_id and status = 'Accepted';
    delete from public.membership_applications where organization_id = p_id;
    delete from public.organizations where id = p_id;
end;
$$;
revoke all on function public.stagecall_admin_delete_organization(text, text) from public, anon, authenticated;
grant execute on function public.stagecall_admin_delete_organization(text, text) to authenticated;
alter policy "Read visible organization notifications" on public.organization_notifications
    using (
        case when kind = 'organization_deleted' then recipient_id = (select auth.uid()) when kind = 'application_decision' then exists (
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
            post_id is null or exists (select 1 from public.campus_posts p where p.id = post_id)
        ) end
    );
commit;
