-- Allow organization presidents to edit their organization’s posts.
begin;

create or replace function public.update_organization_post(
    p_organization_id text, p_title text, p_message text, p_category text,
    p_schedule text, p_members_only boolean, p_image_path text,
    p_post_id integer, p_remove_image boolean
) returns setof public.campus_posts
language plpgsql security definer set search_path = '' as $$
declare
    v_post public.campus_posts%rowtype;
begin
    perform 1 from public.organization_memberships m where m.organization_id = p_organization_id
        and m.user_id = auth.uid() and m.role = 'President' for share;
    if not found then raise exception 'Not authorized to edit posts for this organization' using errcode = '42501'; end if;
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
    update public.campus_posts p
    set title = trim(p_title), message = trim(p_message), category = p_category,
        schedule = trim(p_schedule), members_only = p_members_only,
        image_path = case when p_remove_image then null else coalesce(p_image_path, p.image_path) end
    where p.id = p_post_id and p.organization_id = p_organization_id
    returning p.* into v_post;
    if not found then raise exception 'Post unavailable for this organization'; end if;

    update public.organization_notifications n
    set title = case v_post.category when 'Events' then 'New activity: '
        when 'Announcements' then 'New announcement: ' else 'New post: ' end || v_post.title,
        message = v_post.message
    where n.post_id = v_post.id and n.kind = 'post';

    return next v_post;
end;
$$;

revoke all on function public.update_organization_post(text, text, text, text, text, boolean, text, integer, boolean) from public, anon, authenticated;
grant execute on function public.update_organization_post(text, text, text, text, text, boolean, text, integer, boolean) to authenticated;

commit;
