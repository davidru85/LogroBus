package com.ruizurraca.logrobus.core.common.color

/**
 * DD-08: a line colour as the source writes it, `rgba(15,220,252,1)` in API-01 or `#0FDCFC` in API-07, as an ARGB
 * value; `null` when it is missing or malformed.
 */
fun parseCssColor(raw: String?): Long? = raw?.length?.toLong() // TDD: red
