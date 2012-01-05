package org.atlasapi.remotesite.redux;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.persistence.logging.AdapterLogEntry.warnEntry;

import java.util.Map.Entry;
import java.util.Set;

import org.atlasapi.media.TransportSubType;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.RevenueContract;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.redux.model.FullReduxProgramme;
import org.atlasapi.remotesite.redux.model.ReduxMedia;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.media.MimeType;
import com.metabroadcast.common.time.DateTimeZones;

public class FullProgrammeItemExtractor implements ContentExtractor<FullReduxProgramme, Item>{

    public static final String REDUX_URI_BASE = "http://g.bbcredux.com";
    public static final String CURIE_BASE = "redux:";
    public static final String REDUX_PROGRAMME_URI_BASE = "http://g.bbcredux.com/programme/";
    
    private static final DateTimeFormatter ISO_FORMAT = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZones.UTC);
    private static final String IMAGE_SUFFIX = "image-640.jpg";
    private static final String THUMBNAIL_SUFFIX = "image-74.jpg";
    private static final String LOCATION_URI_FORMAT = "http://devapi.bbcredux.com/programme/%s/media/%s";
    
    private final AdapterLog log;
	private ReduxServices reduxServices;
    
    public FullProgrammeItemExtractor(ChannelResolver channelResolver, AdapterLog log) {
        this.reduxServices = new ReduxServices(channelResolver);
    	this.log = log;
    }
    
    @Override
    public Item extract(FullReduxProgramme source) {
        checkNotNull(source, "Attempt to extract Item from null ReduxProgramme");
        checkNotNull(Strings.emptyToNull(source.getCanonical()), "Non-empty 'canonical' field required");
        checkNotNull(Strings.emptyToNull(source.getDiskref()), "Non empty 'diskref' field required");
        
        Item item = new Item(REDUX_URI_BASE + source.getCanonical(), CURIE_BASE + source.getDiskref(), Publisher.BBC_REDUX);
        if(!Strings.isNullOrEmpty(source.getUri())) {
            item.addAlias(REDUX_URI_BASE + source.getUri());
        }
        
        item.setTitle(source.getTitle());
        item.setDescription(source.getDescription());
        
        item.setImage(source.getDepiction());
        item.setThumbnail(thumbnailFrom(source.getDepiction()));

        Channel channel = reduxServices.channelMap().get(source.getService());
        
        item.setMediaType(channel.mediaType());
        item.setSpecialization(MediaType.AUDIO.equals(channel.mediaType()) ? Specialization.RADIO : Specialization.TV);
        
        item.setVersions(ImmutableSet.of(
            versionFrom(source)
        ));
        
        return item;
    }

    private Version versionFrom(FullReduxProgramme source) {
        Version version = new Version();
        
        Broadcast broadcast = broadcastFrom(source);
        
        if (broadcast != null) {
            version.setBroadcasts(ImmutableSet.of(
                    broadcast
            ));
            version.setPublishedDuration(broadcast.getBroadcastDuration());
            version.setManifestedAs(encodingsFrom(source, broadcast));
        }
        
        return version;
    }

    private Set<Encoding> encodingsFrom(FullReduxProgramme source, Broadcast broadcast) {
        if (source.getMedia() == null) {
            throw new IllegalArgumentException(String.format("Disk ref %s has no media", source.getDiskref()));
        }
        
        Builder<Encoding> encodings = ImmutableSet.<Encoding>builder();
        
        for (Entry<String, ReduxMedia> media : source.getMedia().entrySet()) {
            Encoding encoding = encodingFrom(media.getValue(), source.getDiskref(), media.getKey(), broadcast.getTransmissionEndTime());
            if (encoding != null) {
                encodings.add(encoding);
            }
        }
        
        return encodings.build();
    }

    private Encoding encodingFrom(ReduxMedia media, String diskref, String key, DateTime availableFrom) {
        try {
            Encoding encoding = new Encoding();
            
            encoding.setVideoCoding(MimeType.fromString(media.getType()));
            
            encoding.setAvailableAt(ImmutableSet.of(
                locationFrom(media, diskref, key, availableFrom)
            ));
            
            return encoding;
        } catch (Exception e) {
            log.record(warnEntry().withCause(e).withSource(getClass()).withDescription("Exception extracting encoding %s for diskref %s", media.getType(), diskref));
            return null;
        }
    }

    private Location locationFrom(ReduxMedia media, String diskref, String key, DateTime availableFrom) {
        Location location = new Location();

        Policy policy = new Policy();
        policy.setAvailabilityStart(availableFrom);
        policy.setAvailableCountries(ImmutableSet.of(Countries.ALL));
        policy.setRevenueContract(RevenueContract.PRIVATE);
        location.setPolicy(policy);

        location.setTransportType(TransportType.DOWNLOAD);
        location.setTransportSubType(TransportSubType.HTTP);
        location.setAvailable(true);
        
        location.setUri(String.format(LOCATION_URI_FORMAT, diskref, key));
        
        return location;
    }

    private Broadcast broadcastFrom(FullReduxProgramme source) {
        Channel channel = reduxServices.channelMap().get(source.getService());
        Duration duration = getDuration(source);
        if (channel != null && duration != null) {
            Broadcast broadcast = new Broadcast(channel.uri(), ISO_FORMAT.parseDateTime(source.getWhen()), duration);
            
            broadcast.setSigned(source.getSigned());
            broadcast.setSubtitled(source.getSubtitles());
            broadcast.setHighDefinition(source.getHd());
            broadcast.setAudioDescribed(source.getAd());
            broadcast.setRepeat(source.getRepeat());
            
            return broadcast;
        }
        log.record(warnEntry().withSource(getClass()).withDescription("Couldn't extract broadcast for diskref %s", source.getDiskref()));
        return null;
    }

    private Duration getDuration(FullReduxProgramme source) {
        try {
            return Duration.standardSeconds(Long.parseLong(source.getDuration()));
        } catch (Exception e) {
            log.record(warnEntry().withCause(e).withDescription("Diskref %s, couldn't parse duration %s to long", source.getDiskref(), source.getDuration()).withSource(getClass()));
            return null;
        }
    }

    private String thumbnailFrom(String depiction) {
        if (Strings.isNullOrEmpty(depiction)) {
            return null;
        }
        
        int suffixStart = depiction.indexOf(IMAGE_SUFFIX);
        if (suffixStart > 0 && suffixStart + IMAGE_SUFFIX.length() == depiction.length()) {
            return depiction.substring(0, suffixStart) + THUMBNAIL_SUFFIX;
        }
        return null;
    }

}
