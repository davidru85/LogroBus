package com.ruizurraca.logrobus.android.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme

/**
 * Every brand value of the Android app (NFR-MNT-04, design_guidelines.md §4 and §6.1). Until OQ-18's designs arrive
 * (LB-413), the palette is a neutral placeholder: greys plus one accessible accent. It is defined here and nowhere else.
 */
object Tokens {
    val lightColors: ColorScheme = lightColorScheme()
    val darkColors: ColorScheme = darkColorScheme()
}
