/* Copyright 2026 Morphe. */
package app.morphe.patches.youtube.layout.hide.channel

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.youtube.layout.hide.general.hideLayoutComponentsPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen

@Suppress("unused")
val channelPageSectionsPatch = bytecodePatch(
    name = "Channel page sections",
    description = "Adds controls for hiding tabs and sections on channel pages."
) {
    dependsOn(hideLayoutComponentsPatch)
    execute {
        PreferenceScreen.FEED.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_channel_page_sections_screen",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("morphe_hide_channel_tab"),
                    SwitchPreference("morphe_hide_channel_links_section"),
                    SwitchPreference("morphe_hide_featured_playlists_section"),
                    SwitchPreference("morphe_hide_featured_videos_section")
                )
            )
        )
    }
}
