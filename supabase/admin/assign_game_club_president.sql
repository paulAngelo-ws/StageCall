-- Run once in Supabase SQL Editor after installing the StageCall schema.
-- The Google account must have signed in to StageCall first.
-- The resulting membership row is read by OrganizationRoleStore.kt to enable president actions for this club.
begin;
do $$
declare
    target_user uuid;
    target_organization text;
begin
    select id into strict target_user from auth.users
    where lower(email) = lower('20255821@s.ubaguio.edu');

    select id into strict target_organization from public.organizations
    where name = 'UB Game Development Club';

    insert into public.organization_memberships (organization_id, user_id, role)
    values (target_organization, target_user, 'President')
    on conflict (organization_id, user_id) do update set role = excluded.role;
exception
    when no_data_found then
        raise exception 'User or organization not found. Check the email and organization name; the user must sign in first.';
    when too_many_rows then
        raise exception 'More than one user or organization matched. Resolve duplicates before assigning the role.';
end;
$$;

select o.name as organization, u.email, m.role
from public.organization_memberships m
join public.organizations o on o.id = m.organization_id
join auth.users u on u.id = m.user_id
where o.name = 'UB Game Development Club'
  and lower(u.email) = lower('20255821@s.ubaguio.edu');
commit;
