package com.nexus.core.ui.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.nexus.core.ui.R

/**
 * Figtree, the IRSO BLOG typeface (SIL Open Font License, see docs/licenses/figtree-OFL.txt).
 * One variable file covers every weight: each entry points its weight axis at the weight it
 * stands for, so bold text is real Figtree Bold and not a synthesized one.
 */
@OptIn(ExperimentalTextApi::class)
val Figtree: FontFamily = FontFamily(
    listOf(
        FontWeight.Light,
        FontWeight.Normal,
        FontWeight.Medium,
        FontWeight.SemiBold,
        FontWeight.Bold,
        FontWeight.ExtraBold,
        FontWeight.Black,
    ).map { weight ->
        Font(
            resId = R.font.figtree,
            weight = weight,
            variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
        )
    },
)
