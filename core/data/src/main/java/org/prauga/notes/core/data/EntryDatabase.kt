// SPDX-FileCopyrightText: 2026 Saalim Quadri <danascape@gmail.com>
// SPDX-License-Identifier: Apache-2.0

package org.prauga.notes.core.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities =
        [
            CollectionEntity::class,
            EntryEntity::class,
            EntryFts::class,
            CategoryEntity::class,
            AlarmEntity::class,
            AttachmentEntity::class,
        ],
    version = 2,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
)
abstract class EntryDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao

    abstract fun collectionDao(): CollectionDao

    companion object {
        fun create(context: Context, name: String): EntryDatabase =
            Room.databaseBuilder(context, EntryDatabase::class.java, name).build()
    }
}
