package com.yt.data.shorts

/**
 * Drops Shorts the viewer has already seen, but never drops all of them.
 *
 * Filtering used to be a single pass that returned whatever survived — including nothing. A source
 * that hands back thirty Shorts the viewer has watched before then produced an empty feed, which is
 * indistinguishable on screen from the source having failed, and which happens more often the more
 * the app is used. Re-seeing something is a far smaller cost than an empty screen, so the strictest
 * pass that still leaves something is the one that wins:
 *
 *  1. everything unwatched and unseen — the ideal, used whenever it is not empty
 *  2. unwatched only, allowing Shorts merely shown in the last week back in
 *  3. unfiltered
 *
 * [keepId] survives every pass: it is the Short the viewer asked for by name, and having watched it
 * once is no reason to refuse to open it again.
 */
internal fun <T> freshestNonEmpty(
    items: List<T>,
    id: (T) -> String,
    watchedIds: Set<String>,
    seenIds: Set<String>,
    keepId: String? = null,
): List<T> {
    if (items.isEmpty()) return items
    if (watchedIds.isEmpty() && seenIds.isEmpty()) return items

    fun keep(
        item: T,
        excluded: Set<String>,
    ): Boolean {
        val itemId = id(item)
        return itemId == keepId || itemId !in excluded
    }

    val unseenAndUnwatched = items.filter { keep(it, watchedIds + seenIds) }
    if (unseenAndUnwatched.isNotEmpty()) return unseenAndUnwatched

    val unwatched = items.filter { keep(it, watchedIds) }
    if (unwatched.isNotEmpty()) return unwatched

    return items
}
