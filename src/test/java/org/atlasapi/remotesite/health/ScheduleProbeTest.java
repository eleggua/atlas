package org.atlasapi.remotesite.health;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.List;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.Interval;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.TimeMachine;

public class ScheduleProbeTest extends TestCase {

	private static final Channel CHANNEL4 = new Channel(Publisher.METABROADCAST, "Channel 4", "c4", MediaType.AUDIO, "http://channel4.com");

    private final TimeMachine clock = new TimeMachine();
    private final Mockery context = new Mockery();
    private final ScheduleResolver scheduleResolver = context.mock(ScheduleResolver.class);
    private final Set<Channel> channels = ImmutableSet.of(CHANNEL4);
    private final Set<Publisher> publishers = ImmutableSet.of(Publisher.C4);

    public void testProbeProducesFailureResultWhenNoEntriesInSchedule() throws Exception {

        clock.jumpTo(new DateTime(500, DateTimeZones.UTC));
        
        ScheduleProbe probe = new ScheduleProbe(Publisher.C4, CHANNEL4, scheduleResolver, clock);
        
        context.checking(new Expectations(){{
            one(scheduleResolver).schedule(with(any(DateTime.class)), with(any(DateTime.class)), with(channels), with(publishers));
                    will(returnValue(schedule(CHANNEL4, ImmutableList.<Item>of(), dayIntervalAround(clock.now()))));
        }});
        
        ProbeResult result = probe.probe();
        
        assertThat(result.isFailure(), is(true));
        assertThat(Iterables.size(result.entries()), is(1));
    }
    
    public void testScheduleWithNoGapsPasses() throws Exception {
        
        clock.jumpTo(new DateTime(500, DateTimeZones.UTC));
        
        ScheduleProbe probe = new ScheduleProbe(Publisher.C4, CHANNEL4, scheduleResolver, clock);
        
                                    //start, duration in ms
        final Item item1 = broadcastItem(10, 10);
        final Item item2 = broadcastItem(22, 10);
        
        context.checking(new Expectations(){{
            one(scheduleResolver).schedule(with(any(DateTime.class)), with(any(DateTime.class)), with(channels), with(publishers));
            will(returnValue(schedule(CHANNEL4, ImmutableList.<Item>of(item1, item2), dayIntervalAround(clock.now()))));
        }});
        
        ProbeResult result = probe.probe();
        
        assertThat(result.isFailure(), is(false));
    }
    
    public void testScheduleWithGapsFails() throws Exception {
        
        clock.jumpTo(new DateTime(500, DateTimeZones.UTC));
        
        ScheduleProbe probe = new ScheduleProbe(Publisher.C4, CHANNEL4, scheduleResolver, clock);
        
        final Item item1 = broadcastItem(10, 10);
        final Item item2 = broadcastItem(300022, 10); //starts 5mins and 2 millis after
        
        context.checking(new Expectations(){{
            one(scheduleResolver).schedule(with(any(DateTime.class)), with(any(DateTime.class)), with(channels), with(publishers));
            will(returnValue(schedule(CHANNEL4, ImmutableList.<Item>of(item1, item2), dayIntervalAround(clock.now()))));
        }});
        
        ProbeResult result = probe.probe();
        
        assertThat(result.isFailure(), is(true));
        assertThat(Iterables.get(result.entries(),0).isFailure(), is(false));
        assertThat(Iterables.get(result.entries(),1).isFailure(), is(true));
        assertThat(Iterables.get(result.entries(),2).isFailure(), is(false)); 
        assertThat(Iterables.get(result.entries(),3).isFailure(), is(false)); 
    }
    
    public void testScheduleWithOverlapsFails() throws Exception {
        
        clock.jumpTo(new DateTime(500, DateTimeZones.UTC));
        
        ScheduleProbe probe = new ScheduleProbe(Publisher.C4, CHANNEL4, scheduleResolver, clock);
        
        final Item item1 = broadcastItem(10, 10);
        final Item item2 = broadcastItem(18, 10); //previous ends at 20
        
        context.checking(new Expectations(){{
            one(scheduleResolver).schedule(with(any(DateTime.class)), with(any(DateTime.class)), with(channels), with(publishers));
            will(returnValue(schedule(CHANNEL4, ImmutableList.<Item>of(item1, item2), dayIntervalAround(clock.now()))));
        }});
        
        ProbeResult result = probe.probe();
        
        assertThat(result.isFailure(), is(true));
        assertThat(Iterables.get(result.entries(),0).isFailure(), is(false));
        assertThat(Iterables.get(result.entries(),1).isFailure(), is(false));
        assertThat(Iterables.get(result.entries(),2).isFailure(), is(true)); 
        assertThat(Iterables.get(result.entries(),3).isFailure(), is(false)); 
    }
    
    public void testScheduleWithStaleItemsFails() throws Exception {
        
        clock.jumpTo(new DateTime(3600020, DateTimeZones.UTC));
        
        ScheduleProbe probe = new ScheduleProbe(Publisher.C4, CHANNEL4, scheduleResolver, clock);
        
        final Item item1 = broadcastItem(10, 10, 10);
        final Item item2 = broadcastItem(22, 10, 10);
        
        context.checking(new Expectations(){{
            one(scheduleResolver).schedule(with(any(DateTime.class)), with(any(DateTime.class)), with(channels), with(publishers));
            will(returnValue(schedule(CHANNEL4, ImmutableList.<Item>of(item1, item2), dayIntervalAround(clock.now()))));
        }});
        
        ProbeResult result = probe.probe();
        
        assertThat(result.isFailure(), is(true));
        assertThat(Iterables.get(result.entries(),0).isFailure(), is(false));
        assertThat(Iterables.get(result.entries(),1).isFailure(), is(false));
        assertThat(Iterables.get(result.entries(),2).isFailure(), is(false));
        assertThat(Iterables.get(result.entries(),3).isFailure(), is(true)); 
    }
    
    private Item broadcastItem(long start, long duration) {
        return broadcastItem(start, duration, start);
    }
        
    private Item broadcastItem(long start, long duration, long lastFetched) {
        Item item = new Item("item"+start, "curie"+start, Publisher.C4);
        item.setLastFetched(new DateTime(lastFetched, DateTimeZones.UTC));
        
        Version version = new Version();
        Broadcast broadcast = new Broadcast(CHANNEL4.uri(), new DateTime(start, DateTimeZones.UTC), new Duration(duration));

        version.addBroadcast(broadcast);
        item.addVersion(version);

        return item;
    }

    private Interval dayIntervalAround(DateTime now) {
        DateTime start = now.toLocalDate().toDateTimeAtStartOfDay(DateTimeZones.UTC);
        return new Interval(start, start.plusDays(1));
    }
    
    private Schedule schedule(Channel channel, List<Item> items, Interval interval) {
        Map<Channel, List<Item>> channelMap = Maps.newHashMap();
        channelMap.put(channel, items);
        return Schedule.fromChannelMap(channelMap, interval);
    }
}
