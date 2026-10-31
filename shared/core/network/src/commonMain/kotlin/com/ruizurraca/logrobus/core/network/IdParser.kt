package com.ruizurraca.logrobus.core.network

/**
 * Stop and line ids, which each source writes its own way (spec §5.3). Here, so the transit mappers and the planner's
 * read them alike.
 */
object IdParser {
    // OTP's agency, the only one the source serves. Another prefix would be another feed's ids, which nothing guesses.
    private const val AGENCY_PREFIX = "1:"

    /** DD-01: `152` (API-02), `"0632"` (VM), `"38"` (ET) and `"1:100"` (OTP) are all stops 152, 632, 38 and 100. */
    fun stopId(raw: String?): Int? = number(raw)

    /**
     * DD-02: `33` (API-01), `"1"` (VM and ET) and `"1:5-desc"` (OTP) are all lines 33, 1 and 5. DD-06: VM's
     * `"[object Object]"`, like anything else that is not a number, is `null`.
     */
    fun lineId(raw: String?): Int? = number(raw?.substringBefore('-'))

    private fun number(raw: String?): Int? = raw.trimmedOrNull()?.removePrefix(AGENCY_PREFIX)?.toWholeNumber()

    // Digits only: toIntOrNull alone would also take a sign.
    private fun String.toWholeNumber(): Int? = if (isNotEmpty() && all { it in '0'..'9' }) toIntOrNull() else null
}
