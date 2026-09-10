/*
 * Copyright 2026 Morphe.
 * https://github.com/mrx7014/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.patches.youtube.layout.hide.ai

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.youtube.layout.buttons.action.hideVideoActionButtonsPatch
import app.morphe.patches.youtube.layout.hide.general.hideLayoutComponentsPatch
import app.morphe.patches.youtube.misc.settings.PreferenceScreen
import app.morphe.patches.youtube.misc.settings.settingsPatch
import app.morphe.patches.shared.misc.settings.preference.NonInteractivePreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceCategory
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference
import app.morphe.patches.shared.misc.settings.preference.PreferenceScreenPreference.Sorting
import app.morphe.patches.shared.misc.settings.preference.SwitchPreference
import app.morphe.patches.youtube.shared.Constants.COMPATIBILITY_YOUTUBE

@Suppress("unused")
val hideAIFeaturesPatch = bytecodePatch(
    name = "Hide AI features",
    description = "Adds one option to hide YouTube AI sections, summaries, Ask buttons, and AI entry points."
) {
    dependsOn(
        settingsPatch,
        hideLayoutComponentsPatch,
        hideVideoActionButtonsPatch
    )
    compatibleWith(COMPATIBILITY_YOUTUBE)
    execute {
        PreferenceScreen.FEED.addPreferences(
            PreferenceScreenPreference(
                key = "morphe_hide_ai_features_screen",
                sorting = Sorting.UNSORTED,
                preferences = setOf(
                    SwitchPreference("morphe_hide_ai_features"),
                    PreferenceCategory(
                        key = "morphe_hide_ai_features_about_category",
                        sorting = Sorting.UNSORTED,
                        preferences = setOf(
                            NonInteractivePreference(
                                key = "morphe_hide_ai_features_about",
                                titleKey = "morphe_hide_ai_features_about_title"
                            )
                        )
                    )
                )
            )
        )
    }
}
