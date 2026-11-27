package com.jlees.mibudget

import com.jlees.mibudget.update.NoteBlock
import com.jlees.mibudget.update.Updates
import com.jlees.mibudget.update.WhatsNewStep
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatesTest {
    @Test fun versions() {
        assertTrue(Updates.isNewer("v0.2.1", "0.2.0"))
        assertTrue(Updates.isNewer("v1.0.0", "0.9.9-dev"))
        assertFalse(Updates.isNewer("v0.2.0", "0.2.0"))
        assertFalse(Updates.isNewer("v0.1.9", "0.2.0"))
    }

    @Test fun checksAtMostEveryTwelveHours() {
        val h = 3_600_000L
        assertTrue(Updates.dueForCheck(now = 100 * h, lastNothingNew = 0))
        assertFalse(Updates.dueForCheck(now = 100 * h, lastNothingNew = 95 * h))
        assertTrue(Updates.dueForCheck(now = 100 * h, lastNothingNew = 88 * h))
    }

    @Test fun whatsNewOnlyAfterAnUpdate() {
        assertEquals(WhatsNewStep.RECORD, Updates.whatsNewStep("", "0.2.0", walkthroughDone = false))
        assertEquals(WhatsNewStep.SHOW, Updates.whatsNewStep("0.2.0", "0.2.1", walkthroughDone = true))
        assertEquals(WhatsNewStep.NONE, Updates.whatsNewStep("0.2.1", "0.2.1", walkthroughDone = true))
    }

    @Test fun notesDropDeveloperLines() {
        val n = Updates.parseNotes("0.2.0", "# mi. 0.2.0: refunds\n\nNow you can.\n\n## New\n- **Refunds** [here](x)\n\n[All changes](y)\n`app/mi-budget-0.2.0-release.apk`")
        assertEquals("refunds", n.tagline)
        assertEquals(listOf(NoteBlock.Paragraph("Now you can."), NoteBlock.Heading("New"), NoteBlock.Bullet("**Refunds** here")), n.blocks)
    }
}
