package com.ruizurraca.logrobus.transit.domain

/** Spec §5.1: night (Búho) lines are those whose code starts with "B". */
enum class LineKind {
    DAY,
    NIGHT,
    ;

    companion object {
        /**
         * Spec §5.1's rule in one place: a line whose code starts with "B" is a night line. The catalogue's parser, the
         * stop detail's night-service message and the preview data all read it from here rather than testing the letter
         * themselves, so the answer cannot drift between them.
         */
        fun of(code: String): LineKind = if (code.startsWith('B')) NIGHT else DAY
    }
}
