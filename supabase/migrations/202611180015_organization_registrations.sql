begin;

-- OrganizationRegistrationStore.kt reads this table to decide whether to show the review queue.
create table public.stagecall_admins (
    user_id uuid primary key references auth.users(id) on delete cascade
);
alter table public.stagecall_admins enable row level security;
revoke all on public.stagecall_admins from public, anon, authenticated;
grant select on public.stagecall_admins to authenticated;
create policy "Admins read own role" on public.stagecall_admins
    for select to authenticated using (user_id = (select auth.uid()));

-- OrganizationRegistrationStore.kt sends registration drafts here. Approving one can add an organization to the catalog.
create table public.organization_registrations (
    id integer generated always as identity primary key,
    applicant_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    applicant_email text not null default coalesce(auth.jwt()->>'email', ''),
    name text not null check (char_length(btrim(name)) between 1 and 100),
    category text not null check (char_length(btrim(category)) between 1 and 60),
    tagline text not null check (char_length(btrim(tagline)) between 1 and 200),
    description text not null check (char_length(btrim(description)) between 1 and 2000),
    status text not null default 'Pending' check (status in ('Pending', 'Accepted', 'Rejected')),
    decision text not null default '',
    organization_id text references public.organizations(id) on delete set null,
    submitted_at timestamptz not null default now(),
    reviewed_at timestamptz,
    reviewed_by uuid references auth.users(id) on delete set null
);
create unique index organization_registrations_pending_name_idx
    on public.organization_registrations (applicant_id, lower(btrim(name))) where status = 'Pending';

alter table public.organization_registrations enable row level security;
revoke all on public.organization_registrations from public, anon, authenticated;
grant select on public.organization_registrations to authenticated;
grant insert (name, category, tagline, description) on public.organization_registrations to authenticated;

create policy "Applicants and admins read registrations" on public.organization_registrations
    for select to authenticated using (
        applicant_id = (select auth.uid()) or exists (
            select 1 from public.stagecall_admins where user_id = (select auth.uid())
        )
    );
create policy "Applicants submit their own registrations" on public.organization_registrations
    for insert to authenticated with check (
        applicant_id = (select auth.uid()) and status = 'Pending'
    );

create function public.decide_organization_registration(p_id integer, p_approve boolean, p_decision text)
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
    where id = p_id and status = 'Pending' and applicant_id <> auth.uid()
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
        insert into public.organizations (id, name, category, description, short_tagline, about_headline, full_description, member_count)
        values (v_organization_id, btrim(v_registration.name), btrim(v_registration.category),
                btrim(v_registration.tagline), btrim(v_registration.tagline), 'About us', btrim(v_registration.description), 1);
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
revoke all on function public.decide_organization_registration(integer, boolean, text) from public, anon, authenticated;
grant execute on function public.decide_organization_registration(integer, boolean, text) to authenticated;

commit;
