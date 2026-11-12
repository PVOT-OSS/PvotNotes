// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), EntryDatabase::class.java)

    @Test
    fun keepsNotesFromVersionOne() {
        helper.createDatabase(DB, 1).use { db ->
            db.execSQL("INSERT INTO collections (id, name, color) VALUES (1, 'Notes', NULL)")
            db.execSQL(
                "INSERT INTO entries (id, uid, component, collection_id, summary, description, created," +
                    " last_modified, sequence) VALUES (1, 'u1', 'VJOURNAL', 1, 'Kept', 'body', 0, 0, 0)"
            )
            db.execSQL("INSERT INTO categories (entry_id, name, position) VALUES (1, 'home', 0)")
        }
        helper.runMigrationsAndValidate(DB, 2, true).use { db ->
            db.query("SELECT summary, parent_uid, rrule FROM entries WHERE id = 1").use { c ->
                assertTrue(c.moveToFirst())
                assertEquals("Kept", c.getString(0))
                assertTrue(c.isNull(1))
                assertTrue(c.isNull(2))
            }
            db.query("SELECT COUNT(*) FROM alarms").use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
        }
    }

    private companion object {
        const val DB = "migration-test.db"
    }
}
