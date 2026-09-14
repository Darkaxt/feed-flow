package com.prof18.feedflow.feedsync.database.data

import android.database.sqlite.SQLiteDatabase
import com.prof18.feedflow.feedsync.database.db.FeedFlowFeedSyncDB
import java.io.File

fun prepareSyncDatabaseFile(file: File) {
    SQLiteDatabase.openDatabase(file.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { database ->
        val version = database.rawQuery("PRAGMA user_version", null).use { cursor ->
            check(cursor.moveToFirst()) { "Missing sync database version" }
            cursor.getLong(0)
        }
        val integrity = database.rawQuery("PRAGMA integrity_check", null).use { cursor ->
            buildList {
                while (cursor.moveToNext()) add(cursor.getString(0))
            }
        }
        validateSyncDatabaseMetadata(version, integrity)
        syncDatabaseRequiredQueries.forEach { query ->
            database.rawQuery(query, null).use { cursor -> cursor.moveToFirst() }
        }
        if (version == 0L) {
            database.execSQL("PRAGMA user_version = ${FeedFlowFeedSyncDB.Schema.version}")
        }
    }
}
