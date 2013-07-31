package org.atlasapi.remotesite;

import static org.junit.Assert.assertEquals;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.time.DateTimeZones;


public class ContentMergerTest {
    
    @Test
    public void tesVersionMerger() {
        Item current = new Item();
        Item extracted = new Item();
        
        Broadcast broadcast1 = new Broadcast("http://example.com/channel1", 
                new DateTime(DateTimeZones.UTC),
                new DateTime(DateTimeZones.UTC).plusHours(1));
        Broadcast broadcast2 = new Broadcast("http://example.com/channel1", 
                new DateTime(DateTimeZones.UTC).plusHours(4),
                new DateTime(DateTimeZones.UTC).plusHours(5));
        Version version1 = new Version();
        version1.setCanonicalUri("http://example.org/1");
        version1.setBroadcasts(ImmutableSet.of(broadcast1));
        current.setVersions(ImmutableSet.of(version1));
        Version version2 = new Version();
        version2.setCanonicalUri("http://example.org/1");
        version2.setBroadcasts(ImmutableSet.of(broadcast2));
        extracted.setVersions(ImmutableSet.of(version2));
        ContentMerger.merge(current, extracted);
        assertEquals(current.getVersions().size(), 1);
        for (Version version : current.getVersions()) {
            assertEquals(version.getBroadcasts().size(), 2);
        }
        
    }

}
