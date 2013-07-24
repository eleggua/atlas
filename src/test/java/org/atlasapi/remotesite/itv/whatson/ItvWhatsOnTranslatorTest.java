package org.atlasapi.remotesite.itv.whatson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;
import org.junit.Test;

import com.google.common.base.Optional;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;
import com.metabroadcast.common.time.DateTimeZones;

public class ItvWhatsOnTranslatorTest {
    
    private DateTime getDateTimeFromMillis(long millis) {
        return new DateTime(DateTimeZones.UTC).withMillis(millis);
    }
    
    private ItvWhatsOnEntryTranslator getTranslator() {
        return new ItvWhatsOnEntryTranslator();
    }
    
    private ItvWhatsOnEntry getTestItem() {
        ItvWhatsOnEntry entry = new ItvWhatsOnEntry();
        entry.setChannel("CITV");
        entry.setBroadcastDate(getDateTimeFromMillis(1374644400000L));
        ItvWhatsOnEntryDuration duration = new ItvWhatsOnEntryDuration();
        duration.setTicks(18000000000L);
        duration.setDays(0);
        duration.setHours(0);
        duration.setMilliseconds(0);
        duration.setMinutes(30);
        duration.setSeconds(0);
        duration.setTotalDays(0.020833333333333332);
        duration.setTotalHours(0.5);
        duration.setTotalMilliseconds(1800000);
        duration.setTotalMinutes(30);
        duration.setTotalSeconds(1800);
        entry.setDuration(duration);
        entry.setProgrammeTitle("Huntik - Secrets and Seekers");
        entry.setEpisodeTitle("Cave of the Casterwills");
        entry.setSynopsis("Animated adventure series. Dante Vale and his band of "
                + "Seekers explore exotic locations while on missions to save humanity"
                + " from evil forces ");
        entry.setImageUri("http://example.com/DotCom/episode/318524/image.jpg");
        entry.setVodcrid("318524");
        entry.setAvailabilityStart(getDateTimeFromMillis(1374646200000L));
        entry.setAvailabilityEnd(getDateTimeFromMillis(1377298740000L));
        entry.setRepeat(false);
        entry.setComingSoon(true);
        entry.setProductionId("1/7680/0029#001");
        entry.setProgrammeId("1/7680");
        entry.setSeriesId("1/7680-02");
        entry.setEpisodeId("1/7680/0029");
        return entry;
    }
    
    @Test
    public void testBrandTranslation() {
        ItvWhatsOnEntryTranslator translator = getTranslator();
        Optional<Brand> brand = translator.toBrand(getTestItem());
        assertEquals(brand.get().getCanonicalUri(), "http://itv.com/brand/1/7680");
        assertEquals(brand.get().getTitle(), "Huntik - Secrets and Seekers");
    }
    
    @Test
    public void testSeriesTranslation() {
        ItvWhatsOnEntryTranslator translator = getTranslator();
        Optional<Series> series = translator.toSeries(getTestItem());
        assertEquals(series.get().getCanonicalUri(), "http://itv.com/series/1/7680-02");
    }
    
    @Test
    public void testItemTranslation() {
        ItvWhatsOnEntryTranslator translator = getTranslator();
        Item item = translator.toEpisodeOrItem(getTestItem());
        assertEquals(item.getCanonicalUri(), "http://itv.com/1/7680/0029");
        assertEquals(item.getTitle(), "Cave of the Casterwills");
        assertEquals(item.getDescription().length(), 139);
        assertEquals(item.getImage(), "http://example.com/DotCom/episode/318524/image.jpg");
        assertEquals(item.getAliasUrls().size(), 1);
        for (String aliasUrl : item.getAliasUrls()) {
            assertEquals(aliasUrl, "http://itv.com/vodcrid/318524");
        }
    }
    
    @Test
    public void testEpisodeSynthesizedTranslation() {
        ItvWhatsOnEntryTranslator translator = getTranslator();
        ItvWhatsOnEntry entry = getTestItem();
        entry.setEpisodeId("");        
        Episode episode = (Episode) translator.toEpisodeOrItem(entry);
        assertEquals(episode.getCanonicalUri(), "http://itv.com/synthesized/1/7680/0029#001");
    }
    
    @Test
    public void testVersion() {
        ItvWhatsOnEntryTranslator translator = getTranslator();
        Episode episode = (Episode) translator.toEpisodeOrItem(getTestItem());
        assertEquals(episode.getVersions().size(), 1);
        for (Version version : episode.getVersions()) {
            assertEquals(version.getCanonicalUri(), "http://itv.com/version/1/7680/0029#001");
            assertEquals(version.getDuration().intValue(), 1800);
        }
    }
    
    @Test
    public void testBroadcast() {
        ItvWhatsOnEntryTranslator translator = getTranslator();
        Episode episode = (Episode) translator.toEpisodeOrItem(getTestItem());
        DateTime expectedTransmissionStart = getDateTimeFromMillis(1374644400000L);
        DateTime expectedTransmissionEnd = expectedTransmissionStart.plusSeconds(1800);
        for (Version version : episode.getVersions()) {
            assertEquals(version.getBroadcasts().size(),1);
            for (Broadcast broadcast : version.getBroadcasts()) {
                assertEquals(broadcast.getBroadcastOn(), "http://www.itv.com/channels/citv");
                assertEquals(broadcast.getTransmissionTime(), expectedTransmissionStart);
                assertEquals(broadcast.getTransmissionEndTime(), expectedTransmissionEnd);
                assertEquals(broadcast.getBroadcastDuration().intValue(), 1800);
                assertFalse(broadcast.getRepeat().booleanValue());
            }
        }
    }
    
    @Test
    public void testLocation() {
        ItvWhatsOnEntryTranslator translator = getTranslator();
        Episode episode = (Episode) translator.toEpisodeOrItem(getTestItem());
        for (Version version : episode.getVersions()) {
            for (Encoding encoding : version.getManifestedAs()) {
                assertEquals(encoding.getAvailableAt().size(), 1);
                for (Location location : encoding.getAvailableAt()) {
                    assertEquals(location.getCanonicalUri(), "http://www.itv.com/itvplayer/video/?filter=1%2F7680%2F0029%23001");
                    assertEquals(location.getTransportType(), TransportType.LINK);
                    Policy policy = location.getPolicy();
                    checkPolicy(policy);
                }
            }
        }
    }
    
    private void checkPolicy(Policy policy) {
        assertEquals(policy.getRevenueContract(), RevenueContract.FREE_TO_VIEW);
        assertEquals(policy.getAvailableCountries().size(), 1);
        assertEquals(policy.getAvailabilityStart(), getDateTimeFromMillis(1374646200000L));
        assertEquals(policy.getAvailabilityEnd(), getDateTimeFromMillis(1377298740000L));
        for (Country country : policy.getAvailableCountries()) {
            assertEquals(country, Countries.GB);
        }
    }
}
