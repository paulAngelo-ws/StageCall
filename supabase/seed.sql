-- Optional sample organizations and posts. Run after the schema.
begin;

do $setup$
declare
    catalog jsonb := $catalog_seed$
{
    "organizations": [
        {
            "id": "1",
            "name": "Google Developer Groups - UB",
            "category": "Academic / Tech",
            "description": "Build real-world web, mobile, and AI software projects with a collaborative student developer community.",
            "shortTagline": "Empowering SITizens through Google technologies, peer workshops, and hackathons.",
            "aboutHeadline": "Build solutions that matter",
            "fullDescription": "Google Developer Groups on Campus at the University of Baguio connects student developers interested in Android, Kotlin Multiplatform, Web, Cloud, and AI. Members collaborate on real campus projects and host technical study jams.",
            "memberCount": 0,
            "isRecruiting": true,
            "requirements": [
                "Currently enrolled UB SOIT student",
                "Attend the developer onboarding session"
            ],
            "nextActivityTitle": "Kotlin & Compose Study Jam",
            "nextActivitySchedule": "Nov 16 · 3:00 PM · SIT AVR"
        },
        {
            "id": "2",
            "name": "UB Cybersecurity Society",
            "category": "Academic / Tech",
            "description": "Explore ethical hacking, Capture the Flag (CTF) competitions, network defense, and digital forensics.",
            "shortTagline": "Training the next generation of ethical hackers and information security defenders.",
            "aboutHeadline": "Defend, analyze, and secure",
            "fullDescription": "Supervised by the University of Baguio School of Information Technology, the Cybersecurity Society prepares students for national CTF tournaments while promoting campus-wide digital safety and penetration testing workshops.",
            "memberCount": 0,
            "isRecruiting": true,
            "requirements": [
                "Currently enrolled UB student",
                "Complete basic Linux & networking assessment"
            ],
            "nextActivityTitle": "Intro to Capture the Flag Workshop",
            "nextActivitySchedule": "Nov 19 · 4:30 PM · IT Lab 3"
        },
        {
            "id": "3",
            "name": "SSVG",
            "category": "Community Service",
            "description": "Student-led tech outreach, digital literacy training, and peer academic support for SITizens.",
            "shortTagline": "Student-led tech outreach and peer support for SITizens.",
            "aboutHeadline": "Put IT skills to work",
            "fullDescription": "Supervised by the University of Baguio SIT Student Body, SSVG connects IT learning with community service, tech assistance, university events, and peer mentoring.",
            "memberCount": 0,
            "isRecruiting": true,
            "requirements": [
                "Currently enrolled SIT student",
                "Attend one orientation session"
            ],
            "nextActivityTitle": "Volunteer orientation",
            "nextActivitySchedule": "Nov 18 · 4:00 PM · Student Center"
        },
        {
            "id": "4",
            "name": "Association for Computing",
            "category": "Academic / Tech",
            "description": "Connect through algorithmic coding contests, software engineering seminars, and research colloquiums.",
            "shortTagline": "Advancing computing as a science and profession across the UB campus.",
            "aboutHeadline": "Code, compete, and innovate",
            "fullDescription": "The UB Association for Computing organizes competitive programming bootcamps, algorithm problem-solving sessions, and industry guest lectures for Computer Science and Information Technology majors.",
            "memberCount": 0,
            "isRecruiting": true,
            "requirements": [
                "Open to all BSCS and BSIT students",
                "Submit membership registration form"
            ],
            "nextActivityTitle": "Algorithmic Coding Sprint",
            "nextActivitySchedule": "Nov 21 · 1:30 PM · IT Lab 1"
        },
        {
            "id": "5",
            "name": "UB Game Development Club",
            "category": "Creative / Media",
            "description": "Design 2D/3D games, digital art assets, and interactive experiences using Unity, Godot, and Blender.",
            "shortTagline": "Bringing stories and interactive worlds to life through game design and programming.",
            "aboutHeadline": "Create playable worlds",
            "fullDescription": "A multidisciplinary collective of programmers, digital artists, and sound designers building indie games for annual university showcases and game jams.",
            "memberCount": 0,
            "isRecruiting": false,
            "requirements": [
                "Portfolio or sample project submission",
                "Interview with core team leads"
            ],
            "nextActivityTitle": "Game Jam Showcase",
            "nextActivitySchedule": "Dec 04 · 2:00 PM · FGB Hall"
        }
    ],
    "posts": [
        {
            "id": 1,
            "organizationId": "1",
            "title": "Build with us this semester.",
            "message": "Join the web, mobile or AI team. All skill levels welcome - curiosity comes first.",
            "category": "Recruitment",
            "membersOnly": false,
            "schedule": ""
        },
        {
            "id": 2,
            "organizationId": "2",
            "title": "Intro to Capture the Flag.",
            "message": "Learn how to approach CTF challenges and team up with fellow students.",
            "category": "Events",
            "membersOnly": false,
            "schedule": "Nov 19 - 4:30 PM - IT Lab 3"
        },
        {
            "id": 3,
            "organizationId": "3",
            "title": "Welcome to SSVG!",
            "message": "Join our volunteer orientation and explore community outreach.",
            "category": "Announcements",
            "membersOnly": false,
            "schedule": ""
        }
    ]
}
$catalog_seed$::jsonb;
begin
    insert into public.organizations (
        id, name, category, description, short_tagline, about_headline, full_description,
        is_recruiting, requirements, next_activity_title, next_activity_schedule
    )
    select id, name, category, description, "shortTagline", "aboutHeadline", "fullDescription",
           "isRecruiting", requirements, "nextActivityTitle", "nextActivitySchedule"
    from jsonb_to_recordset(catalog->'organizations') as seed(
        id text, name text, category text, description text, "shortTagline" text,
        "aboutHeadline" text, "fullDescription" text, "memberCount" integer,
        "isRecruiting" boolean, requirements jsonb,
        "nextActivityTitle" text, "nextActivitySchedule" text
    )
    on conflict (id) do nothing;

    insert into public.campus_posts (
        id, organization_id, title, message, category, members_only, schedule
    )
    select id, "organizationId", title, message, category, "membersOnly", schedule
    from jsonb_to_recordset(catalog->'posts') as seed(
        id integer, "organizationId" text, title text, message text,
        category text, "membersOnly" boolean, schedule text
    )
    on conflict (id) do nothing;
end;
$setup$;

-- Explicit fixture IDs must not collide with subsequent SQL Editor inserts.
select setval(pg_get_serial_sequence('public.campus_posts', 'id'),
              greatest(coalesce((select max(id) from public.campus_posts), 0), 1),
              exists(select 1 from public.campus_posts));

commit;
