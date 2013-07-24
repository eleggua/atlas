package org.atlasapi.remotesite.itv.whatson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import org.joda.time.DateTime;
import org.junit.Test;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.metabroadcast.common.time.DateTimeZones;


public class ItvWhatsOnClientTest {
    
    @Test
    public void testDeserialisation() throws IOException {
        DateTime expectedBroadcastDate = new DateTime(DateTimeZones.UTC).withDate(2013, 7, 24).withTime(2, 40, 0, 0);
        
        ItvWhatsOnDeserializer deserializer = new ItvWhatsOnDeserializer();
        String json =  Resources.toString(Resources.getResource("itv-whatson-schedule.json"), Charsets.UTF_8);
        List<ItvWhatsOnEntry> results = deserializer.deserialize(new StringReader(json));
        assertEquals(results.size(), 2);
        ItvWhatsOnEntry testItem = results.get(1);
        assertEquals(testItem.getChannel(), "ITV1");
        assertEquals(testItem.getBroadcastDate(), expectedBroadcastDate);
        ItvWhatsOnEntryDuration duration = testItem.getDuration();
        assertEquals(duration.getTicks(), 51000000000L);
        assertEquals(duration.getDays(), 0);
        assertEquals(duration.getHours(), 1);
        assertEquals(duration.getMilliseconds(), 0);
        assertEquals(duration.getMinutes(), 25);
        assertEquals(duration.getSeconds(), 0);
        assertEquals(duration.getTotalDays(), 0.059027777777777776, 0.0000001);
        assertEquals(duration.getTotalHours(), 1.4166666666666665, 0.0000001);
        assertEquals(duration.getTotalMilliseconds(), 5100000);
        assertEquals(duration.getTotalMinutes(), 85);
        assertEquals(duration.getTotalSeconds(), 5100);
        assertEquals(testItem.getProgrammeTitle(), "ITV Nightscreen");
        assertEquals(testItem.getEpisodeTitle(), "ITV Nightscreen");
        assertEquals(testItem.getSynopsis(), "Text-based information service.");
        assertEquals(testItem.getImageUri(), "");
        assertEquals(testItem.getVodcrid(), "");
        assertNull(testItem.getAvailabilityStart());
        assertNull(testItem.getAvailabilityEnd());
        assertFalse(testItem.isRepeat());
        assertFalse(testItem.isComingSoon());
        assertEquals(testItem.getProductionId(), "3fb961a5-166e-4c73-92be-008f8b0f5e81");
        assertEquals(testItem.getProgrammeId(), "");
        assertEquals(testItem.getSeriesId(), "");
        assertEquals(testItem.getEpisodeId(), "");
    }
}
