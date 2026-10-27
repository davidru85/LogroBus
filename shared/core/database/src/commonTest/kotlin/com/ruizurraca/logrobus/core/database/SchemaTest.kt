package com.ruizurraca.logrobus.core.database

import kotlin.test.Test
import kotlin.test.assertEquals

/** Spec §8.8 and `architecture.md` §4.3: the tables and columns of the local database. */
class SchemaTest {
    @Test
    fun lb007_theSchemaIsTheSpecsPlusTheMvpAdditions() {
        val driver = testDriver()

        val tables = driver.strings("SELECT name FROM sqlite_master WHERE type = 'table' AND name NOT GLOB 'sqlite_*'")
        // FTS4 keeps its index in shadow tables named after the virtual table.
        val columns =
            tables.filterNot { it.startsWith("stop_fts_") }.associateWith { table ->
                driver.strings(
                    "SELECT name || ' ' || type || CASE WHEN \"notnull\" THEN ' NOT NULL' ELSE '' END " +
                        "FROM pragma_table_info('$table')",
                )
            }

        assertEquals(EXPECTED, columns)
    }

    private companion object {
        val EXPECTED =
            mapOf(
                "line" to
                    listOf(
                        "id INTEGER",
                        "code TEXT NOT NULL",
                        "raw_name TEXT NOT NULL",
                        "display_name TEXT NOT NULL",
                        "color INTEGER NOT NULL",
                        "on_color INTEGER NOT NULL",
                        "kind TEXT NOT NULL",
                        "operational INTEGER NOT NULL",
                    ),
                "pattern" to
                    listOf(
                        "line_id INTEGER NOT NULL",
                        "direction TEXT NOT NULL",
                        "headsign TEXT NOT NULL",
                        "shape_polyline TEXT NOT NULL",
                    ),
                "pattern_stop" to
                    listOf(
                        "line_id INTEGER NOT NULL",
                        "direction TEXT NOT NULL",
                        "seq INTEGER NOT NULL",
                        "stop_id INTEGER NOT NULL",
                        "shape_index INTEGER NOT NULL",
                    ),
                // normalized_name: architecture.md §4.3.
                "stop" to
                    listOf(
                        "id INTEGER",
                        "name TEXT NOT NULL",
                        "lat REAL NOT NULL",
                        "lng REAL NOT NULL",
                        "operational INTEGER NOT NULL",
                        "provisional INTEGER NOT NULL",
                        "normalized_name TEXT NOT NULL",
                    ),
                "stop_line" to listOf("stop_id INTEGER NOT NULL", "line_id INTEGER NOT NULL"),
                "stop_fts" to listOf("name ", "normalized_name "),
                "fare_product" to
                    listOf(
                        "code TEXT",
                        "name TEXT NOT NULL",
                        "description TEXT NOT NULL",
                        "price_cents INTEGER NOT NULL",
                        "category TEXT NOT NULL",
                        "tier TEXT NOT NULL",
                        "area TEXT NOT NULL",
                        "channel TEXT NOT NULL",
                        "valid_from TEXT",
                        "valid_until TEXT",
                        "max_quantity INTEGER NOT NULL",
                    ),
                "recharge_point" to
                    listOf("id INTEGER", "name TEXT", "type TEXT", "address TEXT", "lat REAL", "lng REAL"),
                "news" to
                    listOf(
                        "id INTEGER",
                        "document_id TEXT NOT NULL",
                        "title TEXT",
                        "summary TEXT",
                        "body_html TEXT",
                        "image_url TEXT",
                        "featured INTEGER",
                        "published_at INTEGER",
                        "updated_at INTEGER",
                        "affected_lines TEXT",
                        "notified INTEGER NOT NULL",
                    ),
                // missing_syncs: architecture.md §4.3, FR-FAV-04.
                "favourite" to
                    listOf(
                        "id TEXT",
                        "type TEXT NOT NULL",
                        "ref_id TEXT NOT NULL",
                        "label TEXT",
                        "position INTEGER NOT NULL",
                        "created_at INTEGER NOT NULL",
                        "missing_syncs INTEGER NOT NULL",
                    ),
                "place" to listOf("id TEXT", "name TEXT NOT NULL", "lat REAL NOT NULL", "lng REAL NOT NULL"),
                // plan_recent: architecture.md §4.3, FR-PLAN-09.
                "plan_recent" to
                    listOf("id TEXT", "origin TEXT", "destination TEXT", "created_at INTEGER"),
                "direction_code_cache" to listOf("line_id INTEGER", "code TEXT", "direction TEXT"),
                "sync_state" to listOf("dataset TEXT", "last_success INTEGER", "content_hash TEXT"),
            )
    }
}
