package com.weissev.stagecall

import com.weissev.stagecall.data.*

// Test-only fixtures. These records are not included in any application build.
// Give the workflow and persistence tests a reproducible StageCallRepository.kt with fixtures already loaded.
internal fun populatedRepository(): StageCallRepository {
    val organizations = ubSoitOrganizations
    val profile = Student("Alex Morgan", "alex.morgan@university.edu", "2024-00128")

    val bookmarks = listOf("1")
    val applications =
        listOf(
            MembershipApplication(
                1,
                "2",
                profile,
                "2nd Year",
                "I want to build my cybersecurity skills.",
            ),
            MembershipApplication(
                2,
                "3",
                Student("Alex Rivera", "alex.rivera@university.edu", "2024-00201"),
                "2nd Year",
                "I want to use my IT skills in community outreach.",
            ),
            MembershipApplication(
                3,
                "3",
                Student("Jamie Santos", "jamie@university.edu", "2025-00302"),
                "1st Year",
                "I enjoy helping fellow students.",
            ),
        )
    val members =
        listOf(
            Member("4", profile),
            Member("3", Student("Morgan Lee", "morgan@university.edu", "2024-00128"), "President"),
        )
    val posts =
        listOf(
            CampusPost(
                1,
                "1",
                "Build with us this semester.",
                "Join the web, mobile or AI team. All skill levels welcome - curiosity comes first.",
                "Recruitment",
            ),
            CampusPost(
                2,
                "2",
                "Intro to Capture the Flag.",
                "Learn how to approach CTF challenges and team up with fellow students.",
                "Events",
                schedule = "Nov 19 - 4:30 PM - IT Lab 3",
            ),
            CampusPost(
                3,
                "3",
                "Welcome to SSVG!",
                "Join our volunteer orientation and explore community outreach.",
                "Announcements",
            ),
        )
    return StageCallRepository().apply {
        restore(RepositorySnapshot(
            organizations, profile, bookmarks, applications, members, posts, emptyList(),
        ))
    }
}
val ubSoitOrganizations =
    listOf(
        Organization(
            id = "1",
            name = "Google Developer Groups - UB",
            category = "Academic / Tech",
            description =
                "Build real-world web, mobile, and AI software projects with a collaborative student developer community.",
            shortTagline =
                "Empowering SITizens through Google technologies, peer workshops, and hackathons.",
            aboutHeadline = "Build solutions that matter",
            fullDescription =
                "Google Developer Groups on Campus at the University of Baguio connects student developers interested in Android, Kotlin Multiplatform, Web, Cloud, and AI. Members collaborate on real campus projects and host technical study jams.",
            memberCount = 86,
            isRecruiting = true,
            requirements =
                listOf(
                    "Currently enrolled UB SOIT student",
                    "Attend the developer onboarding session",
                ),
            nextActivityTitle = "Kotlin & Compose Study Jam",
            nextActivitySchedule = "Nov 16 · 3:00 PM · SIT AVR",
        ),
        Organization(
            id = "2",
            name = "UB Cybersecurity Society",
            category = "Academic / Tech",
            description =
                "Explore ethical hacking, Capture the Flag (CTF) competitions, network defense, and digital forensics.",
            shortTagline =
                "Training the next generation of ethical hackers and information security defenders.",
            aboutHeadline = "Defend, analyze, and secure",
            fullDescription =
                "Supervised by the University of Baguio School of Information Technology, the Cybersecurity Society prepares students for national CTF tournaments while promoting campus-wide digital safety and penetration testing workshops.",
            memberCount = 54,
            isRecruiting = true,
            requirements =
                listOf(
                    "Currently enrolled UB student",
                    "Complete basic Linux & networking assessment",
                ),
            nextActivityTitle = "Intro to Capture the Flag Workshop",
            nextActivitySchedule = "Nov 19 · 4:30 PM · IT Lab 3",
        ),
        Organization(
            id = "3",
            name = "SSVG",
            category = "Community Service",
            description =
                "Student-led tech outreach, digital literacy training, and peer academic support for SITizens.",
            shortTagline = "Student-led tech outreach and peer support for SITizens.",
            aboutHeadline = "Put IT skills to work",
            fullDescription =
                "Supervised by the University of Baguio SIT Student Body, SSVG connects IT learning with community service, tech assistance, university events, and peer mentoring.",
            memberCount = 42,
            isRecruiting = true,
            requirements =
                listOf("Currently enrolled SIT student", "Attend one orientation session"),
            nextActivityTitle = "Volunteer orientation",
            nextActivitySchedule = "Nov 18 · 4:00 PM · Student Center",
        ),
        Organization(
            id = "4",
            name = "Association for Computing",
            category = "Academic / Tech",
            description =
                "Connect through algorithmic coding contests, software engineering seminars, and research colloquiums.",
            shortTagline = "Advancing computing as a science and profession across the UB campus.",
            aboutHeadline = "Code, compete, and innovate",
            fullDescription =
                "The UB Association for Computing organizes competitive programming bootcamps, algorithm problem-solving sessions, and industry guest lectures for Computer Science and Information Technology majors.",
            memberCount = 68,
            isRecruiting = true,
            requirements =
                listOf("Open to all BSCS and BSIT students", "Submit membership registration form"),
            nextActivityTitle = "Algorithmic Coding Sprint",
            nextActivitySchedule = "Nov 21 · 1:30 PM · IT Lab 1",
        ),
        Organization(
            id = "5",
            name = "UB Game Development Club",
            category = "Creative / Media",
            description =
                "Design 2D/3D games, digital art assets, and interactive experiences using Unity, Godot, and Blender.",
            shortTagline =
                "Bringing stories and interactive worlds to life through game design and programming.",
            aboutHeadline = "Create playable worlds",
            fullDescription =
                "A multidisciplinary collective of programmers, digital artists, and sound designers building indie games for annual university showcases and game jams.",
            memberCount = 49,
            isRecruiting = false,
            requirements =
                listOf("Portfolio or sample project submission", "Interview with core team leads"),
            nextActivityTitle = "Game Jam Showcase",
            nextActivitySchedule = "Dec 04 · 2:00 PM · FGB Hall",
        ),
    )
