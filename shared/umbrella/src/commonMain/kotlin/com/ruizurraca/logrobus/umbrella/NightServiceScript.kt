package com.ruizurraca.logrobus.umbrella

import com.ruizurraca.logrobus.core.config.CompiledDefaultsRemoteConfig
import com.ruizurraca.logrobus.feature.stops.domain.NightServiceText
import com.ruizurraca.logrobus.feature.stops.domain.NightServiceTextOf

/**
 * LB-206: the night-service sentence a preview or a Swift test reads: the one compiled into the app, which a screen
 * shows when remote config is unreachable (NFR-REL-05). Its own class, as the saved stops' script is, so
 * `PreviewUseCases` keeps within detekt's function limit.
 */
internal class NightServiceScript : NightServiceUseCases {
    override fun nightServiceText(): NightServiceText = NightServiceTextOf(CompiledDefaultsRemoteConfig())
}
