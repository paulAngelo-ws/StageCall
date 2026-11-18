-- Legacy private-snapshot setup. The current guest app uses 202611180003_public_catalog.sql instead.
-- Each installation has a private state row. No public write policies.
-- Migration 202611180002_seed_demo_data.sql writes legacy JSON snapshots into this table.
create table if not exists public.stagecall_state (
    user_id uuid primary key references auth.users(id) on delete cascade,
    state jsonb not null check (jsonb_typeof(state) = 'object'),
    updated_at timestamptz not null default now()
);

alter table public.stagecall_state enable row level security;
revoke all on public.stagecall_state from anon;
grant select, insert, update on public.stagecall_state to authenticated;

drop policy if exists "Read own state" on public.stagecall_state;
create policy "Read own state" on public.stagecall_state
    for select to authenticated using ((select auth.uid()) = user_id);
drop policy if exists "Insert own state" on public.stagecall_state;
create policy "Insert own state" on public.stagecall_state
    for insert to authenticated with check ((select auth.uid()) = user_id);
drop policy if exists "Update own state" on public.stagecall_state;
create policy "Update own state" on public.stagecall_state
    for update to authenticated using ((select auth.uid()) = user_id)
    with check ((select auth.uid()) = user_id);

create or replace function public.stagecall_set_updated_at() returns trigger
language plpgsql set search_path = '' as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists stagecall_state_updated_at on public.stagecall_state;
create trigger stagecall_state_updated_at before update on public.stagecall_state
    for each row execute function public.stagecall_set_updated_at();
