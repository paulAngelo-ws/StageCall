begin;
-- OrganizationRoleStore.kt saves the upload path with a post. FeedScreen.kt passes that path to MediaCache.kt when a card displays it.
alter table public.campus_posts add column if not exists image_path text;
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('post-images', 'post-images', false, 5242880, array['image/jpeg', 'image/png'])
on conflict (id) do update set public = false, file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

-- Files are scoped to organization/user; only that organization's president can upload.
drop policy if exists "Presidents upload post images" on storage.objects;
create policy "Presidents upload post images" on storage.objects for insert to authenticated
with check (
    bucket_id = 'post-images' and (storage.foldername(name))[2] = (select auth.uid())::text
    and exists (select 1 from public.organization_memberships m
        where m.organization_id = (storage.foldername(name))[1]
          and m.user_id = (select auth.uid()) and m.role = 'President')
);
-- Post RLS controls image access; unpublished uploads are not readable.
drop policy if exists "Read visible post images" on storage.objects;
create policy "Read visible post images" on storage.objects for select to authenticated
using (bucket_id = 'post-images' and exists (
    select 1 from public.campus_posts p where p.image_path = storage.objects.name
));

drop function if exists public.create_organization_post(text, text, text, text, text, boolean);
-- Return the inserted post to OrganizationRoleStore.kt so it can appear immediately in the app.
create or replace function public.create_organization_post(
    p_organization_id text, p_title text, p_message text, p_category text,
    p_schedule text, p_members_only boolean, p_image_path text
) returns setof public.campus_posts
language plpgsql security definer set search_path = '' as $$
begin
    perform 1 from public.organization_memberships m where m.organization_id = p_organization_id
        and m.user_id = auth.uid() and m.role = 'President' for share;
    if not found then raise exception 'Not authorized to post for this organization' using errcode = '42501'; end if;
    if p_title is null or length(trim(p_title)) not between 1 and 120
       or p_message is null or length(trim(p_message)) not between 1 and 5000
       or p_category is null or p_category not in ('Recruitment', 'Events', 'Announcements')
       or p_schedule is null or length(trim(p_schedule)) > 200 or p_members_only is null then
        raise exception 'Invalid post';
    end if;
    if p_image_path is not null and not exists (
        select 1 from storage.objects o where o.bucket_id = 'post-images' and o.name = p_image_path
          and (storage.foldername(o.name))[1] = p_organization_id
          and (storage.foldername(o.name))[2] = auth.uid()::text
    ) then raise exception 'Invalid post image'; end if;
    return query insert into public.campus_posts (organization_id, title, message, category, schedule, members_only, image_path)
        values (p_organization_id, trim(p_title), trim(p_message), p_category, trim(p_schedule), p_members_only, p_image_path)
        returning *;
end;
$$;
revoke all on function public.create_organization_post(text, text, text, text, text, boolean, text) from public, anon, authenticated;
grant execute on function public.create_organization_post(text, text, text, text, text, boolean, text) to authenticated;
commit;
