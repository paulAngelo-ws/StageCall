-- Run after 202611180004_membership_applications.sql.
-- Assign presidents separately as a project administrator; see README.
begin;
create table if not exists public.organization_memberships (
    organization_id text not null references public.organizations(id) on delete cascade,
    user_id uuid not null references auth.users(id) on delete cascade,
    role text not null default 'Member' check (role in ('Member', 'President')),
    created_at timestamptz not null default now(),
    primary key (organization_id, user_id)
);
create index if not exists organization_memberships_user_id_idx on public.organization_memberships(user_id);
alter table public.organization_memberships enable row level security;
revoke all on public.organization_memberships from public, anon, authenticated;
grant select on public.organization_memberships to authenticated;
drop policy if exists "Users read own memberships" on public.organization_memberships;
create policy "Users read own memberships" on public.organization_memberships
    for select to authenticated using (user_id = (select auth.uid()));

-- Preserve any applications already accepted by a project administrator.
insert into public.organization_memberships (organization_id, user_id, role)
select organization_id, user_id, 'Member' from public.membership_applications where status = 'Accepted'
on conflict (organization_id, user_id) do nothing;

-- Membership prevents a second application, including for manually assigned presidents.
drop policy if exists "Applicants submit own applications" on public.membership_applications;
create policy "Applicants submit own applications" on public.membership_applications
    for insert to authenticated with check (
        user_id = (select auth.uid()) and exists (
            select 1 from public.organizations o where o.id = organization_id and o.is_recruiting
        ) and not exists (
            select 1 from public.organization_memberships m
            where m.organization_id = membership_applications.organization_id and m.user_id = (select auth.uid())
        )
    );

-- Definer functions are necessary because presidents may not otherwise read other
-- applicants' rows. Each function verifies the caller's role for this organization.
create or replace function public.review_organization_applications(p_organization_id text)
returns setof public.membership_applications
language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is null or not exists (
        select 1 from public.organization_memberships m
        where m.organization_id = p_organization_id and m.user_id = auth.uid() and m.role = 'President'
    ) then
        raise exception 'Not authorized to review applications' using errcode = '42501';
    end if;
    return query select a.* from public.membership_applications a
        where a.organization_id = p_organization_id and a.user_id <> auth.uid()
        order by a.submitted_at desc;
end;
$$;

create or replace function public.decide_organization_application(
    p_organization_id text, p_application_id integer, p_status text, p_decision text default ''
) returns void language plpgsql security definer set search_path = '' as $$
declare v_user_id uuid;
begin
    if auth.uid() is null then
        raise exception 'Not authorized' using errcode = '42501';
    end if;
    -- Hold the role row until commit, so revocation and a decision cannot race.
    perform 1 from public.organization_memberships m
        where m.organization_id = p_organization_id and m.user_id = auth.uid() and m.role = 'President'
        for share;
    if not found then
        raise exception 'Not authorized to review applications' using errcode = '42501';
    end if;
    if p_status is null or p_status not in ('Accepted', 'Rejected') or p_decision is null or length(p_decision) > 2000 then
        raise exception 'Invalid decision';
    end if;
    update public.membership_applications a
        set status = p_status, decision = trim(p_decision), updated_at = now()
        where a.id = p_application_id and a.organization_id = p_organization_id
          and a.status = 'Pending' and a.user_id <> auth.uid()
        returning a.user_id into v_user_id;
    if not found then
        raise exception 'Application unavailable or already reviewed';
    end if;
    if p_status = 'Accepted' then
        insert into public.organization_memberships (organization_id, user_id, role)
            values (p_organization_id, v_user_id, 'Member')
            on conflict (organization_id, user_id) do nothing;
    end if;
end;
$$;
revoke all on function public.review_organization_applications(text) from public, anon, authenticated;
revoke all on function public.decide_organization_application(text, integer, text, text) from public, anon, authenticated;
grant execute on function public.review_organization_applications(text) to authenticated;
grant execute on function public.decide_organization_application(text, integer, text, text) to authenticated;
commit;
