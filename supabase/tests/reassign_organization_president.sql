-- Run as database owner after migration 202611250007.
begin;
do $$
declare
    v_president uuid := gen_random_uuid();
    v_member uuid := gen_random_uuid();
    v_outsider uuid := gen_random_uuid();
    v_admin uuid := gen_random_uuid();
    v_org text;
    v_other text;
    v_data jsonb := jsonb_build_object(
        'name', 'Transfer test ' || gen_random_uuid()::text,
        'category', 'Academic', 'description', 'Summary', 'short_tagline', 'Tagline',
        'about_headline', 'About', 'full_description', 'Description',
        'banner_color', 'blue', 'is_recruiting', false, 'requirements', '[]'::jsonb);
begin
    insert into auth.users (id, email, email_confirmed_at)
    select id, id::text || '@s.ubaguio.edu', now()
    from unnest(array[v_president, v_member, v_outsider, v_admin]) id;
    insert into public.stagecall_admins(user_id) values (v_admin);
    perform set_config('request.jwt.claim.sub', v_admin::text, true);
    v_org := public.stagecall_admin_save_organization(null, v_data);
    v_other := public.stagecall_admin_save_organization(null,
        v_data || jsonb_build_object('name', 'Other ' || gen_random_uuid()::text));
    insert into public.organization_memberships(organization_id, user_id, role) values
        (v_org, v_president, 'President'), (v_org, v_member, 'Member'),
        (v_other, v_outsider, 'President');

    -- A president of another organization cannot transfer this club.
    perform set_config('request.jwt.claim.sub', v_outsider::text, true);
    begin
        perform public.reassign_organization_president(v_org, v_member);
        raise exception 'Foreign president was allowed';
    exception when insufficient_privilege then null;
    end;
    perform set_config('request.jwt.claim.sub', v_member::text, true);
    begin
        perform public.reassign_organization_president(v_org, v_member);
        raise exception 'Ordinary member was allowed';
    exception when insufficient_privilege then null;
    end;

    perform set_config('request.jwt.claim.sub', v_president::text, true);
    begin
        perform public.reassign_organization_president(v_org, v_outsider);
        raise exception 'Cross-organization successor was allowed';
    exception when invalid_parameter_value then null;
    end;
    begin
        perform public.reassign_organization_president(v_org, v_president);
        raise exception 'Self transfer was allowed';
    exception when invalid_parameter_value then null;
    end;
    perform public.reassign_organization_president(v_org, v_member);
    if not exists (select 1 from public.organization_memberships
        where organization_id = v_org and user_id = v_member and role = 'President')
       or not exists (select 1 from public.organization_memberships
        where organization_id = v_org and user_id = v_president and role = 'Member') then
        raise exception 'Transfer failed';
    end if;
    begin
        perform public.reassign_organization_president(v_org, v_president);
        raise exception 'Former president retained transfer access';
    exception when insufficient_privilege then null;
    end;

    -- Administrators need not belong to the club, but successors must belong.
    perform set_config('request.jwt.claim.sub', v_admin::text, true);
    begin
        perform public.reassign_organization_president(v_org, v_outsider);
        raise exception 'Admin assigned a non-member';
    exception when invalid_parameter_value then null;
    end;
    perform public.reassign_organization_president(v_org, v_president);
    if (select count(*) from public.organization_memberships
        where organization_id = v_org and role = 'President') <> 1
       or not exists (select 1 from public.organization_memberships
        where organization_id = v_org and user_id = v_president and role = 'President')
       or not exists (select 1 from public.organization_memberships
        where organization_id = v_other and user_id = v_outsider and role = 'President') then
        raise exception 'Admin transfer failed or changed another organization';
    end if;
    delete from public.stagecall_admins where user_id = v_admin;
    begin
        perform public.reassign_organization_president(v_org, v_member);
        raise exception 'Revoked administrator was allowed';
    exception when insufficient_privilege then null;
    end;
    perform set_config('request.jwt.claim.sub', '', true);
    begin
        perform public.reassign_organization_president(v_org, v_member);
        raise exception 'Unauthenticated transfer was allowed';
    exception when insufficient_privilege then null;
    end;
end;
$$;
rollback;
