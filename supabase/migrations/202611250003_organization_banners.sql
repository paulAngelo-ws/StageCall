begin;

alter table public.organizations add column if not exists banner_path text;
create index if not exists organizations_banner_path_idx on public.organizations (banner_path) where banner_path is not null;

-- Profiles and banners share the private image bucket, size limit and MIME types.
drop policy if exists "Read published organization photos" on storage.objects;
create policy "Read published organization photos" on storage.objects for select to authenticated
using (bucket_id = 'organization-photos' and exists (
    select 1 from public.organizations o
    where o.photo_path = storage.objects.name or o.banner_path = storage.objects.name
));

create policy "Admins upload organization images" on storage.objects for insert to authenticated
with check (bucket_id = 'organization-photos'
    and (storage.foldername(name))[2] = (select auth.uid())::text
    and exists (select 1 from public.stagecall_admins where user_id = (select auth.uid())));

create or replace function public.stagecall_set_organization_images(
    p_id text, p_photo_path text, p_banner_path text,
    p_remove_photo boolean, p_remove_banner boolean
) returns void language plpgsql security definer set search_path = '' as $$
declare
    v_admin boolean := false;
    v_path text;
begin
    perform 1 from public.stagecall_admins where user_id = auth.uid() for share;
    v_admin := found;
    if not v_admin then
        perform 1 from public.organization_memberships
        where organization_id = p_id and user_id = auth.uid() and role = 'President' for share;
        if not found then raise exception 'Organization management access required' using errcode = '42501'; end if;
    end if;
    if p_remove_photo is null or p_remove_banner is null then raise exception 'Invalid image options'; end if;
    foreach v_path in array array[p_photo_path, p_banner_path] loop
        if v_path is not null and not exists (
            select 1 from storage.objects o where o.bucket_id = 'organization-photos' and o.name = v_path
            and (storage.foldername(o.name))[2] = auth.uid()::text
            and ((storage.foldername(o.name))[1] = p_id or (v_admin and (storage.foldername(o.name))[1] = 'admin'))
        ) then raise exception 'Invalid organization image' using errcode = '22023'; end if;
    end loop;
    update public.organizations
    set photo_path = case when p_remove_photo then null else coalesce(p_photo_path, photo_path) end,
        banner_path = case when p_remove_banner then null else coalesce(p_banner_path, banner_path) end
    where id = p_id;
    if not found then raise exception 'Organization no longer exists'; end if;
end;
$$;

-- Wrapper keeps the existing president settings RPC available to older clients.
create or replace function public.update_organization_settings_with_images(
    p_organization_id text, p_about_headline text, p_full_description text,
    p_is_recruiting boolean, p_banner_color text, p_requirements text[],
    p_photo_path text, p_remove_photo boolean, p_banner_path text, p_remove_banner boolean
) returns void language plpgsql security definer set search_path = '' as $$
begin
    perform public.update_organization_settings(p_organization_id, p_about_headline, p_full_description,
        p_is_recruiting, p_banner_color, p_requirements, p_photo_path, p_remove_photo);
    perform public.stagecall_set_organization_images(p_organization_id, null, p_banner_path, false, p_remove_banner);
end;
$$;

-- Admin create/edit and image attachment commit together, including new orgs.
create or replace function public.stagecall_admin_save_organization_with_images(
    p_id text, p_data jsonb, p_photo_path text, p_banner_path text,
    p_remove_photo boolean, p_remove_banner boolean
) returns text language plpgsql security definer set search_path = '' as $$
declare v_id text;
begin
    v_id := public.stagecall_admin_save_organization(p_id, p_data);
    perform public.stagecall_set_organization_images(v_id, p_photo_path, p_banner_path, p_remove_photo, p_remove_banner);
    return v_id;
end;
$$;

revoke all on function public.stagecall_set_organization_images(text, text, text, boolean, boolean) from public, anon, authenticated;
revoke all on function public.update_organization_settings_with_images(text, text, text, boolean, text, text[], text, boolean, text, boolean) from public, anon, authenticated;
revoke all on function public.stagecall_admin_save_organization_with_images(text, jsonb, text, text, boolean, boolean) from public, anon, authenticated;
grant execute on function public.update_organization_settings_with_images(text, text, text, boolean, text, text[], text, boolean, text, boolean) to authenticated;
grant execute on function public.stagecall_admin_save_organization_with_images(text, jsonb, text, text, boolean, boolean) to authenticated;

commit;
