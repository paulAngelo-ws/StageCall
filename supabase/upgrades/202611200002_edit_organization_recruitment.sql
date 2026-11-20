-- Run on installed StageCall databases to enable editing recruitment details.
begin;

drop function if exists public.update_organization_settings(text, text, text, boolean, text);

create or replace function public.update_organization_settings(
    p_organization_id text, p_about_headline text, p_full_description text,
    p_is_recruiting boolean, p_banner_color text,
    p_requirements text[] default null, p_deadline text default null
) returns void language plpgsql security definer set search_path = '' as $$
begin
    perform 1 from public.organization_memberships m
        where m.organization_id = p_organization_id and m.user_id = auth.uid() and m.role = 'President'
        for share;
    if not found then
        raise exception 'Not authorized to edit organization' using errcode = '42501';
    end if;
    if p_about_headline is null or length(trim(p_about_headline)) not between 1 and 120
       or p_full_description is null or length(trim(p_full_description)) not between 1 and 5000
       or p_is_recruiting is null or p_banner_color is null
       or p_banner_color not in ('blue', 'green', 'purple', 'orange', 'indigo', 'gold') then
        raise exception 'Invalid organization settings';
    end if;
    update public.organizations set about_headline = trim(p_about_headline),
        full_description = trim(p_full_description), is_recruiting = p_is_recruiting,
        banner_color = p_banner_color,
        requirements = coalesce((
            select jsonb_agg(btrim(entry) order by position)
            from unnest(p_requirements) with ordinality as submitted(entry, position)
            where btrim(entry) <> ''
        ), case when p_requirements is not null then '[]'::jsonb else requirements end),
        deadline = coalesce(btrim(p_deadline), deadline)
    where id = p_organization_id;
    if not found then raise exception 'Organization not found'; end if;
end;
$$;

revoke all on function public.update_organization_settings(text, text, text, boolean, text, text[], text) from public, anon, authenticated;
grant execute on function public.update_organization_settings(text, text, text, boolean, text, text[], text) to authenticated;

commit;
