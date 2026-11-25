-- Run as database owner after 202611250003_organization_banners.sql.
-- Storage rows below are metadata fixtures; no files are uploaded.
begin;
do $$
declare
    v_admin uuid := gen_random_uuid();
    v_president uuid := gen_random_uuid();
    v_stranger uuid := gen_random_uuid();
    v_org text;
    v_photo text;
    v_banner text;
    v_data jsonb := jsonb_build_object('name', 'Banner test ' || gen_random_uuid()::text,
        'category', 'Academic', 'description', 'Summary', 'short_tagline', 'Tagline',
        'about_headline', 'About us', 'full_description', 'Description',
        'banner_color', 'blue', 'is_recruiting', false, 'requirements', '[]'::jsonb);
begin
    insert into auth.users (id, email, email_confirmed_at) values
        (v_admin, v_admin::text || '@s.ubaguio.edu', now()),
        (v_president, v_president::text || '@s.ubaguio.edu', now()),
        (v_stranger, v_stranger::text || '@s.ubaguio.edu', now());
    insert into public.stagecall_admins (user_id) values (v_admin);
    perform set_config('request.jwt.claim.sub', v_admin::text, true);
    perform set_config('request.jwt.claims', jsonb_build_object('sub', v_admin)::text, true);

    -- New organizations can attach admin-uploaded media in the same transaction.
    v_banner := 'admin/' || v_admin::text || '/banner.png';
    insert into storage.objects (bucket_id, name) values ('organization-photos', v_banner);
    v_org := public.stagecall_admin_save_organization_with_images(null, v_data, null, v_banner, false, false);
    if not exists (select 1 from public.organizations where id = v_org and banner_path = v_banner and photo_path is null) then
        raise exception 'Admin banner creation failed';
    end if;
    insert into public.organization_memberships (organization_id, user_id, role) values (v_org, v_president, 'President');
    v_photo := v_org || '/' || v_president::text || '/profile.png';
    insert into storage.objects (bucket_id, name) values ('organization-photos', v_photo);
    perform set_config('request.jwt.claim.sub', v_president::text, true);
    perform set_config('request.jwt.claims', jsonb_build_object('sub', v_president)::text, true);
    perform public.update_organization_settings_with_images(v_org, 'About us', 'Description', false, 'blue',
        array[]::text[], v_photo, false, null, false);
    if not exists (select 1 from public.organizations where id = v_org and photo_path = v_photo and banner_path = v_banner) then
        raise exception 'Profile edit removed the banner';
    end if;
    -- A president cannot attach another user's upload, even from the same org.
    begin
        perform public.update_organization_settings_with_images(v_org, 'Unauthorized change', 'Description', false, 'blue',
            array[]::text[], null, false, v_banner, false);
        raise exception 'Another uploader''s image was accepted';
    exception when invalid_parameter_value then null;
    end;
    if exists (select 1 from public.organizations where id = v_org and about_headline = 'Unauthorized change') then
        raise exception 'Image failure did not roll back other changes';
    end if;
    perform public.update_organization_settings_with_images(v_org, 'About us', 'Description', false, 'blue',
        array[]::text[], null, false, null, true);
    if not exists (select 1 from public.organizations where id = v_org and photo_path = v_photo and banner_path is null) then
        raise exception 'Banner removal changed the profile';
    end if;
    perform set_config('request.jwt.claim.sub', v_stranger::text, true);
    perform set_config('request.jwt.claims', jsonb_build_object('sub', v_stranger)::text, true);
    begin
        perform public.update_organization_settings_with_images(v_org, 'About us', 'Description', false, 'blue',
            array[]::text[], null, true, null, true);
        raise exception 'Non-president could change organization images';
    exception when insufficient_privilege then null;
    end;
    if has_function_privilege('authenticated', 'public.stagecall_set_organization_images(text,text,text,boolean,boolean)', 'EXECUTE') then
        raise exception 'Internal image helper is exposed';
    end if;
end;
$$;
rollback;
