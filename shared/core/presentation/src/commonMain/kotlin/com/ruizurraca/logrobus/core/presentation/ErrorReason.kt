package com.ruizurraca.logrobus.core.presentation

import com.ruizurraca.logrobus.core.common.AppError

/** FR-ERR-05: the short reason an Error state gives, beside its Retry action. Each platform words it. */
enum class ErrorReason {
    NETWORK,
    SERVER,
    DATA,
    ;

    companion object {
        fun of(error: AppError): ErrorReason =
            when (error) {
                AppError.Network -> NETWORK

                is AppError.Server -> SERVER

                // A screen that treats a missing resource as Empty maps NotFound before it gets here.
                AppError.Data, AppError.NotFound -> DATA
            }
    }
}
