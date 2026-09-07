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
 * Permanently hides content from channels identified by their stable YouTube channel ID.
 *
 * The setting accepts one value per line in either of these forms:
 * <ul>
 *     <li>UCxxxxxxxxxxxxxxxxxxxxxx</li>
 *     <li>https://www.youtube.com/channel/UCxxxxxxxxxxxxxxxxxxxxxx</li>
 * </ul>
 * Display names and handles are intentionally not accepted: they are not unique and can
 * change. The ID is searched in the serialized YouTube element buffer, where it is emitted as
 * channelId, browseId, or part of a channel URL.
 */
@SuppressWarnings({"unused", "unchecked"})
public final class BlockChannelsFilter extends BufferPhraseFilter {
    private static final Pattern CHANNEL_ID_PATTERN =
            Pattern.compile("(?:https?://(?:www\\.)?youtube\\.com/channel/)?(UC[A-Za-z0-9_-]{22})");

    private final StringFilterGroup commentsFilter = new StringFilterGroup(
            Settings.BLOCK_CHANNELS,
            "comment_thread.eml"
    );

    private volatile String lastChannelsParsed;
    private volatile ByteTrieSearch channelIdSearch;

    private synchronized void parseChannels() {
        String rawChannels = Settings.BLOCK_CHANNELS_LIST.get();
        //noinspection StringEquality
        if (rawChannels == lastChannelsParsed) return;

        ByteTrieSearch search = new ByteTrieSearch();
        Set<String> channelIds = new LinkedHashSet<>();

        for (String entry : rawChannels.split("\\R")) {
            String value = entry.trim();
            if (value.isEmpty() || value.startsWith("#")) continue;

            Matcher matcher = CHANNEL_ID_PATTERN.matcher(value);
            if (!matcher.matches()) {
                Utils.showToastLong(str("morphe_block_channels_toast_invalid_id", value));
                continue;
            }
            channelIds.add(matcher.group(1));
        }

        for (String channelId : channelIds) {
            final String matchedId = channelId;
            TrieSearch.TriePatternMatchedCallback<byte[]> callback =
                    (textSearched, startIndex, matchLength, callbackParameter) -> {
                        MutableReference<String> reference =
                                (MutableReference<String>) callbackParameter;
                        reference.value = matchedId;
                        Logger.printDebug(() -> "Matched blocked channel ID: " + matchedId);
                        return true;
                    };
            search.addPattern(channelId.getBytes(StandardCharsets.UTF_8), callback);
        }

        channelIdSearch = search;
        lastChannelsParsed = rawChannels;
        Logger.printDebug(() -> "Blocking channels by ID: " + channelIds);
    }

    public BlockChannelsFilter() {
        super();
        addPathCallbacks(commentsFilter);
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
        ByteTrieSearch search = channelIdSearch;
        if (search == null) return null;

        MutableReference<String> matchRef = new MutableReference<>();
        return search.matches(buffer, matchRef) ? matchRef.value : null;
    }
}
