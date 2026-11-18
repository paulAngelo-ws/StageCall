-- Legacy private-snapshot seed. The current guest app uses supabase/setup.sql instead.
-- Safe to rerun: existing user snapshots are preserved, including edits.
-- Source of sample data for the app, using version-1 RepositorySnapshot JSON keys.
-- Sample students/memberships are demo fixtures, not Supabase accounts or roles.
begin;

create or replace function public.stagecall_demo_state() returns jsonb
language sql stable set search_path = '' as $function$
    with seed(payload) as (
        values ($stagecall_seed$
{
    "organizations":  [
                          {
                              "id":  "1",
                              "name":  "Google Developer Groups - UB",
                              "category":  "Academic / Tech",
                              "description":  "Build real-world web, mobile, and AI software projects with a collaborative student developer community.",
                              "shortTagline":  "Empowering SITizens through Google technologies, peer workshops, and hackathons.",
                              "aboutHeadline":  "Build solutions that matter",
                              "fullDescription":  "Google Developer Groups on Campus at the University of Baguio connects student developers interested in Android, Kotlin Multiplatform, Web, Cloud, and AI. Members collaborate on real campus projects and host technical study jams.",
                              "memberCount":  86,
                              "isRecruiting":  true,
                              "requirements":  [
                                                   "Currently enrolled UB SOIT student",
                                                   "Attend the developer onboarding session"
                                               ],
                              "deadline":  "November 25, 2026",
                              "nextActivityTitle":  "Kotlin \u0026 Compose Study Jam",
                              "nextActivitySchedule":  "Nov 16 · 3:00 PM · SIT AVR"
                          },
                          {
                              "id":  "2",
                              "name":  "UB Cybersecurity Society",
                              "category":  "Academic / Tech",
                              "description":  "Explore ethical hacking, Capture the Flag (CTF) competitions, network defense, and digital forensics.",
                              "shortTagline":  "Training the next generation of ethical hackers and information security defenders.",
                              "aboutHeadline":  "Defend, analyze, and secure",
                              "fullDescription":  "Supervised by the University of Baguio School of Information Technology, the Cybersecurity Society prepares students for national CTF tournaments while promoting campus-wide digital safety and penetration testing workshops.",
                              "memberCount":  54,
                              "isRecruiting":  true,
                              "requirements":  [
                                                   "Currently enrolled UB student",
                                                   "Complete basic Linux \u0026 networking assessment"
                                               ],
                              "deadline":  "November 22, 2026",
                              "nextActivityTitle":  "Intro to Capture the Flag Workshop",
                              "nextActivitySchedule":  "Nov 19 · 4:30 PM · IT Lab 3"
                          },
                          {
                              "id":  "3",
                              "name":  "SSVG",
                              "category":  "Community Service",
                              "description":  "Student-led tech outreach, digital literacy training, and peer academic support for SITizens.",
                              "shortTagline":  "Student-led tech outreach and peer support for SITizens.",
                              "aboutHeadline":  "Put IT skills to work",
                              "fullDescription":  "Supervised by the University of Baguio SIT Student Body, SSVG connects IT learning with community service, tech assistance, university events, and peer mentoring.",
                              "memberCount":  42,
                              "isRecruiting":  true,
                              "requirements":  [
                                                   "Currently enrolled SIT student",
                                                   "Attend one orientation session"
                                               ],
                              "deadline":  "November 20, 2026",
                              "nextActivityTitle":  "Volunteer orientation",
                              "nextActivitySchedule":  "Nov 18 · 4:00 PM · Student Center"
                          },
                          {
                              "id":  "4",
                              "name":  "Association for Computing",
                              "category":  "Academic / Tech",
                              "description":  "Connect through algorithmic coding contests, software engineering seminars, and research colloquiums.",
                              "shortTagline":  "Advancing computing as a science and profession across the UB campus.",
                              "aboutHeadline":  "Code, compete, and innovate",
                              "fullDescription":  "The UB Association for Computing organizes competitive programming bootcamps, algorithm problem-solving sessions, and industry guest lectures for Computer Science and Information Technology majors.",
                              "memberCount":  68,
                              "isRecruiting":  true,
                              "requirements":  [
                                                   "Open to all BSCS and BSIT students",
                                                   "Submit membership registration form"
                                               ],
                              "deadline":  "November 28, 2026",
                              "nextActivityTitle":  "Algorithmic Coding Sprint",
                              "nextActivitySchedule":  "Nov 21 · 1:30 PM · IT Lab 1"
                          },
                          {
                              "id":  "5",
                              "name":  "UB Game Development Club",
                              "category":  "Creative / Media",
                              "description":  "Design 2D/3D games, digital art assets, and interactive experiences using Unity, Godot, and Blender.",
                              "shortTagline":  "Bringing stories and interactive worlds to life through game design and programming.",
                              "aboutHeadline":  "Create playable worlds",
                              "fullDescription":  "A multidisciplinary collective of programmers, digital artists, and sound designers building indie games for annual university showcases and game jams.",
                              "memberCount":  49,
                              "isRecruiting":  false,
                              "requirements":  [
                                                   "Portfolio or sample project submission",
                                                   "Interview with core team leads"
                                               ],
                              "deadline":  "Closed for Semester 1",
                              "nextActivityTitle":  "Game Jam Showcase",
                              "nextActivitySchedule":  "Dec 04 · 2:00 PM · FGB Hall"
                          }
                      ],
    "profile":  {
                    "name":  "Alex Morgan",
                    "email":  "alex.morgan@university.edu",
                    "studentId":  "2024-00128"
                },
    "bookmarks":  [
                      "1"
                  ],
    "applications":  [
                         {
                             "id":  1,
                             "organizationId":  "2",
                             "student":  {
                                             "name":  "Alex Morgan",
                                             "email":  "alex.morgan@university.edu",
                                             "studentId":  "2024-00128"
                                         },
                             "year":  "2nd Year",
                             "reason":  "I want to build my cybersecurity skills.",
                             "status":  "Pending",
                             "decision":  "",
                             "updatedAt":  null
                         },
                         {
                             "id":  2,
                             "organizationId":  "3",
                             "student":  {
                                             "name":  "Alex Rivera",
                                             "email":  "alex.rivera@university.edu",
                                             "studentId":  "2024-00201"
                                         },
                             "year":  "2nd Year",
                             "reason":  "I want to use my IT skills in community outreach.",
                             "status":  "Pending",
                             "decision":  "",
                             "updatedAt":  null
                         },
                         {
                             "id":  3,
                             "organizationId":  "3",
                             "student":  {
                                             "name":  "Jamie Santos",
                                             "email":  "jamie@university.edu",
                                             "studentId":  "2025-00302"
                                         },
                             "year":  "1st Year",
                             "reason":  "I enjoy helping fellow students.",
                             "status":  "Pending",
                             "decision":  "",
                             "updatedAt":  null
                         }
                     ],
    "members":  [
                    {
                        "organizationId":  "4",
                        "student":  {
                                        "name":  "Alex Morgan",
                                        "email":  "alex.morgan@university.edu",
                                        "studentId":  "2024-00128"
                                    },
                        "role":  "Member",
                        "active":  true
                    },
                    {
                        "organizationId":  "3",
                        "student":  {
                                        "name":  "Morgan Lee",
                                        "email":  "morgan@university.edu",
                                        "studentId":  "2024-00128"
                                    },
                        "role":  "President",
                        "active":  true
                    }
                ],
    "posts":  [
                  {
                      "id":  1,
                      "organizationId":  "1",
                      "title":  "Build with us this semester.",
                      "message":  "Join the web, mobile or AI team. All skill levels welcome - curiosity comes first.",
                      "category":  "Recruitment",
                      "membersOnly":  false,
                      "schedule":  ""
                  },
                  {
                      "id":  2,
                      "organizationId":  "2",
                      "title":  "Intro to Capture the Flag.",
                      "message":  "Learn how to approach CTF challenges and team up with fellow students.",
                      "category":  "Events",
                      "membersOnly":  false,
                      "schedule":  "Nov 19 - 4:30 PM - IT Lab 3"
                  },
                  {
                      "id":  3,
                      "organizationId":  "3",
                      "title":  "Welcome to SSVG!",
                      "message":  "Join our volunteer orientation and explore community outreach.",
                      "category":  "Announcements",
                      "membersOnly":  false,
                      "schedule":  ""
                  }
              ],
    "notifications":  [

                      ],
    "version":  1
}
$stagecall_seed$::jsonb)
    )
    select payload || jsonb_build_object(
        'applications', (
            select jsonb_agg(item || jsonb_build_object('submittedAt', now()) order by position)
            from jsonb_array_elements(payload->'applications') with ordinality as entries(item, position)
        ),
        'posts', (
            select jsonb_agg(item || jsonb_build_object('createdAt', now()) order by position)
            from jsonb_array_elements(payload->'posts') with ordinality as entries(item, position)
        )
    ) from seed;
$function$;

-- Called only by the auth trigger or by an administrator in SQL Editor.
revoke all on function public.stagecall_demo_state() from public, anon, authenticated;

create or replace function public.stagecall_initialize_user_state() returns trigger
language plpgsql security definer set search_path = '' as $function$
begin
    insert into public.stagecall_state (user_id, state)
    values (new.id, public.stagecall_demo_state())
    on conflict (user_id) do nothing;
    return new;
end;
$function$;

revoke all on function public.stagecall_initialize_user_state() from public, anon, authenticated;

drop trigger if exists stagecall_initialize_user_state on auth.users;
create trigger stagecall_initialize_user_state
    after insert on auth.users
    for each row execute function public.stagecall_initialize_user_state();

-- Fill missing snapshots for accounts that signed in before this seed was installed.
-- No auth.users records or credentials are created by this script.
insert into public.stagecall_state (user_id, state)
select id, public.stagecall_demo_state() from auth.users
on conflict (user_id) do nothing;

commit;

-- Verify counts in SQL Editor without exposing profile contents:
-- select user_id,
--        jsonb_array_length(state->'organizations') as organizations,
--        jsonb_array_length(state->'applications') as applications,
--        jsonb_array_length(state->'members') as memberships,
--        jsonb_array_length(state->'posts') as posts
-- from public.stagecall_state;
