-- Organization membership controls members-only post visibility.
begin;
drop policy if exists "Members read organization posts" on public.campus_posts;
create policy "Members read organization posts" on public.campus_posts
    for select to authenticated using (
        members_only and exists (
            select 1 from public.organization_memberships m
            where m.organization_id = campus_posts.organization_id and m.user_id = (select auth.uid())
        )
    );

-- Remove the old public-only signature so there is only one callable variant.
drop function if exists public.create_organization_post(text, text, text, text, text);
-- The audience controls which rows the post policies expose. Migration 202611180013_post_images.sql adds the optional image path.
create or replace function public.create_organization_post(
    p_organization_id text, p_title text, p_message text, p_category text, p_schedule text, p_members_only boolean
) returns setof public.campus_posts
language plpgsql security definer set search_path = '' as $$
begin
    perform 1 from public.organization_memberships m
        where m.organization_id = p_organization_id and m.user_id = auth.uid() and m.role = 'President'
        for share;
    if not found then
        raise exception 'Not authorized to post for this organization' using errcode = '42501';
    end if;
    if p_title is null or length(trim(p_title)) not between 1 and 120
       or p_message is null or length(trim(p_message)) not between 1 and 5000
       or p_category is null or p_category not in ('Recruitment', 'Events', 'Announcements')
       or p_schedule is null or length(trim(p_schedule)) > 200 or p_members_only is null then
        raise exception 'Invalid post';
    end if;
    return query insert into public.campus_posts (organization_id, title, message, category, schedule, members_only)
        values (p_organization_id, trim(p_title), trim(p_message), p_category, trim(p_schedule), p_members_only)
        returning *;
end;
$$;
revoke all on function public.create_organization_post(text, text, text, text, text, boolean) from public, anon, authenticated;
grant execute on function public.create_organization_post(text, text, text, text, text, boolean) to authenticated;
commit;
