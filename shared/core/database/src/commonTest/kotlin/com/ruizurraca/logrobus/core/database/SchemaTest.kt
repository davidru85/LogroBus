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
                        "|| CASE WHEN pk > 0 THEN ' PK ' || pk ELSE '' END " +
                        "|| CASE WHEN dflt_value IS NOT NULL THEN ' DEFAULT ' || dflt_value ELSE '' END " +
                        "FROM pragma_table_info('$table')",
                )
            }

        assertEquals(EXPECTED, columns)
    }

    @Test
    fun lb007_theTablesHaveTheirUniqueKeysAndLookupIndexes() {
        val driver = testDriver()

        // Every index but a primary key's, as its columns and whether it is unique.
        val indexes =
            EXPECTED.keys
                .associateWith { table ->
                    driver.strings(
                        "SELECT (SELECT group_concat(name, ',') FROM pragma_index_info(list.name)) " +
                            "|| CASE WHEN list.\"unique\" THEN ' UNIQUE' ELSE '' END " +
                            "FROM pragma_index_list('$table') AS list WHERE list.origin != 'pk'",
                    )
                }.filterValues { it.isNotEmpty() }

        assertEquals(
            mapOf(
                // A stop's lines and patterns, for search subtitles (DD-16) and direction inference (DD-04).
                "pattern_stop" to listOf("stop_id"),
                "stop_line" to listOf("line_id"),
                "news" to listOf("document_id UNIQUE"),
                // One favourite per line or stop: a double tap must not save it twice (FR-FAV-05 counts them).
                "favourite" to listOf("type,ref_id UNIQUE"),
            ),
            indexes,
        )
    }

    private companion object {
        val EXPECTED =
            mapOf(
                "line" to
                    listOf(
                        "id INTEGER PK 1",
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
                        "line_id INTEGER NOT NULL PK 1",
                        "direction TEXT NOT NULL PK 2",
                        "headsign TEXT NOT NULL",
                        "shape_polyline TEXT NOT NULL",
                    ),
                "pattern_stop" to
                    listOf(
                        "line_id INTEGER NOT NULL PK 1",
                        "direction TEXT NOT NULL PK 2",
                        "seq INTEGER NOT NULL PK 3",
                        "stop_id INTEGER NOT NULL",
                        "shape_index INTEGER NOT NULL",
                    ),
                // normalized_name: architecture.md §4.3.
                "stop" to
                    listOf(
                        "id INTEGER PK 1",
                        "name TEXT NOT NULL",
                        "lat REAL NOT NULL",
                        "lng REAL NOT NULL",
                        "operational INTEGER NOT NULL",
                        "provisional INTEGER NOT NULL",
                        "normalized_name TEXT NOT NULL",
                    ),
                "stop_line" to listOf("stop_id INTEGER NOT NULL PK 1", "line_id INTEGER NOT NULL PK 2"),
                "stop_fts" to listOf("name ", "normalized_name "),
                "fare_product" to
                    listOf(
                        "code TEXT PK 1",
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
                    listOf("id INTEGER PK 1", "name TEXT", "type TEXT", "address TEXT", "lat REAL", "lng REAL"),
                "news" to
                    listOf(
                        "id INTEGER PK 1",
                        "document_id TEXT NOT NULL",
                        "title TEXT",
                        "summary TEXT",
                        "body_html TEXT",
                        "image_url TEXT",
                        "featured INTEGER",
                        "published_at INTEGER",
                        "updated_at INTEGER",
                        "affected_lines TEXT",
                        "notified INTEGER NOT NULL DEFAULT 0",
                    ),
                // missing_syncs: architecture.md §4.3, FR-FAV-04.
                "favourite" to
                    listOf(
                        "id TEXT PK 1",
                        "type TEXT NOT NULL",
                        "ref_id TEXT NOT NULL",
                        "label TEXT",
                        "position INTEGER NOT NULL",
                        "created_at INTEGER NOT NULL",
                        "missing_syncs INTEGER NOT NULL DEFAULT 0",
                    ),
                "place" to listOf("id TEXT PK 1", "name TEXT NOT NULL", "lat REAL NOT NULL", "lng REAL NOT NULL"),
                // plan_recent: architecture.md §4.3, FR-PLAN-09, which lists them newest first.
                "plan_recent" to
                    listOf("id TEXT PK 1", "origin TEXT", "destination TEXT", "created_at INTEGER NOT NULL"),
                // Key columns are NOT NULL, where spec §8.8 leaves them nullable: SQLite keeps rows whose key is NULL
                // apart, so a NULL line or code would cache duplicates (DD-04), and a NULL direction would cache an
                // ambiguous code as if it were known (architecture.md §4.3).
                "direction_code_cache" to
                    listOf("line_id INTEGER NOT NULL PK 1", "code TEXT NOT NULL PK 2", "direction TEXT NOT NULL"),
                "sync_state" to listOf("dataset TEXT NOT NULL PK 1", "last_success INTEGER", "content_hash TEXT"),
            )
    }
}
