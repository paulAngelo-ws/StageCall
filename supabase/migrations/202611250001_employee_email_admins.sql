-- Verified employee emails receive app admin access. Keep manual grants separate
-- so an email change only revokes access granted by this rule.
begin;

alter table public.stagecall_admins
    add column if not exists granted_by_employee_email boolean not null default false;

create or replace function public.stagecall_sync_employee_admin()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
    if new.email_confirmed_at is not null
       and coalesce(new.email, '') ~* '^[^@[:space:]]+@e\.ubaguio\.edu$' then
        insert into public.stagecall_admins (user_id, granted_by_employee_email)
        values (new.id, true)
        on conflict (user_id) do nothing;
    else
        delete from public.stagecall_admins
        where user_id = new.id and granted_by_employee_email;
    end if;
    return new;
end;
$$;

revoke all on function public.stagecall_sync_employee_admin() from public, anon, authenticated;

drop trigger if exists stagecall_employee_admin on auth.users;
create trigger stagecall_employee_admin
after insert or update of email, email_confirmed_at on auth.users
for each row execute function public.stagecall_sync_employee_admin();

-- Include accounts created before this migration, without changing manual grants.
insert into public.stagecall_admins (user_id, granted_by_employee_email)
select id, true from auth.users
where email_confirmed_at is not null
  and coalesce(email, '') ~* '^[^@[:space:]]+@e\.ubaguio\.edu$'
on conflict (user_id) do nothing;

commit;
