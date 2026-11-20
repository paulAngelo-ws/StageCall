-- For existing StageCall projects that have applied the earlier migrations through 017.
-- This also works if 018 or 019 was already applied. Existing data stays in place.
begin;

-- Keep the catalog count aligned with the membership roster, including existing organizations.
lock table public.organization_memberships in share row exclusive mode;

create or replace function public.stagecall_update_member_count()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
    if tg_op = 'TRUNCATE' then
        update public.organizations set member_count = 0 where member_count <> 0;
        return null;
    end if;
    if tg_op = 'UPDATE' and old.organization_id = new.organization_id then
        return null;
    end if;
    if tg_op in ('DELETE', 'UPDATE') then
        update public.organizations set member_count = member_count - 1 where id = old.organization_id;
    end if;
    if tg_op in ('INSERT', 'UPDATE') then
        update public.organizations set member_count = member_count + 1 where id = new.organization_id;
    end if;
    return null;
end;
$$;

revoke all on function public.stagecall_update_member_count() from public, anon, authenticated;
drop trigger if exists stagecall_membership_count on public.organization_memberships;
create trigger stagecall_membership_count
    after insert or delete or update of organization_id on public.organization_memberships
    for each row execute function public.stagecall_update_member_count();

drop trigger if exists stagecall_membership_count_reset on public.organization_memberships;
create trigger stagecall_membership_count_reset
    after truncate on public.organization_memberships
    for each statement execute function public.stagecall_update_member_count();

update public.organizations o set member_count = (
    select count(*) from public.organization_memberships m where m.organization_id = o.id
);

-- Expose only the fields displayed on the member profile to signed-in directory users.
create or replace function public.list_organization_members(p_organization_id text)
returns table (user_id uuid, name text, picture text, bio text, role text, joined_at timestamptz)
language sql stable security definer set search_path = '' as $$
    select m.user_id,
        coalesce(nullif(btrim(u.raw_user_meta_data ->> 'full_name'), ''), 'Campus member'),
        coalesce(nullif(u.raw_user_meta_data ->> 'avatar_url', ''), u.raw_user_meta_data ->> 'picture', ''),
        coalesce(p.bio, ''),
        m.role,
        m.created_at
    from public.organization_memberships m
    join auth.users u on u.id = m.user_id
    left join public.user_profiles p on p.user_id = m.user_id
    where auth.uid() is not null and m.organization_id = p_organization_id
    order by (m.role = 'President') desc, lower(coalesce(u.raw_user_meta_data ->> 'full_name', '')), m.user_id;
$$;

create or replace function public.remove_organization_member(p_organization_id text, p_user_id uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
    perform 1 from public.organization_memberships m
        where m.organization_id = p_organization_id and m.user_id = auth.uid() and m.role = 'President'
        for share;
    if not found then
        raise exception 'President access is required' using errcode = '42501';
    end if;

    -- Restrict removal to ordinary members and allow retries after a successful delete.
    delete from public.organization_memberships m
        where m.organization_id = p_organization_id and m.user_id = p_user_id and m.role = 'Member';
end;
$$;

revoke all on function public.list_organization_members(text) from public, anon, authenticated;
revoke all on function public.remove_organization_member(text, uuid) from public, anon, authenticated;
grant execute on function public.list_organization_members(text) to authenticated;
grant execute on function public.remove_organization_member(text, uuid) to authenticated;

create or replace function public.decide_organization_registration(p_id integer, p_approve boolean, p_decision text)
returns void language plpgsql security definer set search_path = '' as $$
declare
    v_registration public.organization_registrations%rowtype;
    v_organization_id text;
begin
    perform 1 from public.stagecall_admins where user_id = auth.uid() for share;
    if not found then
        raise exception 'Administrator access required' using errcode = '42501';
    end if;
    if p_approve is null or p_decision is null or char_length(btrim(p_decision)) > 1000 then
        raise exception 'Invalid decision';
    end if;

    select * into v_registration from public.organization_registrations
    where id = p_id and status = 'Pending'
    for update;
    if not found then
        raise exception 'Registration unavailable or already reviewed';
    end if;

    if p_approve then
        -- Serialize approvals of the same normalized name.
        perform pg_catalog.pg_advisory_xact_lock(912515, pg_catalog.hashtext(lower(btrim(v_registration.name))));
        if exists (
            select 1 from public.organizations where lower(btrim(name)) = lower(btrim(v_registration.name))
        ) then
            raise exception 'An organization with this name already exists' using errcode = '23505';
        end if;
        v_organization_id := 'registration-' || v_registration.id;
        insert into public.organizations (id, name, category, description, short_tagline, about_headline, full_description)
        values (v_organization_id, btrim(v_registration.name), btrim(v_registration.category),
                btrim(v_registration.tagline), btrim(v_registration.tagline), 'About us', btrim(v_registration.description));
        insert into public.organization_memberships (organization_id, user_id, role)
        values (v_organization_id, v_registration.applicant_id, 'President');
    end if;

    update public.organization_registrations
    set status = case when p_approve then 'Accepted' else 'Rejected' end,
        decision = btrim(p_decision), organization_id = v_organization_id,
        reviewed_by = auth.uid(), reviewed_at = now()
    where id = p_id;
end;
$$;


alter table public.organization_notifications
    drop constraint organization_notifications_kind_check,
    add constraint organization_notifications_kind_check
        check (kind in ('post', 'recruitment', 'organization', 'application_review')),
    add column if not exists application_id integer references public.membership_applications(id) on delete cascade;

create unique index if not exists organization_notifications_application_idx
    on public.organization_notifications (application_id) where application_id is not null;

alter policy "Read visible organization notifications" on public.organization_notifications
    using (
        case when kind = 'application_review' then exists (
            select 1 from public.organization_memberships m
            where m.organization_id = organization_notifications.organization_id
                and m.user_id = (select auth.uid()) and m.role = 'President'
        ) else (
            post_id is null or exists (select 1 from public.campus_posts p where p.id = post_id)
        ) end
    );

create or replace function public.stagecall_notify_application_submission()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
    insert into public.organization_notifications (organization_id, application_id, kind, title, message, created_at)
    values (new.organization_id, new.id, 'application_review', 'New membership application',
        'A student applied to join your organization.', new.submitted_at);
    return new;
end;
$$;

revoke all on function public.stagecall_notify_application_submission() from public, anon, authenticated;
drop trigger if exists stagecall_application_notification on public.membership_applications;
create trigger stagecall_application_notification
    after insert on public.membership_applications
    for each row when (new.status = 'Pending')
    execute function public.stagecall_notify_application_submission();

commit;
