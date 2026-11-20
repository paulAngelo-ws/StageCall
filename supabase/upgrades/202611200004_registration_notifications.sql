-- Enable admin submission alerts and applicant-only registration decisions.
begin;

alter table public.organization_notifications
    alter column organization_id drop not null,
    add column if not exists registration_id integer references public.organization_registrations(id) on delete cascade,
    drop constraint organization_notifications_kind_check,
    add constraint organization_notifications_kind_check check (kind in ('post', 'recruitment', 'organization', 'application_review',
        'registration_review', 'registration_decision')),
    drop constraint if exists organization_notifications_registration_scope_check,
    add constraint organization_notifications_registration_scope_check check (
        case when kind in ('registration_review', 'registration_decision') then
            registration_id is not null and organization_id is null and post_id is null and application_id is null
        else registration_id is null and organization_id is not null end
    );

create unique index if not exists organization_notifications_registration_idx
    on public.organization_notifications (registration_id, kind) where registration_id is not null;

alter policy "Read visible organization notifications" on public.organization_notifications
    using (
        case when kind = 'application_review' then exists (
            select 1 from public.organization_memberships m
            where m.organization_id = organization_notifications.organization_id
                and m.user_id = (select auth.uid()) and m.role = 'President'
        ) when kind = 'registration_review' then exists (
            select 1 from public.stagecall_admins a where a.user_id = (select auth.uid())
        ) when kind = 'registration_decision' then exists (
            select 1 from public.organization_registrations r
            where r.id = organization_notifications.registration_id and r.applicant_id = (select auth.uid())
        ) else (
            post_id is null or exists (select 1 from public.campus_posts p where p.id = post_id)
        ) end
    );

create or replace function public.stagecall_notify_registration()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
    if tg_op = 'INSERT' then
        if new.status = 'Pending' then
            insert into public.organization_notifications (registration_id, kind, title, message, created_at)
            values (new.id, 'registration_review', 'New organization registration',
                new.name || ' was submitted for review.', new.submitted_at);
        end if;
    elsif old.status = 'Pending' and new.status in ('Accepted', 'Rejected') then
        insert into public.organization_notifications (registration_id, kind, title, message)
        values (new.id, 'registration_decision',
            'Registration ' || lower(new.status) || ': ' || new.name,
            coalesce(nullif(btrim(new.decision), ''), 'Open your registration to view the decision.'));
    end if;
    return new;
end;
$$;

drop trigger if exists stagecall_registration_notification on public.organization_registrations;
create trigger stagecall_registration_notification
    after insert or update of status on public.organization_registrations
    for each row execute function public.stagecall_notify_registration();

revoke all on function public.stagecall_notify_registration() from public, anon, authenticated;
commit;
