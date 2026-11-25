begin;

-- All catalog mutations pass through these functions; clients cannot write
-- arbitrary columns, membership counts, or admin roles.
create or replace function public.stagecall_admin_save_organization(p_id text, p_data jsonb)
returns text language plpgsql security definer set search_path = '' as $$
declare
    v_id text := coalesce(p_id, 'admin-' || gen_random_uuid()::text);
    v_name text := btrim(p_data->>'name');
    v_category text := btrim(p_data->>'category');
    v_tagline text := btrim(p_data->>'short_tagline');
    v_summary text := btrim(p_data->>'description');
    v_headline text := btrim(p_data->>'about_headline');
    v_description text := btrim(p_data->>'full_description');
    v_color text := p_data->>'banner_color';
    v_recruiting boolean := (p_data->>'is_recruiting')::boolean;
    v_requirements jsonb := p_data->'requirements';
begin
    perform 1 from public.stagecall_admins where user_id = auth.uid() for share;
    if not found then raise exception 'Administrator access required' using errcode = '42501'; end if;
    if v_name is null or char_length(v_name) not between 1 and 100
       or v_category is null or char_length(v_category) not between 1 and 60
       or v_tagline is null or char_length(v_tagline) not between 1 and 200
       or v_summary is null or char_length(v_summary) not between 1 and 2000
       or v_headline is null or char_length(v_headline) not between 1 and 200
       or v_description is null or char_length(v_description) not between 1 and 2000
       or v_color is null or v_color not in ('blue', 'green', 'purple', 'orange', 'indigo', 'gold')
       or v_recruiting is null
       or v_requirements is null or jsonb_typeof(v_requirements) <> 'array' then
        raise exception 'Invalid organization details' using errcode = '22023';
    end if;
    if jsonb_array_length(v_requirements) > 30 or exists (
        select 1 from jsonb_array_elements(v_requirements) item
        where jsonb_typeof(item) <> 'string' or char_length(btrim(item #>> '{}')) not between 1 and 200
    ) then raise exception 'Invalid requirements' using errcode = '22023'; end if;

    -- Use the same name lock as registration approvals.
    perform pg_catalog.pg_advisory_xact_lock(912515, pg_catalog.hashtext(lower(v_name)));
    if exists (select 1 from public.organizations where lower(btrim(name)) = lower(v_name) and id <> v_id) then
        raise exception 'An organization with this name already exists' using errcode = '23505';
    end if;
    if p_id is null then
        insert into public.organizations (id, name, category, description, short_tagline,
            about_headline, full_description, banner_color, is_recruiting, requirements)
        values (v_id, v_name, v_category, v_summary, v_tagline, v_headline, v_description,
            v_color, v_recruiting, v_requirements);
    else
        update public.organizations set name = v_name, category = v_category,
            description = v_summary, short_tagline = v_tagline, about_headline = v_headline,
            full_description = v_description, banner_color = v_color,
            is_recruiting = v_recruiting, requirements = v_requirements
        where id = p_id;
        if not found then raise exception 'Organization no longer exists'; end if;
    end if;
    return v_id;
end;
$$;

create or replace function public.stagecall_admin_delete_organization(p_id text)
returns void language plpgsql security definer set search_path = '' as $$
begin
    perform 1 from public.stagecall_admins where user_id = auth.uid() for share;
    if not found then raise exception 'Administrator access required' using errcode = '42501'; end if;
    perform 1 from public.organizations where id = p_id for update;
    if not found then raise exception 'Organization no longer exists'; end if;
    -- These applications deliberately have no cascade in the baseline schema.
    delete from public.membership_applications where organization_id = p_id;
    -- Posts, memberships and notifications cascade; registration history is kept.
    delete from public.organizations where id = p_id;
end;
$$;

revoke all on function public.stagecall_admin_save_organization(text, jsonb) from public, anon, authenticated;
revoke all on function public.stagecall_admin_delete_organization(text) from public, anon, authenticated;
grant execute on function public.stagecall_admin_save_organization(text, jsonb) to authenticated;
grant execute on function public.stagecall_admin_delete_organization(text) to authenticated;

commit;
