-- Presidents may publish public posts only for their own organizations.
begin;
-- This first post function is replaced by 202611180012_post_audiences.sql, which adds a members-only option.
create or replace function public.create_organization_post(
    p_organization_id text, p_title text, p_message text, p_category text, p_schedule text
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
       or p_schedule is null or length(trim(p_schedule)) > 200 then
        raise exception 'Invalid post';
    end if;
    return query insert into public.campus_posts (organization_id, title, message, category, schedule, members_only)
        values (p_organization_id, trim(p_title), trim(p_message), p_category, trim(p_schedule), false)
        returning *;
end;
$$;
revoke all on function public.create_organization_post(text, text, text, text, text) from public, anon, authenticated;
grant execute on function public.create_organization_post(text, text, text, text, text) to authenticated;
commit;
