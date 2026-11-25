begin;

create function public.reassign_organization_president(p_organization_id text, p_user_id uuid)
returns void language plpgsql security definer set search_path = '' as $$
begin
    if auth.uid() is null then
        raise exception 'Sign in to reassign the president' using errcode = '42501';
    end if;

    -- Serialize transfers for this organization, then lock memberships so leaving
    -- or removal cannot invalidate authorization or the chosen successor.
    perform 1 from public.organizations where id = p_organization_id for update;
    if not found then
        raise exception 'Organization not found' using errcode = '22023';
    end if;
    perform 1 from public.organization_memberships
        where organization_id = p_organization_id order by user_id for update;

    perform 1 from public.stagecall_admins where user_id = auth.uid() for share;
    if not found and not exists (
        select 1 from public.organization_memberships
        where organization_id = p_organization_id and user_id = auth.uid() and role = 'President'
    ) then
        raise exception 'Current president or administrator access required' using errcode = '42501';
    end if;

    if not exists (
        select 1 from public.organization_memberships
        where organization_id = p_organization_id and user_id = p_user_id and role = 'Member'
    ) then
        raise exception 'Choose another member of this organization' using errcode = '22023';
    end if;

    update public.organization_memberships set role = 'Member'
        where organization_id = p_organization_id and role = 'President';
    update public.organization_memberships set role = 'President'
        where organization_id = p_organization_id and user_id = p_user_id;
end;
$$;

revoke all on function public.reassign_organization_president(text, uuid) from public, anon, authenticated;
grant execute on function public.reassign_organization_president(text, uuid) to authenticated;

commit;
