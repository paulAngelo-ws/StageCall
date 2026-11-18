begin;

-- OrganizationRegistrationStore.kt calls this version when an admin submits a decision. Approval creates a row for SupabaseStateStore.kt to read.
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

commit;
