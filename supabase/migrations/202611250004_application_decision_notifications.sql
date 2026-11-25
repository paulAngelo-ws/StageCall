-- Applicant-only membership decisions and admin registration alerts.
begin;

alter table public.organization_notifications
    drop constraint organization_notifications_kind_check,
    add constraint organization_notifications_kind_check check (kind in (
        'post', 'recruitment', 'organization', 'application_review', 'application_decision',
        'registration_review', 'registration_decision'));

drop index public.organization_notifications_application_idx;
create unique index organization_notifications_application_idx
    on public.organization_notifications (application_id, kind) where application_id is not null;

alter policy "Read visible organization notifications" on public.organization_notifications
    using (
        case when kind = 'application_decision' then exists (
            select 1 from public.membership_applications a
            where a.id = organization_notifications.application_id and a.user_id = (select auth.uid())
        ) when kind = 'application_review' then exists (
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

create function public.stagecall_notify_application_decision()
returns trigger language plpgsql security definer set search_path = '' as $$
begin
    insert into public.organization_notifications (organization_id, application_id, kind, title, message)
    values (new.organization_id, new.id, 'application_decision',
        'Application ' || lower(new.status),
        coalesce(nullif(btrim(new.decision), ''), 'Open your application to view the decision.'));
    return new;
end;
$$;

create trigger stagecall_application_decision_notification
    after update of status on public.membership_applications
    for each row when (old.status = 'Pending' and new.status in ('Accepted', 'Rejected'))
    execute function public.stagecall_notify_application_decision();

revoke all on function public.stagecall_notify_application_decision() from public, anon, authenticated;

-- Existing pending registrations should also appear in the admin inbox.
insert into public.organization_notifications (registration_id, kind, title, message, created_at)
select r.id, 'registration_review', 'New organization registration',
    r.name || ' was submitted for review.', r.submitted_at
from public.organization_registrations r
where r.status = 'Pending'
on conflict (registration_id, kind) where registration_id is not null do nothing;

commit;
