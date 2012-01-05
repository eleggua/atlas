package org.atlasapi.remotesite.pa;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.ScheduleEntry.ItemRefAndBroadcast;
import org.atlasapi.remotesite.pa.bindings.ProgData;
import org.joda.time.DateTimeZone;

import com.metabroadcast.common.time.Timestamp;

public interface PaProgDataProcessor {

    public ItemRefAndBroadcast process(ProgData progData, Channel channel, DateTimeZone zone, Timestamp updatedAt);

}