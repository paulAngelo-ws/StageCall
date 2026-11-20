-- Allow signed-in members to leave their organization.
begin;

create or replace function public.leave_organization(p_organization_id text)
returns void language plpgsql security definer set search_path = '' as $$
declare
    v_user_id uuid := auth.uid();
begin
    if v_user_id is null then
        raise exception 'Sign in to leave an organization' using errcode = '42501';
    end if;

    delete from public.organization_memberships m
    where m.organization_id = p_organization_id and m.user_id = v_user_id;
end;
$$;

revoke all on function public.leave_organization(text) from public, anon, authenticated;
grant execute on function public.leave_organization(text) to authenticated;

commit;
