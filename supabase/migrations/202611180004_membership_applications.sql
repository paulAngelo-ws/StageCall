-- Run after supabase/setup.sql. Applicants can only submit and read their own rows.
begin;
create table if not exists public.membership_applications (
    id integer generated always as identity primary key,
    user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
    organization_id text not null references public.organizations(id),
    applicant_name text not null check (length(trim(applicant_name)) between 1 and 200),
    applicant_email text not null check (length(trim(applicant_email)) between 3 and 320),
    student_number text not null check (length(trim(student_number)) between 1 and 100),
    year_level text not null check (year_level in ('1st Year', '2nd Year', '3rd Year', '4th Year')),
    reason text not null check (length(trim(reason)) between 1 and 5000),
    status text not null default 'Pending' check (status in ('Pending', 'Accepted', 'Rejected')),
    decision text not null default '',
    submitted_at timestamptz not null default now(),
    updated_at timestamptz,
    unique (user_id, organization_id)
);
alter table public.membership_applications enable row level security;
revoke all on public.membership_applications from public, anon, authenticated;
grant select on public.membership_applications to authenticated;
grant insert (organization_id, applicant_name, applicant_email, student_number, year_level, reason)
    on public.membership_applications to authenticated;
drop policy if exists "Applicants read own applications" on public.membership_applications;
create policy "Applicants read own applications" on public.membership_applications
    for select to authenticated using (user_id = (select auth.uid()));
drop policy if exists "Applicants submit own applications" on public.membership_applications;
create policy "Applicants submit own applications" on public.membership_applications
    for insert to authenticated with check (
        user_id = (select auth.uid()) and exists (
            select 1 from public.organizations o where o.id = organization_id and o.is_recruiting
        )
    );
commit;
