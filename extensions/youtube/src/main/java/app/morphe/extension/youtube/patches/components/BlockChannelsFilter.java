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
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import app.morphe.extension.shared.ByteTrieSearch;
import app.morphe.extension.shared.Logger;
import app.morphe.extension.shared.TrieSearch;
import app.morphe.extension.shared.Utils;
import app.morphe.extension.shared.patches.components.BufferPhraseFilter;
import app.morphe.extension.shared.patches.components.StringFilterGroup;
import app.morphe.extension.youtube.settings.Settings;

/**
 * Permanently hides content and channel results identified by a YouTube channel ID or handle.
 *
 * <p>One value can be entered per line in any of these forms:
 * <ul>
 *     <li>UCxxxxxxxxxxxxxxxxxxxxxx</li>
 *     <li>https://www.youtube.com/channel/UCxxxxxxxxxxxxxxxxxxxxxx</li>
 *     <li>@channel_handle</li>
 *     <li>https://www.youtube.com/@channel_handle</li>
 * </ul>
 * Stable channel IDs are preferred, but handles make the feature practical when copying a
 * channel link from the YouTube app. The ID and handle are searched in serialized YouTube
 * element buffers, including video cards and channel search-result cards.
 */
@SuppressWarnings({"unused", "unchecked"})
public final class BlockChannelsFilter extends BufferPhraseFilter {
    private static final Pattern CHANNEL_ID_PATTERN =
            Pattern.compile("UC[A-Za-z0-9_-]{22}");
    private static final Pattern HANDLE_PATTERN =
            Pattern.compile("@[A-Za-z0-9._-]{3,30}");
    private static final Pattern HANDLE_URL_PATTERN =
            Pattern.compile("https?://(?:www\\.)?youtube\\.com/(@[A-Za-z0-9._-]{3,30})(?:[/?#].*)?",
                    Pattern.CASE_INSENSITIVE);

    private final StringFilterGroup commentsFilter = new StringFilterGroup(
            Settings.BLOCK_CHANNELS,
            "comment_thread.eml"
    );

    /* Search-result channel cards use different identifiers from video cards across app versions. */
    private final StringFilterGroup channelResultFilter = new StringFilterGroup(
            Settings.BLOCK_CHANNELS,
            "channel_result",
            "channel_renderer",
            "search_channel",
            "channel_lockup",
            "compact_channel_bar.e"
    );

    private volatile String lastChannelsParsed;
    private volatile ByteTrieSearch channelSearch;

    private static String normalizeEntry(String value) {
        Matcher idMatcher = CHANNEL_ID_PATTERN.matcher(value);
        if (idMatcher.find()) return idMatcher.group();

        Matcher handleUrlMatcher = HANDLE_URL_PATTERN.matcher(value);
        if (handleUrlMatcher.matches()) return handleUrlMatcher.group(1);

        Matcher handleMatcher = HANDLE_PATTERN.matcher(value);
        if (handleMatcher.matches()) return handleMatcher.group();

        return null;
    }

    private synchronized void parseChannels() {
        String rawChannels = Settings.BLOCK_CHANNELS_LIST.get();
        //noinspection StringEquality
        if (rawChannels == lastChannelsParsed) return;

        ByteTrieSearch search = new ByteTrieSearch();
        Set<String> channels = new LinkedHashSet<>();

        for (String entry : rawChannels.split("\\R")) {
            String value = entry.trim();
            if (value.isEmpty() || value.startsWith("#")) continue;

            String normalized = normalizeEntry(value);
            if (normalized == null) {
                Utils.showToastLong(str("morphe_block_channels_toast_invalid_id", value));
                continue;
            }
            channels.add(normalized);
        }

        for (String channel : channels) {
            final String matchedChannel = channel;
            TrieSearch.TriePatternMatchedCallback<byte[]> callback =
                    (textSearched, startIndex, matchLength, callbackParameter) -> {
                        MutableReference<String> reference =
                                (MutableReference<String>) callbackParameter;
                        reference.value = matchedChannel;
                        Logger.printDebug(() -> "Matched blocked channel: " + matchedChannel);
                        return true;
                    };
            search.addPattern(channel.getBytes(StandardCharsets.UTF_8), callback);
        }

        channelSearch = search;
        lastChannelsParsed = rawChannels;
        Logger.printDebug(() -> "Blocking channels: " + channels);
    }

    public BlockChannelsFilter() {
        super();
        addPathCallbacks(commentsFilter, channelResultFilter);
        parseChannels();
    }

    @Override
    protected void reparseIfNeeded() {
        //noinspection StringEquality
        if (Settings.BLOCK_CHANNELS_LIST.get() != lastChannelsParsed) {
            parseChannels();
        }
    }

    @Override
    protected boolean isActiveForFeedContext() {
        return Settings.BLOCK_CHANNELS.get();
    }

    @Override
    @Nullable
    protected String matchBuffer(byte[] buffer, StringFilterGroup matchedGroup) {
        ByteTrieSearch search = channelSearch;
        if (search == null) return null;

        MutableReference<String> matchRef = new MutableReference<>();
        return search.matches(buffer, matchRef) ? matchRef.value : null;
    }
}
