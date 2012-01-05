package org.atlasapi.remotesite.channel4.epg;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.atlasapi.media.entity.Publisher.C4;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.util.List;

import junit.framework.TestCase;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.SystemOutAdapterLog;
import org.atlasapi.persistence.testing.StubContentResolver;
import org.atlasapi.remotesite.channel4.C4BrandUpdater;
import org.atlasapi.remotesite.channel4.C4RelatedEntry;
import org.atlasapi.remotesite.channel4.RecordingContentWriter;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.time.DateTimeZones;

public class C4EpgEntryProcessorTest extends TestCase {
    
	private static final Channel CHANNEL_FOUR = new Channel(Publisher.METABROADCAST, "Channel 4", "channel4", MediaType.VIDEO, "http://www.channel4.com");

    private final AdapterLog log = new SystemOutAdapterLog();
    
    private final C4BrandUpdater brandUpdater = new C4BrandUpdater() {
        
        @Override
        public Brand createOrUpdateBrand(String uri) {
            throw new RuntimeException();
        }
        
        @Override
        public boolean canFetch(String uri) {
            return false;
        }
    };
    
    //Item, series and brand don't exist so all are made.
    public void testProcessNewItemSeriesBrand() {
        
    	ContentResolver resolver = StubContentResolver.RESOLVES_NOTHING;

    	RecordingContentWriter writer = new RecordingContentWriter();
    	
    	C4EpgEntryProcessor processor = new C4EpgEntryProcessor(writer, resolver, brandUpdater, log);
        processor.process(buildEntry(), CHANNEL_FOUR);
        
        Brand brand = Iterables.getOnlyElement(writer.updatedBrands);
        
        assertThat(brand.getCanonicalUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs")));
        assertThat(brand.getCurie(), is(equalTo("c4:the-hoobs")));
        
        
        Series series = Iterables.getOnlyElement(writer.updatedSeries);
        assertThat(series.getCanonicalUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1")));
        assertThat(series.getParent().getUri(), is(brand.getCanonicalUri()));
        
        List<Episode> contents = ImmutableList.copyOf(Iterables.filter(writer.updatedItems, Episode.class));
        assertThat(contents.size(), is(equalTo(1)));
        
        Episode episode = getOnlyElement(contents);
        assertThat(episode.getCanonicalUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1/episode-59")));
        assertThat(episode.getTitle(), is(equalTo("Dancing")));
        assertThat(episode.getEpisodeNumber(), is(59));
        assertThat(episode.getSeriesNumber(), is(1));
        assertThat(episode.getSeriesRef().getUri(), is(series.getCanonicalUri()));
        
        
        Version version = getOnlyElement(episode.getVersions());
        assertThat(version.getDuration().longValue(), is(equalTo(Duration.standardMinutes(24).plus(Duration.standardSeconds(12)).getStandardSeconds())));
        
        Broadcast broadcast = getOnlyElement(version.getBroadcasts());
        assertThat(broadcast.getSourceId(), is(equalTo("c4:337")));
        assertThat(broadcast.getAliases().size(), is(1));
        assertThat(broadcast.getAliases(), hasItem("tag:www.channel4.com,2009:slot/C4337"));
        assertThat(broadcast.getTransmissionTime(), is(equalTo(new DateTime("2011-01-07T06:35:00.000Z"))));
        assertThat(broadcast.getTransmissionEndTime(), is(equalTo(new DateTime("2011-01-07T06:35:00.000Z").plus(Duration.standardMinutes(24).plus(Duration.standardSeconds(12))))));
        
        Encoding encoding = getOnlyElement(version.getManifestedAs());
        Location location = getOnlyElement(encoding.getAvailableAt());
        assertThat(location.getUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs/4od#2930251")));
        assertThat(location.getPolicy().getAvailabilityStart(), is(equalTo(new DateTime("2009-06-07T22:00:00.000Z"))));
        assertThat(location.getPolicy().getAvailabilityEnd(), is(equalTo(new DateTime("2018-12-07T00:00:00.000Z"))));
    }
    
    public void testProcessExistingItemSeriesBrand() { 
        final Episode previouslyWrittenEpisode = existingEpisode();
        final Series previouslyWrittenSeries = new Series("http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1", "c4:the-hoobs-series-1", C4);
        final Brand previouslyWrittenBrand = new Brand("http://www.channel4.com/programmes/the-hoobs", "c4:the-hoobs", C4);

        previouslyWrittenEpisode.setContainer(previouslyWrittenBrand);
        previouslyWrittenEpisode.setSeries(previouslyWrittenSeries);
        
        final RecordingContentWriter writer = new RecordingContentWriter();
        
        StubContentResolver resolver = new StubContentResolver()
            .respondTo(previouslyWrittenEpisode)
            .respondTo(previouslyWrittenSeries)
            .respondTo(previouslyWrittenBrand);
        
        C4EpgEntryProcessor processor = new C4EpgEntryProcessor(writer, resolver, brandUpdater, log);
        
        processor.process(buildEntry(), CHANNEL_FOUR);
        
        Brand brand = Iterables.getOnlyElement(writer.updatedBrands);
        
        assertThat(brand.getCanonicalUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs")));
        
        Series series = Iterables.getOnlyElement(writer.updatedSeries);
        assertThat(series.getCanonicalUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1")));

        Episode episode = (Episode) getOnlyElement(writer.updatedItems);
        assertThat(episode.getCanonicalUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1/episode-59")));
        assertThat(episode.getTitle(), is(equalTo("Dancing")));
        assertThat(episode.getEpisodeNumber(), is(59));
        assertThat(episode.getSeriesNumber(), is(1));
        
        Version version = getOnlyElement(episode.getVersions());
        assertThat(version.getDuration().longValue(), is(equalTo(Duration.standardMinutes(24).plus(Duration.standardSeconds(12)).getStandardSeconds())));
        
        assertThat(version.getBroadcasts().size(), is(2));
        Broadcast broadcast = Iterables.getLast(version.getBroadcasts()).getCurie() != null ? Iterables.get(version.getBroadcasts(), 0) : Iterables.getLast(version.getBroadcasts());
        assertThat(broadcast.getSourceId(), is(equalTo("c4:337")));
        assertThat(broadcast.getAliases().size(), is(1));
        assertThat(broadcast.getAliases(), hasItem("tag:www.channel4.com,2009:slot/C4337"));
        assertThat(broadcast.getTransmissionTime(), is(equalTo(new DateTime("2011-01-07T06:35:00.000Z"))));
        assertThat(broadcast.getTransmissionEndTime(), is(equalTo(new DateTime("2011-01-07T06:35:00.000Z").plus(Duration.standardMinutes(24).plus(Duration.standardSeconds(12))))));
        
        Encoding encoding = getOnlyElement(version.getManifestedAs());
        assertThat(encoding.getAvailableAt().size(), is(1));
        Location location = getOnlyElement(encoding.getAvailableAt());

        // EPG update doesn't modify locations created overnight
        assertThat(location.getUri(), is(equalTo("oldUri")));
        
    }

    private Episode existingEpisode() {
        Episode episode = new Episode("http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1/episode-59", "c4:the-hoobs-series-1-episode-1", C4);
        episode.setTitle("Dancing");
        episode.setSeriesNumber(1);
        episode.setEpisodeNumber(59);
        
        Version version = new Version();

        Broadcast broadcast = new Broadcast("http://www.channel4.com", new DateTime(DateTimeZones.UTC), new DateTime(DateTimeZones.UTC));
        broadcast.setCurie("old");
        broadcast.withId("c4:345");

        Encoding encoding = new Encoding();
        Location location = new Location();
        location.setUri("oldUri");
		encoding.addAvailableAt(location);
        
        version.addBroadcast(broadcast);
        version.addManifestedAs(encoding);
        episode.addVersion(version);
        
        return episode;
    }
    
    public void testProcessNewItemSeriesExistingBrand() {
        final Brand previouslySavedBrand = new Brand("http://www.channel4.com/programmes/the-hoobs", "c4:the-hoobs", C4);

        RecordingContentWriter writer = new RecordingContentWriter();
        
        // hasn't seen the item, series or episode before but has seen the brand
        StubContentResolver resolver = new StubContentResolver().respondTo(previouslySavedBrand);
    
        C4EpgEntryProcessor processor = new C4EpgEntryProcessor(writer, resolver, brandUpdater, log);
        
        processor.process(buildEntry(), CHANNEL_FOUR);
        
        Brand brand = Iterables.getOnlyElement(writer.updatedBrands);
        
        assertThat(brand.getCanonicalUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs")));
        
        Series series =  Iterables.getOnlyElement(writer.updatedSeries);
        
        assertThat(series.getCanonicalUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1")));

        Episode episode = (Episode) Iterables.getOnlyElement(writer.updatedItems);
        assertThat(episode.getCanonicalUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1/episode-59")));
        assertThat(episode.getTitle(), is(equalTo("Dancing")));
        assertThat(episode.getEpisodeNumber(), is(59));
        assertThat(episode.getSeriesNumber(), is(1));
        
        
        Version version = getOnlyElement(episode.getVersions());
        assertThat(version.getDuration().longValue(), is(equalTo(Duration.standardMinutes(24).plus(Duration.standardSeconds(12)).getStandardSeconds())));
        
        Broadcast broadcast = getOnlyElement(version.getBroadcasts());
        assertThat(broadcast.getSourceId(), is(equalTo("c4:337")));
        assertThat(broadcast.getAliases().size(), is(1));
        assertThat(broadcast.getAliases(), hasItem("tag:www.channel4.com,2009:slot/C4337"));
        assertThat(broadcast.getTransmissionTime(), is(equalTo(new DateTime("2011-01-07T06:35:00.000Z"))));
        assertThat(broadcast.getTransmissionEndTime(), is(equalTo(new DateTime("2011-01-07T06:35:00.000Z").plus(Duration.standardMinutes(24).plus(Duration.standardSeconds(12))))));
        
        Encoding encoding = getOnlyElement(version.getManifestedAs());
        Location location = getOnlyElement(encoding.getAvailableAt());
        assertThat(location.getUri(), is(equalTo("http://www.channel4.com/programmes/the-hoobs/4od#2930251")));
        assertThat(location.getPolicy().getAvailabilityStart(), is(equalTo(new DateTime("2009-06-07T22:00:00.000Z"))));
        assertThat(location.getPolicy().getAvailabilityEnd(), is(equalTo(new DateTime("2018-12-07T00:00:00.000Z"))));
    }

    private C4EpgEntry buildEntry() {
        return new C4EpgEntry("tag:www.channel4.com,2009:slot/337")
            .withRelatedEntry(new C4RelatedEntry("some feed", "tag:www.channel4.com,2009:/programmes/the-hoobs/episode-guide/series-1/episode-59"))
            .withTitle("Dancing")
            .withUpdated(new DateTime("2010-11-03T05:57:50.175Z"))
            .withSummary("Hoobs have been dancing the Hoobyjiggle since Hooby time began. But is there a Peep dance that fits to the Hoobyjiggle music?")
            .withLinks(ImmutableList.of(
                    "http://www.channel4.com/programmes/the-hoobs/4od#2930251", 
                    "http://www.channel4.com/programmes/the-hoobs/episode-guide/series-1/episode-59.atom"
            )).withMedia(
                    new C4EpgMedia()
                        .withPlayer("http://www.channel4.com/programmes/the-hoobs/4od#2930251")
                        .withThumbnail("http://www.channel4.com/assets/programmes/images/the-hoobs/series-1/the-hoobs-s1-20090623112301_200x113.jpg")
                        .withRating("nonadult")
                        .withRestriction(ImmutableSet.of(Countries.GB, Countries.IE)))
            .withBrandTitle("The Hoobs")
            .withAvailable("start=2009-06-07T22:00:00.000Z; end=2018-12-07T00:00:00.000Z; scheme=W3C-DTF")
            .withSeriesNumber(1)
            .withEpisodeNumber(59)
            .withAgeRating(0)
            .withTxDate(new DateTime("2011-01-07T06:35:00.000Z"))
            .withTxChannel("C4")
            .withSubtitles(true)
            .withAudioDescription(false)
            .withDuration(Duration.standardMinutes(24).plus(Duration.standardSeconds(12)));
    }

}
