-- Run after migrations 004 and 005. Presidents can edit only their own organization.
begin;
alter table public.organizations add column if not exists banner_color text not null default 'blue';
alter table public.organizations drop constraint if exists organizations_banner_color_check;
alter table public.organizations add constraint organizations_banner_color_check
    check (banner_color in ('blue', 'green', 'purple', 'orange', 'indigo', 'gold'));

create or replace function public.update_organization_settings(
    p_organization_id text, p_about_headline text, p_full_description text,
    p_is_recruiting boolean, p_banner_color text
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
        banner_color = p_banner_color where id = p_organization_id;
    if not found then raise exception 'Organization not found'; end if;
end;
$$;
revoke all on function public.update_organization_settings(text, text, text, boolean, text) from public, anon, authenticated;
grant execute on function public.update_organization_settings(text, text, text, boolean, text) to authenticated;
commit;
