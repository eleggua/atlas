package org.atlasapi.remotesite.itv.whatson;

import org.atlasapi.media.TransportSubType;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.media.entity.Location;
import org.joda.time.DateTime;
import org.joda.time.Duration;

import com.google.common.base.Optional;
import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableSet;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.url.UrlEncoding;


public class ItvWhatsOnEntryTranslator {
    private static final String BRAND_PREFIX = "http://itv.com/brand/";
    private static final String SERIES_PREFIX = "http://itv.com/series/";
    private static final String EPISODE_PREFIX = "http://itv.com/";
    private static final String EPISODE_ALIASES_PREFIX = "http://itv.com/vodcrid/";
    private static final String EPISODE_SYNTHESIZED_PREFIX = "http://itv.com/synthesized/";
    private static final String VERSION_PREFIX = "http://itv.com/version/";
    private static final String LOCATION_PREFIX = "http://www.itv.com/itvplayer/video/?filter=";
    private static final Publisher PUBLISHER_ITV = Publisher.ITV;
    
    private final BiMap<String, String> channelMap;
    
    public ItvWhatsOnEntryTranslator() {
        channelMap = ImmutableBiMap.<String, String>builder()
                .put("ITV1", "http://www.itv.com/channels/itv1/london")
                .put("ITV2", "http://www.itv.com/channels/itv2")
                .put("ITV3", "http://www.itv.com/channels/itv3")
                .put("ITV4", "http://www.itv.com/channels/itv4")
                .put("CITV", "http://www.itv.com/channels/citv")
                .build();
    }
   
    public Optional<Brand> toBrand(ItvWhatsOnEntry entry) {
        if (entry.getProgrammeId().isEmpty()) {
            return Optional.absent();
        }
        Brand brand = new Brand();
        brand.setCanonicalUri(BRAND_PREFIX + entry.getProgrammeId());
        brand.setTitle(entry.getProgrammeTitle());
        brand.setPublisher(PUBLISHER_ITV);
        return Optional.of(brand);
    }
    
    public Optional<Series> toSeries(ItvWhatsOnEntry entry) {
        if (entry.getSeriesId().isEmpty()) {
            return Optional.absent();
        }
        Series series = new Series();
        series.setCanonicalUri(SERIES_PREFIX + entry.getSeriesId());
        series.setPublisher(PUBLISHER_ITV);
        return Optional.of(series);
    }
    
    public Item toEpisodeOrItem(ItvWhatsOnEntry entry) {
        Optional<Brand> brand = toBrand(entry);
        Optional<Series> series = toSeries(entry);
        if (brand.isPresent() || series.isPresent()) {
            return toEpisode(entry, brand, series);
        } else {
            return toItem(entry);
        }
    }
    
    private void setCommonItemAttributes(Item target, ItvWhatsOnEntry entry) {
        String uri;
        if (entry.getEpisodeId() != null && !entry.getEpisodeId().isEmpty()) {
            uri = EPISODE_PREFIX + entry.getEpisodeId();            
        } else {
            uri = EPISODE_SYNTHESIZED_PREFIX + entry.getProductionId();
        }
        target.setCanonicalUri(uri);
        target.setTitle(entry.getEpisodeTitle());
        target.setDescription(entry.getSynopsis());
        target.setImage(entry.getImageUri());
        target.addAliasUrl(EPISODE_ALIASES_PREFIX + entry.getVodcrid());
        target.setPublisher(PUBLISHER_ITV);
        target.setVersions(ImmutableSet.of(toVersionAndLocation(entry)));
    }
    
    private Episode toEpisode(ItvWhatsOnEntry entry, Optional<Brand> brand, Optional<Series> series) {
        Episode episode = new Episode();
        if (brand.isPresent()) {
            episode.setContainer(brand.get());
        }
        if (series.isPresent()) {
            episode.setSeries(series.get());
        }
        setCommonItemAttributes(episode, entry);
        return episode;
    }
    
    private Item toItem(ItvWhatsOnEntry entry) {
        Item item = new Item();
        setCommonItemAttributes(item, entry);
        return item;
    }
    
    private Policy getPolicy(ItvWhatsOnEntry entry) {
        Policy policy = new Policy();
        policy.setAvailabilityStart(entry.getAvailabilityStart());
        policy.setAvailabilityEnd(entry.getAvailabilityEnd());
        policy.setAvailableCountries(ImmutableSet.of(Countries.GB));
        policy.setRevenueContract(RevenueContract.FREE_TO_VIEW);
        return policy;
    }
    
    private Location getLocation(ItvWhatsOnEntry entry, Policy policy) {
        Location location = new Location();        
        String uri = LOCATION_PREFIX + UrlEncoding.encode(entry.getProductionId());
        location.setUri(uri);    
        location.setTransportType(TransportType.LINK);
        location.setTransportSubType(TransportSubType.HTTP);
        location.setPolicy(policy);
        return location;
    }
    
    public Version toVersionAndLocation(ItvWhatsOnEntry entry) {
        Policy policy = getPolicy(entry);
        Location location = getLocation(entry, policy);
        Encoding encoding = new Encoding();
        encoding.addAvailableAt(location);
        Version version = new Version();
        version.setCanonicalUri(VERSION_PREFIX + entry.getProductionId());
        version.setPublishedDuration(entry.getDuration().getTotalSeconds());
        Duration duration = new Duration(entry.getDuration().getTotalSeconds()*1000);
        version.setDuration(duration);
        version.addManifestedAs(encoding);
        version.setBroadcasts(ImmutableSet.of(toBroadcast(entry)));
        return version;
    }
    
    public Broadcast toBroadcast(ItvWhatsOnEntry entry) {
        String channelUri = channelMap.get(entry.getChannel());
        DateTime start = entry.getBroadcastDate();
        DateTime end = entry.getBroadcastDate().plusSeconds(entry.getDuration().getTotalSeconds());
        Broadcast broadcast = new Broadcast(channelUri, start, end);
        broadcast.setRepeat(entry.isRepeat());
        return broadcast;
    }
}
