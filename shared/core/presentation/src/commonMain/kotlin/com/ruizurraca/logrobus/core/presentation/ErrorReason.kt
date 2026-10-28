package com.ruizurraca.logrobus.core.presentation

import com.ruizurraca.logrobus.core.common.AppError

/** FR-ERR-05: the short reason an Error state gives, beside its Retry action. Each platform words it. */
enum class ErrorReason {
    NETWORK,
    SERVER,
    DATA,
    ;

    companion object {
        fun of(error: AppError): ErrorReason = DATA
    }
}
