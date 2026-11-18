begin;

-- Use the caller's RLS permissions for both changed rows and removals.
-- CollectionCache.kt sends the name of a collection, the account ID, and a map of saved row IDs to versions.
create or replace function public.stagecall_sync(p_collection text, p_known jsonb, p_account uuid)
returns jsonb
language sql stable security invoker set search_path = ''
as $$
    -- Read only the requested collection. Row-level security and the account filters determine which records are visible.
    with visible(id, row_data) as (
        select o.id, to_jsonb(o) from public.organizations o where p_collection = 'organizations'
        union all
        select p.id::text, to_jsonb(p) from public.campus_posts p
            where p_collection = 'public_posts' and not p.members_only -- Only this collection includes posts marked public.
        union all
        select p.id::text, to_jsonb(p) from public.campus_posts p
            where p_collection = 'member_posts' and p.members_only -- The post table’s access rules decide which member posts are visible.
        union all
        select a.id::text, to_jsonb(a) from public.membership_applications a
            where p_collection = 'applications' and a.user_id = (select auth.uid()) -- Check only this student’s own requests.
        union all
        select p.user_id::text, to_jsonb(p) from public.user_profiles p
            where p_collection = 'profile' and p.user_id = (select auth.uid())
        union all
        select m.organization_id, to_jsonb(m) from public.organization_memberships m
            where p_collection = 'memberships' and m.user_id = (select auth.uid())
        union all
        select r.id::text, to_jsonb(r) from public.organization_registrations r
            where p_collection = 'registrations' and r.applicant_id = (select auth.uid())
        union all
        select n.id::text, to_jsonb(n) || jsonb_build_object('is_read', r.notification_id is not null)
            from public.organization_notifications n
            left join public.organization_notification_reads r
                on r.notification_id = n.id and r.user_id = (select auth.uid()) -- Look up this account’s read receipt for each notification.
            where p_collection = 'notifications'
    -- Hash the full row so edits can be detected even for tables without an updated timestamp.
    ), versions as (
        select id, row_data, encode(sha256(convert_to(row_data::text, 'UTF8')), 'hex') as version -- Convert the row into a version that changes when its contents change.
        from visible
        where (select auth.uid()) = p_account
    )
    -- Return changed rows and removed IDs to CollectionCache.kt. A removal can mean a deletion or a row the account can no longer see.
    select jsonb_build_object(
        'changed', coalesce((
            select jsonb_object_agg(id, jsonb_build_object('version', version, 'data', row_data))
            from versions where p_known->>id is distinct from version -- Send row data only when the saved version differs or is missing.
        ), '{}'::jsonb),
        'removed', coalesce((
            select jsonb_agg(known.id)
            from jsonb_object_keys(coalesce(p_known, '{}'::jsonb)) as known(id) -- Start with the IDs CollectionCache.kt said it already had.
            where (select auth.uid()) = p_account and not exists (select 1 from versions where versions.id = known.id) -- Remove a saved ID when it is missing from the visible rows.
        ), '[]'::jsonb)
    ) where (select auth.uid()) = p_account;
$$;

revoke all on function public.stagecall_sync(text, jsonb, uuid) from public, anon, authenticated;
grant execute on function public.stagecall_sync(text, jsonb, uuid) to authenticated;

commit;
