package org.atlasapi.remotesite.pa;

import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;

import java.util.HashSet;
import java.util.Set;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.ScheduleEntry.ItemRefAndBroadcast;
import org.atlasapi.persistence.content.schedule.mongo.ScheduleWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.remotesite.channel4.epg.BroadcastTrimmer;
import org.atlasapi.remotesite.pa.PaBaseProgrammeUpdater.PaChannelData;
import org.atlasapi.remotesite.pa.bindings.ProgData;
import org.joda.time.Interval;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;

public class PaChannelProcessor {

    private final PaProgDataProcessor processor;
    private final BroadcastTrimmer trimmer;
    private final ScheduleWriter scheduleWriter;
    private final AdapterLog log;

    public PaChannelProcessor(PaProgDataProcessor processor, BroadcastTrimmer trimmer, ScheduleWriter scheduleWriter, AdapterLog log) {
        this.processor = processor;
        this.trimmer = trimmer;
        this.scheduleWriter = scheduleWriter;
        this.log = log;
    }

    public int process(PaChannelData channelData, Set<String> currentlyProcessing) {
        int processed = 0;
        Set<ItemRefAndBroadcast> broadcasts = new HashSet<ItemRefAndBroadcast>();
        Channel channel = channelData.channel();
        try {
            Builder<String, String> acceptableBroadcastIds = ImmutableMap.builder();
            for (ProgData programme : channelData.programmes()) {
                String programmeLock = lockIdentifier(programme);
                lock(currentlyProcessing, programmeLock);
                try {
                    ItemRefAndBroadcast itemAndBroadcast = processor.process(programme, channel, channelData.zone(), channelData.lastUpdated());
                    if(itemAndBroadcast != null) {
	                    broadcasts.add(itemAndBroadcast);
	                    acceptableBroadcastIds.put(itemAndBroadcast.getBroadcast().getSourceId(),itemAndBroadcast.getItemUri());
                    }
                    processed++;
                } catch (Exception e) {
                    log.record(errorEntry().withCause(e).withSource(getClass()).withDescription("Error processing channel %s, prog id %s", channel.key(), programme.getProgId()));
                } finally {
                    unlock(currentlyProcessing, programmeLock);
                }
            }
            if (trimmer != null) {
                trimmer.trimBroadcasts(new Interval(channelData.day(), channelData.day().plusDays(1)), channel, acceptableBroadcastIds.build());
            }
            scheduleWriter.replaceScheduleBlock(Publisher.PA, channel, broadcasts);
            
        } catch (Exception e) {
            log.record(errorEntry().withCause(e).withSource(getClass()).withDescription("Error processing channel %s", channel.key()));
        }
        return processed;
    }

    private void unlock(Set<String> currentlyProcessing, String programmeLock) {
        synchronized (currentlyProcessing) {
            currentlyProcessing.remove(programmeLock);
            currentlyProcessing.notifyAll();
        }
    }

    private void lock(Set<String> currentlyProcessing, String programmeLock) throws InterruptedException {
        synchronized (currentlyProcessing) {
            while (currentlyProcessing.contains(programmeLock)) {
                currentlyProcessing.wait();
            }
            currentlyProcessing.add(programmeLock);
        }
    }

    private String lockIdentifier(ProgData programme) {
        return Strings.isNullOrEmpty(programme.getSeriesId()) ? programme.getProgId() : programme.getSeriesId();
    }
}
