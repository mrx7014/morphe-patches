/* Copyright 2026 Morphe. */
package app.morphe.patches.youtube.layout.hide.search

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.youtube.layout.hide.general.hideLayoutComponentsPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen

@Suppress("unused")
val searchCleanupPatch = bytecodePatch(
    name = "Search cleanup",
    description = "Adds controls for hiding Shorts, live content, channels, playlists, and other search clutter."
) {
    dependsOn(hideLayoutComponentsPatch)
    execute {
        PreferenceScreen.FEED.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_search_cleanup_screen",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("morphe_hide_shorts_search"),
                    SwitchPreference("morphe_hide_shorts_home"),
                    SwitchPreference("morphe_hide_web_search_results")
                )
            )
        )
    }
}
