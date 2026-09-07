/*
 * Copyright 2026 Morphe.
 * https://github.com/mrx7014/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */

package app.morphe.extension.youtube.patches.components;

import static app.morphe.extension.shared.StringRef.str;

import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import app.morphe.extension.shared.ByteTrieSearch;
import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.TrieSearch;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.patches.components.BufferPhraseFilter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.youtube.settings.Settings;

/**
 * <pre>
 * Permanently hides all content from specific channels (by name or @handle) across
 * the Home feed, Subscriptions, Search results, related videos, Shorts shelves, and
 * comment threads.
 *
 * Unlike {@link KeywordContentFilter}, entries here are always matched as whole words/phrases
 * (channel identity, not free text) and are not scoped to individual feed tabs; if the block
 * list is not empty, filtering is active everywhere a video/channel buffer is scanned.
 *
 * Limitations (inherited from the underlying Litho buffer scanning approach used by every
 * filter in this project):
 * - Some Shorts cards inside search results do not include the channel name in their buffer,
 *   so a handful of Shorts entries from a blocked channel may still slip through.
 * - Opening the channel's page directly (by URL, deep link, or from an existing notification)
 *   is not blocked; this only hides the channel's content from feeds/search/comments, it does
 *   not prevent navigating to the channel directly.
 * - Entries are matched against whatever text YouTube's servers put in the buffer (channel name
 *   and/or @handle). If a channel changes its display name, the old entry will no longer match
 *   and the new name must be added.
 */
@SuppressWarnings({"unused", "unchecked"})
public final class BlockChannelsFilter extends BufferPhraseFilter {

    private final StringFilterGroup commentsFilter = new StringFilterGroup(
            Settings.BLOCK_CHANNELS,
            "comment_thread.eml"
    );

    /**
     * The last value of {@link Settings#BLOCK_CHANNELS_LIST}
     * parsed and loaded into {@link #bufferSearch}.
     * Allows changing the blocked channels without restarting the app.
     */
    private volatile String lastChannelsParsed;

    private volatile ByteTrieSearch bufferSearch;

    private synchronized void parseChannels() { // Must be synchronized since Litho is multithreaded.
        String rawChannels = Settings.BLOCK_CHANNELS_LIST.get();

        //noinspection StringEquality
        if (rawChannels == lastChannelsParsed) {
            Logger.printDebug(() -> "Using previously initialized search");
            return; // Another thread won the race, and search is already initialized.
        }

        ByteTrieSearch search = new ByteTrieSearch();
        String[] split = rawChannels.split("\n");
        if (split.length != 0) {
            // Linked set so log statements are organized and easy to read.
            Set<String> channelVariations = new LinkedHashSet<>(6 * split.length);

            for (String entry : split) {
                String channel = entry.stripLeading().stripTrailing();
                if (channel.isBlank()) continue;

                if (channel.length() < 2) {
                    // Do not reset the setting. Keep the entry so the user can fix the mistake.
                    Utils.showToastLong(str("morphe_block_channels_toast_invalid_length", channel));
                    continue;
                }

                // Channel identity should always match the whole name/handle, never a
                // substring of it, otherwise unrelated channels could be caught too.
                // Common casing that might appear in the buffer.
                Locale defaultLocale = Locale.getDefault();
                channelVariations.add(channel);
                channelVariations.add(channel.toLowerCase(Locale.ROOT));
                channelVariations.add(channel.toLowerCase(defaultLocale));
                channelVariations.add(channel.toUpperCase(Locale.ROOT));
                channelVariations.add(channel.toUpperCase(defaultLocale));
                channelVariations.add(titleCaseFirstWordOnly(channel));
                channelVariations.add(capitalizeAllFirstLetters(channel));

                // If the entry is a handle without the leading '@', also match it with one,
                // and vice versa, since the buffer may contain either form.
                if (channel.startsWith("@")) {
                    channelVariations.add(channel.substring(1));
                } else {
                    channelVariations.add("@" + channel);
                }
            }

            for (String variation : channelVariations) {
                TrieSearch.TriePatternMatchedCallback<byte[]> callback =
                        (textSearched, startIndex, matchLength, callbackParameter) -> {
                            if (!keywordMatchIsWholeWord(textSearched, startIndex, matchLength)) {
                                return false;
                            }

                            Logger.printDebug(() -> "Matched blocked channel: '" + variation + "'");
                            // noinspection unchecked
                            ((MutableReference<String>) callbackParameter).value = variation;
                            return true;
                        };
                byte[] stringBytes = variation.getBytes(StandardCharsets.UTF_8);
                search.addPattern(stringBytes, callback);
            }

            Logger.printDebug(() -> "Blocking channels: (" + search.getEstimatedMemorySize()
                    + " KB) entries: " + channelVariations);
        }

        bufferSearch = search;
        lastChannelsParsed = rawChannels; // Must set last.
    }

    public BlockChannelsFilter() {
        super(); // commentsFilter is registered below because instance fields initialize after super().
        addPathCallbacks(commentsFilter);
        // Channels are parsed on first call to isFiltered().
    }

    @Override
    protected void reparseIfNeeded() {
        // Field is intentionally compared using reference equality.
        //noinspection StringEquality
        if (Settings.BLOCK_CHANNELS_LIST.get() != lastChannelsParsed) {
            // User changed the blocked channels list.
            parseChannels();
        }
    }

    @Override
    protected boolean isActiveForFeedContext() {
        // No per-tab scoping: a blocked channel should never appear, anywhere.
        return Settings.BLOCK_CHANNELS.get();
    }

    @Override
    @Nullable
    protected String matchBuffer(byte[] buffer, StringFilterGroup matchedGroup) {
        ByteTrieSearch search = bufferSearch;
        if (search == null) return null;
        MutableReference<String> matchRef = new MutableReference<>();
        if (!search.matches(buffer, matchRef)) return null;
        return matchRef.value;
    }
}
