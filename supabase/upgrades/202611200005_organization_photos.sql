-- Remove the obsolete deadline and add organization photos.
begin;

drop function if exists public.update_organization_settings(text, text, text, boolean, text);
drop function if exists public.update_organization_settings(text, text, text, boolean, text, text[], text);

alter table public.organizations
    drop column if exists deadline,
    add column if not exists photo_path text;

create index if not exists organizations_photo_path_idx
    on public.organizations (photo_path) where photo_path is not null;

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('organization-photos', 'organization-photos', false, 5242880, array['image/jpeg', 'image/png'])
on conflict (id) do update set public = false, file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "Presidents upload organization photos" on storage.objects;
create policy "Presidents upload organization photos" on storage.objects for insert to authenticated
with check (
    bucket_id = 'organization-photos' and (storage.foldername(name))[2] = (select auth.uid())::text
    and exists (
        select 1 from public.organization_memberships m
        where m.organization_id = (storage.foldername(name))[1]
            and m.user_id = (select auth.uid()) and m.role = 'President'
    )
);

drop policy if exists "Read published organization photos" on storage.objects;
create policy "Read published organization photos" on storage.objects for select to authenticated
using (
    bucket_id = 'organization-photos' and exists (
        select 1 from public.organizations o where o.photo_path = storage.objects.name
    )
);

create or replace function public.update_organization_settings(
    p_organization_id text, p_about_headline text, p_full_description text,
    p_is_recruiting boolean, p_banner_color text,
    p_requirements text[] default null, p_photo_path text default null, p_remove_photo boolean default false
) returns void language plpgsql security definer set search_path = '' as $$
begin
    perform 1 from public.organization_memberships m
        where m.organization_id = p_organization_id and m.user_id = auth.uid() and m.role = 'President'
        for share;
    if not found then
        raise exception 'Not authorized to edit organization' using errcode = '42501';
    end if;
    if p_about_headline is null or length(trim(p_about_headline)) not between 1 and 120
       or p_full_description is null or length(trim(p_full_description)) not between 1 and 5000
       or p_is_recruiting is null or p_banner_color is null
       or p_banner_color not in ('blue', 'green', 'purple', 'orange', 'indigo', 'gold') then
        raise exception 'Invalid organization settings';
    end if;
    if p_photo_path is not null and not exists (
        select 1 from storage.objects o
        where o.bucket_id = 'organization-photos' and o.name = p_photo_path
            and (storage.foldername(o.name))[1] = p_organization_id
            and (storage.foldername(o.name))[2] = auth.uid()::text
    ) then raise exception 'Invalid organization photo'; end if;

    update public.organizations set about_headline = trim(p_about_headline),
        full_description = trim(p_full_description), is_recruiting = p_is_recruiting,
        banner_color = p_banner_color,
        requirements = coalesce((
            select jsonb_agg(btrim(entry) order by position)
            from unnest(p_requirements) with ordinality as submitted(entry, position)
            where btrim(entry) <> ''
        ), case when p_requirements is not null then '[]'::jsonb else requirements end),
        photo_path = case when p_remove_photo then null else coalesce(p_photo_path, photo_path) end
    where id = p_organization_id;
    if not found then raise exception 'Organization not found'; end if;
end;
$$;

revoke all on function public.update_organization_settings(text, text, text, boolean, text, text[], text, boolean) from public, anon, authenticated;
grant execute on function public.update_organization_settings(text, text, text, boolean, text, text[], text, boolean) to authenticated;

commit;
