package com.mj.yaja.data

data class EntryTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val body: String,
    val bestFor: String = "",
    val isPopular: Boolean = false,
    val isNew: Boolean = false
)

object EntryTemplates {
    val builtIns: List<EntryTemplate> =
        listOf(
            EntryTemplate(
                id = "meeting_note",
                name = "Meeting Note",
                description = "Agenda, notes, decisions, and action items.",
                category = "Work",
                bestFor = "calls, syncs, reviews",
                isPopular = true,
                body =
                    """
                    ### Attendees
                    + Add attendees

                    ### Agenda
                    + Add agenda item

                    ### Notes

                    ### Decisions
                    + Add decision

                    ### Action Items
                    + [ ] Add action item

                    ### Follow-up
                    + Add follow-up
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "work_log",
                name = "Work Log",
                description = "Progress, blockers, wins, and next steps.",
                category = "Work",
                bestFor = "daily work updates",
                body =
                    """
                    ### What I Worked On
                    + Add work item

                    ### Progress
                    + Add progress note

                    ### Blockers
                    + Add blocker

                    ### Wins
                    + Add win

                    ### Next Steps
                    + [ ] Add next step
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "travel_day",
                name = "Travel Day",
                description = "Route, highlights, food, expenses, and moments.",
                category = "Life",
                bestFor = "trips, outings, journeys",
                isPopular = true,
                body =
                    """
                    ### Route
                    + From:
                    + To:
                    + Mode:

                    ### People
                    + Add people

                    ### Highlights
                    + Add highlight

                    ### Food
                    + Add food note

                    ### Expenses
                    + Add expense

                    ### Moments

                    ### Notes
                    + Add note
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "idea_dump",
                name = "Idea Dump",
                description = "Capture a rough idea before it disappears.",
                category = "Ideas",
                bestFor = "rough thoughts, sparks",
                isPopular = true,
                body =
                    """
                    ### Idea
                    + Add idea

                    ### Why It Matters
                    + Add reason

                    ### Rough Shape
                    + Add rough outline

                    ### Risks / Unknowns
                    + Add unknown

                    ### Next Action
                    + [ ] Add next action
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "health_log",
                name = "Health Log",
                description = "Symptoms, meds, sleep, food, and follow-up.",
                category = "Health",
                bestFor = "symptoms, meds, sleep",
                isPopular = true,
                body =
                    """
                    ### Symptoms
                    + Add symptom

                    ### Energy
                    + Add energy note

                    ### Sleep
                    + Add sleep note

                    ### Food
                    + Add food note

                    ### Medicines / Treatment
                    + Add medicine or treatment

                    ### Observations

                    ### Follow-up
                    + [ ] Add follow-up item
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "expense_note",
                name = "Expense Note",
                description = "Track a purchase, cost, reason, and follow-up.",
                category = "Money",
                bestFor = "purchases, spending",
                isNew = true,
                body =
                    """
                    ### Item
                    + Add item

                    ### Cost
                    + Add amount

                    ### Why I Bought It
                    + Add reason

                    ### Payment / Source
                    + Add payment detail

                    ### Follow-up
                    + [ ] Add warranty or return follow-up
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "reflection",
                name = "Reflection",
                description = "A simple reflective note for the day.",
                category = "Reflection",
                bestFor = "end-of-day reflection",
                isPopular = true,
                body =
                    """
                    ### What Happened
                    + Add moment

                    ### What I Felt
                    + Add feeling

                    ### What I Learned
                    + Add lesson

                    ### What I Want To Carry Forward
                    + Add takeaway
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "decision_log",
                name = "Decision Log",
                description = "Record a decision, options, and why it was made.",
                category = "Planning",
                bestFor = "important choices",
                isNew = true,
                body =
                    """
                    ### Decision
                    + Add decision

                    ### Options Considered
                    + Add option

                    ### Why This Choice
                    + Add reasoning

                    ### Risks / Trade-offs
                    + Add trade-off

                    ### Revisit
                    + [ ] Add revisit point
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "conversation_note",
                name = "Conversation Note",
                description = "Capture a call or important conversation.",
                category = "People",
                bestFor = "calls, check-ins, talks",
                isPopular = true,
                body =
                    """
                    ### With
                    + Add person

                    ### Context
                    + Add context

                    ### Key Points
                    + Add point

                    ### Promises / Next Steps
                    + [ ] Add promise or next step

                    ### Follow-up
                    + [ ] Add follow-up
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "event_note",
                name = "Event / Occasion",
                description = "Capture people, highlights, and memorable moments.",
                category = "Life",
                bestFor = "gatherings, functions",
                body =
                    """
                    ### Occasion
                    + Add event

                    ### People
                    + Add people

                    ### Place
                    + Add place

                    ### Highlights
                    + Add highlight

                    ### Memorable Line
                    + Add quote or moment

                    ### Follow-up
                    + [ ] Add follow-up
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "reading_note",
                name = "Reading / Watching Note",
                description = "Capture ideas, quotes, and takeaways from something consumed.",
                category = "Learning",
                bestFor = "books, videos, films",
                isNew = true,
                body =
                    """
                    ### Title
                    + Add title

                    ### Key Ideas
                    + Add idea

                    ### Favorite Quote / Moment
                    + Add quote

                    ### Takeaway
                    + Add takeaway

                    ### Follow-up
                    + [ ] Add follow-up
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "dream_log",
                name = "Dream Log",
                description = "Capture scenes, feelings, and symbols from a dream.",
                category = "Personal",
                bestFor = "dreams, symbols",
                body =
                    """
                    ### Dream Summary
                    + Add summary

                    ### Scenes
                    + Add scene

                    ### Feelings
                    + Add feeling

                    ### Symbols / Themes
                    + Add symbol
                    """.trimIndent()
            ),
            EntryTemplate(
                id = "project_update",
                name = "Project Update",
                description = "A structured snapshot of project status and next steps.",
                category = "Work",
                bestFor = "project tracking",
                isNew = true,
                body =
                    """
                    ### Status
                    + Add status

                    ### Progress
                    + Add progress

                    ### Issues
                    + Add issue

                    ### Next Milestone
                    + Add milestone

                    ### Next Steps
                    + [ ] Add next step
                    """.trimIndent()
            )
        )
}
