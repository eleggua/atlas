package org.atlasapi.query.content.schedule;

import java.util.List;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.ScheduleEntry;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.logging.SystemOutAdapterLog;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.joda.time.DateTime;
import org.joda.time.Interval;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.time.DateTimeZones;

public class ScheduleOverlapResolverTest extends MockObjectTestCase {

    private final ScheduleResolver scheduleResolver = mock(ScheduleResolver.class);
    private final ScheduleOverlapListener listener = mock(ScheduleOverlapListener.class);
    
    private final Channel channel = new Channel(Publisher.METABROADCAST, "BBC One", "bbcone", MediaType.VIDEO, "http://www.bbc.co.uk/bbcone");
    
    private final Publisher publisher = Publisher.BBC;
    private final DateTime now = new DateTime(DateTimeZones.UTC);
    
    private final Broadcast b1 = new Broadcast(channel.uri(), now, now.plusMinutes(30)).withId("1");
    private final Broadcast b2 = new Broadcast(channel.uri(), now.plusMinutes(30), now.plusMinutes(30)).withId("2");
    private final Broadcast b3 = new Broadcast(channel.uri(), now.plusMinutes(30), now.plusMinutes(60)).withId("3");
    private final Broadcast b4 = new Broadcast(channel.uri(), now.plusMinutes(60), now.plusMinutes(120)).withId("4");
    
    private final Item item = new Item("item1", "item1", publisher);
    
    private final DateTime from = now;
    private final DateTime to = now.plusMinutes(60);
    private final Interval interval = new Interval(from, to);
    private final List<Channel> channels = ImmutableList.of(channel);
    private final List<Publisher> publishers = ImmutableList.of(publisher);
    
    private final Schedule schedule = schedule(channel, item(b1), item(b2), item(b3), item(b4));
    
    private final ScheduleOverlapResolver resolver = new ScheduleOverlapResolver(scheduleResolver, listener, new SystemOutAdapterLog());
    
    public void testBroadcastsShouldNotOverlap() {
        checking(new Expectations() {{ 
            one(scheduleResolver).schedule(from, to, channels, publishers); will(returnValue(schedule));
        }});
        
        Schedule result = resolver.schedule(from, to, channels, publishers);
        assertSchedules(schedule, result);
    }
    
    public void testOverlappingWithIdBroadcast() {
        final Broadcast overlap = new Broadcast(channel.uri(), now.plusMinutes(35), now.plusMinutes(60)).withId("3");
        overlap.setLastUpdated(now);
        b3.setLastUpdated(now.plusMinutes(1));
        final Item overlappingItem = item(overlap);
        
        final Schedule overlappingSchedule = schedule(channel, item(b1), item(b2), item(b3), overlappingItem, item(b4));
        
        checking(new Expectations() {{ 
            one(scheduleResolver).schedule(from, to, channels, publishers); will(returnValue(overlappingSchedule));
            one(listener).itemRemovedFromSchedule(overlappingItem, overlap);
        }});
        
        Schedule result = resolver.schedule(from, to, channels, publishers);
        assertSchedules(schedule, result);
    }
    
    public void testOverlappingLastWithIdBroadcast() {
        final Broadcast overlap = new Broadcast(channel.uri(), now.plusMinutes(70), now.plusMinutes(80)).withId("4");
        overlap.setLastUpdated(now);
        b4.setLastUpdated(now.plusMinutes(1));
        final Item overlappingItem = item(overlap);
        
        final Schedule overlappingSchedule = schedule(channel, item(b1), item(b2), item(b3), item(b4), overlappingItem);
        
        checking(new Expectations() {{ 
            one(scheduleResolver).schedule(from, to, channels, publishers); will(returnValue(overlappingSchedule));
            one(listener).itemRemovedFromSchedule(overlappingItem, overlap);
        }});
        
        Schedule result = resolver.schedule(from, to, channels, publishers);
        assertSchedules(schedule, result);
    }
    
    public void testOverlappingBroadcastSwap() {
        final Broadcast overlap = new Broadcast(channel.uri(), now.plusMinutes(35), now.plusMinutes(70)).withId("somethingElse");
        final Item overlappingItem = item(overlap);
        
        final Schedule overlappingSchedule = schedule(channel, item(b1), item(b2), item(b3), overlappingItem, item(b4));
        
        checking(new Expectations() {{ 
            one(scheduleResolver).schedule(from, to, channels, publishers); will(returnValue(overlappingSchedule));
            one(listener).itemRemovedFromSchedule(overlappingItem, overlap);
        }});
        
        Schedule result = resolver.schedule(from, to, channels, publishers);
        assertSchedules(schedule, result);
    }
    
    public void testOverlappingEarlierBroadcastSwap() {
        final Broadcast overlap = new Broadcast(channel.uri(), now.plusMinutes(20), now.plusMinutes(70)).withId("somethingElse");
        final Item overlappingItem = item(overlap);
        
        final Schedule overlappingSchedule = schedule(channel, item(b1), overlappingItem, item(b2), item(b3), item(b4));
        
        checking(new Expectations() {{ 
            one(scheduleResolver).schedule(from, to, channels, publishers); will(returnValue(overlappingSchedule));
            one(listener).itemRemovedFromSchedule(overlappingItem, overlap);
        }});
        
        Schedule result = resolver.schedule(from, to, channels, publishers);
        assertSchedules(schedule, result);
    }
    
    public void testMultipleOverlappingBroadcastSwap() {
        final Broadcast overlap = new Broadcast(channel.uri(), now.plusMinutes(20), now.plusMinutes(70)).withId("somethingElse");
        final Item overlappingItem = item(overlap);
        final Broadcast overlap2 = new Broadcast(channel.uri(), now.plusMinutes(35), now.plusMinutes(75)).withId("somethingElse");
        final Item overlappingItem2 = item(overlap2);
        
        final Schedule overlappingSchedule = schedule(channel, item(b1), overlappingItem, item(b2), item(b3), overlappingItem2, item(b4));
        
        checking(new Expectations() {{ 
            one(scheduleResolver).schedule(from, to, channels, publishers); will(returnValue(overlappingSchedule));
            one(listener).itemRemovedFromSchedule(overlappingItem, overlap);
            one(listener).itemRemovedFromSchedule(overlappingItem2, overlap2);
        }});
        
        Schedule result = resolver.schedule(from, to, channels, publishers);
        assertSchedules(schedule, result);
    }
    
    public void testOverlappingBroadcastExactSwap() {
        final Broadcast overlap = new Broadcast(channel.uri(), now.plusMinutes(35), now.plusMinutes(60)).withId("somethingElse");
        overlap.setLastUpdated(now);
        b3.setLastUpdated(now.plusMinutes(1));
        final Item overlappingItem = item(overlap);
        
        final Schedule overlappingSchedule = schedule(channel, item(b1), item(b2), item(b3), overlappingItem, item(b4));
        
        checking(new Expectations() {{ 
            one(scheduleResolver).schedule(from, to, channels, publishers); will(returnValue(overlappingSchedule));
            one(listener).itemRemovedFromSchedule(overlappingItem, overlap);
        }});
        
        Schedule result = resolver.schedule(from, to, channels, publishers);
        assertSchedules(schedule, result);
    }
    
    private Schedule schedule(Channel channel, Item...items) {
        Schedule.ScheduleChannel scheduleChannel = new Schedule.ScheduleChannel(channel, ImmutableList.copyOf(items));
        return new Schedule(ImmutableList.of(scheduleChannel), interval);
    }
    
    private Item item(Broadcast broadcast) {
        Item itemWithBroadcast = (Item) item.copy();
        Version version = new Version();
        version.addBroadcast(broadcast);
        itemWithBroadcast.addVersion(version);
        return itemWithBroadcast;
    }
    
    private void assertSchedules(Schedule schedule, Schedule testSchedule) {
        assertEquals(schedule, testSchedule);
        Schedule.ScheduleChannel channel = Iterables.getOnlyElement(schedule.scheduleChannels());
        Schedule.ScheduleChannel testChannel = Iterables.getOnlyElement(schedule.scheduleChannels());
        
        for (int i=0; i<channel.items().size(); i++) {
            Item item = channel.items().get(i);
            Item testItem = testChannel.items().get(i);
            
            Broadcast broadcast = ScheduleEntry.BROADCAST.apply(item);
            Broadcast testBroadcast = ScheduleEntry.BROADCAST.apply(testItem);
            
            assertEquals(broadcast, testBroadcast);
        }
    }
}
