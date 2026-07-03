package com.clover.updater.theme

import android.graphics.Typeface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.Typeface as ComposeTypeface
import com.android.settingslib.spa.framework.theme.SettingsTheme

val DisplayLargeFont = FontFamily(ComposeTypeface(Typeface.create("variable-display-large", Typeface.NORMAL)))
val DisplayMediumFont = FontFamily(ComposeTypeface(Typeface.create("variable-display-medium", Typeface.NORMAL)))
val DisplaySmallFont = FontFamily(ComposeTypeface(Typeface.create("variable-display-small", Typeface.NORMAL)))

val HeadlineLargeFont = FontFamily(ComposeTypeface(Typeface.create("variable-headline-large", Typeface.NORMAL)))
val HeadlineMediumFont = FontFamily(ComposeTypeface(Typeface.create("variable-headline-medium", Typeface.NORMAL)))
val HeadlineSmallFont = FontFamily(ComposeTypeface(Typeface.create("variable-headline-small", Typeface.NORMAL)))

val TitleLargeFont = FontFamily(ComposeTypeface(Typeface.create("variable-title-large", Typeface.NORMAL)))
val TitleMediumFont = FontFamily(ComposeTypeface(Typeface.create("variable-title-medium", Typeface.NORMAL)))
val TitleSmallFont = FontFamily(ComposeTypeface(Typeface.create("variable-title-small", Typeface.NORMAL)))

val BodyLargeFont = FontFamily(ComposeTypeface(Typeface.create("variable-body-large", Typeface.NORMAL)))
val BodyMediumFont = FontFamily(ComposeTypeface(Typeface.create("variable-body-medium", Typeface.NORMAL)))
val BodySmallFont = FontFamily(ComposeTypeface(Typeface.create("variable-body-small", Typeface.NORMAL)))

val LabelLargeFont = FontFamily(ComposeTypeface(Typeface.create("variable-label-large", Typeface.NORMAL)))
val LabelMediumFont = FontFamily(ComposeTypeface(Typeface.create("variable-label-medium", Typeface.NORMAL)))
val LabelSmallFont = FontFamily(ComposeTypeface(Typeface.create("variable-label-small", Typeface.NORMAL)))

@Composable
fun UpdaterTheme(content: @Composable () -> Unit) {
    SettingsTheme {
        val defaultTypography = MaterialTheme.typography
        val customTypography = androidx.compose.material3.Typography(
            displayLarge = defaultTypography.displayLarge.copy(fontFamily = DisplayLargeFont),
            displayMedium = defaultTypography.displayMedium.copy(fontFamily = DisplayMediumFont),
            displaySmall = defaultTypography.displaySmall.copy(fontFamily = DisplaySmallFont),
            headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = HeadlineLargeFont),
            headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = HeadlineMediumFont),
            headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = HeadlineSmallFont),
            titleLarge = defaultTypography.titleLarge.copy(fontFamily = TitleLargeFont),
            titleMedium = defaultTypography.titleMedium.copy(fontFamily = TitleMediumFont),
            titleSmall = defaultTypography.titleSmall.copy(fontFamily = TitleSmallFont),
            bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = BodyLargeFont),
            bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = BodyMediumFont),
            bodySmall = defaultTypography.bodySmall.copy(fontFamily = BodySmallFont),
            labelLarge = defaultTypography.labelLarge.copy(fontFamily = LabelLargeFont),
            labelMedium = defaultTypography.labelMedium.copy(fontFamily = LabelMediumFont),
            labelSmall = defaultTypography.labelSmall.copy(fontFamily = LabelSmallFont)
        )
        MaterialTheme(
            colorScheme = MaterialTheme.colorScheme,
            typography = customTypography,
            content = content
        )
    }
}
