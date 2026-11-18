-- Run after 202611180006_user_profiles.sql.
begin;
alter table public.user_profiles add column if not exists dark_theme boolean not null default false;
alter table public.user_profiles add column if not exists bookmarks text[] not null default '{}';

-- Update only the supplied fields so changes to a bio and preferences do not overwrite each other.
-- ProfileStore.kt calls this with the account ID and whichever field the user just changed.
create or replace function public.update_own_profile(
    p_user_id uuid, p_bio text default null, p_dark_theme boolean default null, p_bookmarks text[] default null
) returns void language plpgsql security invoker set search_path = '' as $$
begin
    if auth.uid() is null or auth.uid() <> p_user_id then
        raise exception 'Account has changed' using errcode = '42501';
    end if;
    if p_bio is not null and char_length(p_bio) > 500 then raise exception 'Bio too long'; end if;
    insert into public.user_profiles (user_id, bio, dark_theme, bookmarks)
    values (auth.uid(), coalesce(p_bio, ''), coalesce(p_dark_theme, false), coalesce(p_bookmarks, '{}'::text[]))
    on conflict (user_id) do update set
        bio = coalesce(p_bio, public.user_profiles.bio),
        dark_theme = coalesce(p_dark_theme, public.user_profiles.dark_theme),
        bookmarks = coalesce(p_bookmarks, public.user_profiles.bookmarks);
end;
$$;
revoke all on function public.update_own_profile(uuid, text, boolean, text[]) from public, anon, authenticated;
grant execute on function public.update_own_profile(uuid, text, boolean, text[]) to authenticated;
commit;
