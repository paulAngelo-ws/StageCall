alter table public.user_profiles
    add column student_number text not null default '' check (char_length(student_number) <= 8),
    add column year_level text check (year_level in ('1st Year', '2nd Year', '3rd Year', '4th Year'));

drop function if exists public.update_own_profile(uuid, text, boolean, text[]);
create function public.update_own_profile(
    p_user_id uuid, p_bio text default null, p_dark_theme boolean default null, p_bookmarks text[] default null,
    p_student_number text default null, p_year_level text default null
) returns void language plpgsql security invoker set search_path = '' as $$
begin
    if auth.uid() is null or auth.uid() <> p_user_id then
        raise exception 'Account has changed' using errcode = '42501';
    end if;
    if p_bio is not null and char_length(p_bio) > 500 then raise exception 'Bio too long'; end if;
    insert into public.user_profiles (user_id, bio, dark_theme, bookmarks, student_number, year_level)
    values (auth.uid(), coalesce(p_bio, ''), coalesce(p_dark_theme, false), coalesce(p_bookmarks, '{}'::text[]),
        coalesce(p_student_number, ''), p_year_level)
    on conflict (user_id) do update set
        bio = coalesce(p_bio, public.user_profiles.bio),
        dark_theme = coalesce(p_dark_theme, public.user_profiles.dark_theme),
        bookmarks = coalesce(p_bookmarks, public.user_profiles.bookmarks),
        student_number = coalesce(p_student_number, public.user_profiles.student_number),
        year_level = coalesce(p_year_level, public.user_profiles.year_level);
end;
$$;

revoke all on function public.update_own_profile(uuid, text, boolean, text[], text, text) from public, anon, authenticated;
grant execute on function public.update_own_profile(uuid, text, boolean, text[], text, text) to authenticated;

drop function if exists public.get_organization_member_details(text, uuid);
create function public.get_organization_member_details(p_organization_id text, p_user_id uuid)
returns table (email text, shared_organizations text[], year_level text)
language sql stable security definer set search_path = '' as $$
    select u.email::text, array(
        select o.name
        from public.organization_memberships target
        join public.organization_memberships viewer
            on viewer.organization_id = target.organization_id and viewer.user_id = (select auth.uid())
        join public.organizations o on o.id = target.organization_id
        where target.user_id = m.user_id and viewer.user_id <> m.user_id
        order by lower(o.name), o.id
    ), p.year_level
    from public.organization_memberships m
    join auth.users u on u.id = m.user_id
    left join public.user_profiles p on p.user_id = m.user_id
    where (select auth.uid()) is not null
        and m.organization_id = p_organization_id and m.user_id = p_user_id;
$$;

revoke all on function public.get_organization_member_details(text, uuid) from public, anon, authenticated;
grant execute on function public.get_organization_member_details(text, uuid) to authenticated;
