package org.atlasapi.remotesite.channel4.epg;

import java.util.Map;

import org.atlasapi.media.channel.Channel;
import org.joda.time.Interval;

public interface BroadcastTrimmer {

    void trimBroadcasts(Interval scheduleInterval, Channel channel, Map<String, String> acceptableIds);
    
}