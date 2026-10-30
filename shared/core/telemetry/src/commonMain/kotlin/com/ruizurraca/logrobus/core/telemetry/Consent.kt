package com.ruizurraca.logrobus.core.telemetry

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The user's answer to one consent question: none yet, yes or no. */
enum class ConsentDecision {
    UNDECIDED,
    GRANTED,
    DENIED,
}

/**
 * The user's consent to each kind of telemetry, asked separately (NFR-PRIV-03). Both start undecided: telemetry is
 * opt-in, so nothing is sent until the user says yes.
 */
data class TelemetryConsent(
    val analytics: ConsentDecision = ConsentDecision.UNDECIDED,
    val crashReports: ConsentDecision = ConsentDecision.UNDECIDED,
)

/** The user's telemetry consent, as it changes (ADR-0009). */
interface ConsentStore {
    val consent: StateFlow<TelemetryConsent>
}

/**
 * A [ConsentStore] held in memory, undecided at start. It forgets on restart: the consent dialog and the Settings
 * toggles of LB-409 will ask the user and persist the answer (ADR-0018).
 */
class MemoryConsentStore(
    initial: TelemetryConsent = TelemetryConsent(),
) : ConsentStore {
    private val state = MutableStateFlow(initial)

    override val consent: StateFlow<TelemetryConsent> = state.asStateFlow()

    fun update(consent: TelemetryConsent) {
        state.value = consent
    }
}
