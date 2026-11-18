-- Enable both functions in Authentication > Hooks after applying this migration.
-- Match the whole domain, allowing ubaguio.edu and its subdomains only.
create or replace function public.stagecall_before_user_created(event jsonb)
returns jsonb language plpgsql stable set search_path = '' as $$
begin
    if coalesce(event->'user'->>'email', '') !~* '^[^@[:space:]]+@([a-z0-9-]+\.)*ubaguio\.edu$' then
        return jsonb_build_object('error', jsonb_build_object(
            'http_code', 403, 'message', 'Please sign in with your University of Baguio Google account.'
        ));
    end if;
    return '{}'::jsonb;
end;
$$;

-- Also checks existing users whenever Supabase issues or refreshes a token.
create or replace function public.stagecall_restrict_access_token(event jsonb)
returns jsonb language plpgsql stable set search_path = '' as $$
begin
    if coalesce(event->'claims'->>'email', '') !~* '^[^@[:space:]]+@([a-z0-9-]+\.)*ubaguio\.edu$' then
        return jsonb_build_object('error', jsonb_build_object(
            'http_code', 403, 'message', 'Please sign in with your University of Baguio Google account.'
        ));
    end if;
    return jsonb_build_object('claims', event->'claims');
end;
$$;

grant execute on function public.stagecall_before_user_created(jsonb) to supabase_auth_admin;
grant execute on function public.stagecall_restrict_access_token(jsonb) to supabase_auth_admin;
revoke execute on function public.stagecall_before_user_created(jsonb) from public, anon, authenticated;
revoke execute on function public.stagecall_restrict_access_token(jsonb) from public, anon, authenticated;
