package com.ruizurraca.logrobus.core.network

/**
 * Stop and line ids, which each source writes its own way (spec §5.3). Here, so the transit mappers and the planner's
 * read them alike.
 */
object IdParser {
    /** DD-01: `152` (API-02), `"0632"` (VM), `"38"` (ET) and `"1:100"` (OTP) are all stops 152, 632, 38 and 100. */
    fun stopId(raw: String?): Int? = raw?.length // TDD: red

    /** DD-02: `33` (API-01), `"1"` (VM and ET) and `"1:5-desc"` (OTP) are all lines 33, 1 and 5. */
    fun lineId(raw: String?): Int? = raw?.length // TDD: red
}
