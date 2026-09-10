/* Copyright 2026 Morphe. */
package app.morphe.patches.youtube.layout.hide.metadata

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.youtube.layout.hide.general.hideLayoutComponentsPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen

@Suppress("unused")
val videoMetadataPatch = bytecodePatch(
    name = "Video metadata controls",
    description = "Adds a focused group of controls for hiding video timestamps, upload times, and view counts."
) {
    dependsOn(hideLayoutComponentsPatch)
    execute {
        PreferenceScreen.FEED.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_video_metadata_screen",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("morphe_hide_view_count"),
                    SwitchPreference("morphe_hide_upload_time"),
                    SwitchPreference("morphe_hide_timestamp")
                )
            )
        )
    }
}
