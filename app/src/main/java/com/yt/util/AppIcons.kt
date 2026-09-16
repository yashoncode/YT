package com.yt.util

object AppIcons {
    /** Component name prefix shared by every alias (the application id namespace). */
    const val NAMESPACE = "com.yt"

    /** The alias enabled by default in the manifest. Used as a safe fallback. */
    const val DEFAULT_SUFFIX = ".IconYTRed"

    /** Every launcher alias suffix, in manifest declaration order. */
    val ALL_SUFFIXES = listOf(
        ".IconYTRed",
        ".IconYTLight",
        ".IconYTPlay",
        ".IconAmoled",
        ".IconMonochrome",
        ".IconGhost",
        ".IconDynamic",
        ".IconMaterialSky",
        ".IconMaterialMint"
    )
}
