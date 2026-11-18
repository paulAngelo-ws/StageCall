-- Run after migrations 004 and 005 in the Supabase SQL Editor.
begin;
-- ProfileStore.kt reads this row. Migration 202611180008_profile_preferences.sql extends it with theme and bookmark preferences.
create table if not exists public.user_profiles (
    user_id uuid primary key references auth.users(id) on delete cascade,
    bio text not null default '' check (char_length(bio) <= 500)
);
alter table public.user_profiles enable row level security;
revoke all on public.user_profiles from public, anon, authenticated;
grant select, insert, update on public.user_profiles to authenticated;
drop policy if exists "Users read own profile" on public.user_profiles;
create policy "Users read own profile" on public.user_profiles for select to authenticated
    using (user_id = (select auth.uid()));
drop policy if exists "Users insert own profile" on public.user_profiles;
create policy "Users insert own profile" on public.user_profiles for insert to authenticated
    with check (user_id = (select auth.uid()));
drop policy if exists "Users update own profile" on public.user_profiles;
create policy "Users update own profile" on public.user_profiles for update to authenticated
    using (user_id = (select auth.uid())) with check (user_id = (select auth.uid()));
commit;
