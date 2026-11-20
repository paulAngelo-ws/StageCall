-- Enable member profile email and shared organizations on an installed StageCall schema.
begin;

create or replace function public.get_organization_member_details(p_organization_id text, p_user_id uuid)
returns table (email text, shared_organizations text[])
language sql stable security definer set search_path = '' as $$
    select u.email::text, array(
        select o.name
        from public.organization_memberships target
        join public.organization_memberships viewer
            on viewer.organization_id = target.organization_id and viewer.user_id = (select auth.uid())
        join public.organizations o on o.id = target.organization_id
        where target.user_id = m.user_id and viewer.user_id <> m.user_id
        order by lower(o.name), o.id
    )
    from public.organization_memberships m
    join auth.users u on u.id = m.user_id
    where (select auth.uid()) is not null
        and m.organization_id = p_organization_id and m.user_id = p_user_id;
$$;

revoke all on function public.get_organization_member_details(text, uuid) from public, anon, authenticated;
grant execute on function public.get_organization_member_details(text, uuid) to authenticated;

commit;
