package com.yt.widget.core

import com.google.common.truth.Truth.assertThat
import com.yt.data.local.decodeCustomThemePalettes
import com.yt.data.local.encodeCustomThemePalettes
import com.yt.ui.theme.CustomColorRole
import com.yt.ui.theme.CustomThemeColors
import com.yt.ui.theme.CustomThemePalettes
import com.yt.ui.theme.ThemeMode
import com.yt.ui.theme.ThemeVariant
import org.junit.Test

class WidgetThemeSignatureTest {
    private fun signatureOf(palettes: CustomThemePalettes) =
        WidgetThemeSignature(
            themeMode = ThemeMode.CUSTOM,
            themeVariant = ThemeVariant.DARK,
            customThemePalettes = palettes,
            systemLightThemeMode = ThemeMode.LIGHT,
            systemDarkThemeMode = ThemeMode.DARK,
            systemDarkThemeVariant = ThemeVariant.DARK,
        )

    @Test
    fun `a persisted palette signs without a cast failure`() {
        val stored =
            encodeCustomThemePalettes(
                CustomThemePalettes().withPalette(
                    ThemeVariant.DARK,
                    CustomThemeColors.default(ThemeVariant.DARK).withColor(CustomColorRole.PRIMARY, 0xFF00FF00),
                ),
            )

        val signature = signatureOf(decodeCustomThemePalettes(raw = stored, legacyRaw = null)).persistedForm()

        assertThat(signature).contains("PRIMARY=4278255360")
    }

    @Test
    fun `the same palette signs the same way twice`() {
        val palettes = CustomThemePalettes()

        assertThat(signatureOf(palettes).persistedForm()).isEqualTo(signatureOf(palettes).persistedForm())
    }

    @Test
    fun `a changed colour changes the signature`() {
        val before = signatureOf(CustomThemePalettes()).persistedForm()
        val after =
            signatureOf(
                CustomThemePalettes().withPalette(
                    ThemeVariant.AMOLED,
                    CustomThemeColors.default(ThemeVariant.AMOLED).withColor(CustomColorRole.SURFACE, 0xFF123456),
                ),
            ).persistedForm()

        assertThat(after).isNotEqualTo(before)
    }
}
