package org.atlasapi.remotesite.bbc.ion;

import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;
import static org.atlasapi.persistence.logging.AdapterLogEntry.warnEntry;
import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.WARN;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.people.ItemsPeopleWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.remotesite.SiteSpecificAdapter;
import org.atlasapi.remotesite.bbc.ion.model.IonBroadcast;
import org.atlasapi.remotesite.bbc.ion.model.IonSchedule;
import org.joda.time.Duration;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.metabroadcast.common.base.Maybe;

public class DefaultBbcIonScheduleHandler implements BbcIonScheduleHandler {

    private static final String BBC_CURIE_BASE = "bbc:";
    private static final String SLASH_PROGRAMMES_ROOT = "http://www.bbc.co.uk/programmes/";

    private final ContentResolver resolver;
    private final ContentWriter writer;
    private final AdapterLog log;
    private final BbcIonEpisodeItemContentExtractor itemExtractor;
    
    private final BbcIonItemMerger merger = new BbcIonItemMerger();
    
    private SiteSpecificAdapter<Item> itemClient;
    private BbcContainerFetcherClient containerClient;
    private ItemsPeopleWriter itemsPeopleWriter;
    
    public DefaultBbcIonScheduleHandler(ContentResolver resolver, ContentWriter writer, AdapterLog log) {
        this.resolver = resolver;
        this.writer = writer;
        this.log = log;
        this.itemExtractor = new BbcIonEpisodeItemContentExtractor(log);
    }

    public DefaultBbcIonScheduleHandler withItemFetcherClient(SiteSpecificAdapter<Item> client) {
        this.itemClient = client;
        return this;
    }

    public DefaultBbcIonScheduleHandler withContainerFetcherClient(BbcContainerFetcherClient containerClient) {
        this.containerClient = containerClient;
        return this;
    }

    public DefaultBbcIonScheduleHandler withItemPeopleWriter(ItemsPeopleWriter itemsPeopleWriter) {
        this.itemsPeopleWriter = itemsPeopleWriter;
        return this;
    }

    @Override
    public int handle(IonSchedule schedule) {
        int processed = 0;

        for (IonBroadcast broadcast : schedule.getBlocklist()) {
            String itemUri = SLASH_PROGRAMMES_ROOT + broadcast.getEpisodeId();
            try {
                
                Item item = resolveOrFetchItem(broadcast, itemUri);
                
                if(item == null) {
                    continue;
                }
                
                //ensure broadcast is included.
                addBroadcastToItem(item, broadcast);

                String canonicalUri = item.getCanonicalUri();

                Brand brand = !Strings.isNullOrEmpty(broadcast.getBrandId()) ? getOrCreateBrand(broadcast, canonicalUri) : null;
                Series series = !Strings.isNullOrEmpty(broadcast.getSeriesId()) ? getOrCreateSeries(broadcast, canonicalUri) : null;

                if (brand != null) {
                    updateBrand(brand, broadcast);
                    item.setContainer(brand);
                    writer.createOrUpdate(brand);
                }

                if (series != null) {
                    updateSeries(series, broadcast);
                    updateEpisodeSeriesDetails(series, (Episode) item);
                    if (brand != null) {
                        series.setParent(brand);
                    } else {
                        item.setContainer(series);
                        series.setParentRef(null);
                    }
                    writer.createOrUpdate(series);
                    ((Episode) item).setSeries(series);
                }

                writer.createOrUpdate(item);
                createOrUpdatePeople((Item) item);

                processed++;
            } catch (Exception e) {
                log.record(errorEntry().withCause(e).withSource(getClass())
                        .withDescription("Schedule Updater failed for %s %s, processing broadcast %s of %s", broadcast.getService(), broadcast.getDate(), broadcast.getId(), itemUri));
            }
        }

        return processed;
    }

    private Item resolveOrFetchItem(IonBroadcast broadcast, String itemUri) {
        //Get basic item from latest remote data.
        Item item = itemExtractor.extract(broadcast.getEpisode()); 

        // look for existing item, merge into latest remote data, else fetch complete.
        Maybe<Identified> possibleIdentified = resolver.findByCanonicalUris(ImmutableList.of(itemUri)).get(itemUri);
        if (possibleIdentified.hasValue()) {
            Identified ided = possibleIdentified.requireValue();
            if (!(ided instanceof Item)) {
                log.record(new AdapterLogEntry(Severity.WARN).withDescription("Updating %s, expecting Item, got %s", itemUri, ided.getClass().getSimpleName()).withSource(getClass()));
                return null;
            }
            item = merger.merge(item, (Item)ided);
        } else if (itemClient != null) {
            try {
                Item fetchedItem = itemClient.fetch(itemUri);
                item = fetchedItem != null ? fetchedItem : item;
            } catch (Exception e) {
                log.record(warnEntry().withSource(getClass()).withCause(e).withDescription("Failed to fetch ", itemUri));
            }
        }
        return item;
    }

//    private void merge(Item fetchedItem, Item existing) {
//        //merge into fetched item to ensure type is up to date, need to be careful not to overwrite new data then.
//        fetchedItem.addAliases(existing.getAliases());
//        fetchedItem.setGenres(Iterables.concat(fetchedItem.getGenres(), existing.getGenres()));
//        fetchedItem.setTags(ImmutableSet.copyOf(Iterables.concat(fetchedItem.getTags(), existing.getTags())));
//        fetchedItem.setFirstSeen(existing.getFirstSeen());
//        fetchedItem.setPeople(existing.getPeople());
//        fetchedItem.setClips(Iterables.concat(fetchedItem.getClips(),existing.getClips()));
//        fetchedItem.setVersions(ImmutableSet.copyOf(Iterables.concat(fetchedItem.getVersions(), existing.getVersions())));
//    }

    private void updateEpisodeSeriesDetails(Series series, Episode episode) {
        Integer seriesNumber = series.getSeriesNumber();
        if (seriesNumber != null && seriesNumber > 0) {
            episode.setSeriesNumber(seriesNumber);
        }
        episode.setSeries(series);
    }

    private Series getOrCreateSeries(IonBroadcast broadcast, String itemUri) {
        String seriesUri = SLASH_PROGRAMMES_ROOT + broadcast.getSeriesId();
        Maybe<Identified> maybeSeries = resolver.findByCanonicalUris(ImmutableList.of(seriesUri)).get(seriesUri);

        if (maybeSeries.isNothing()) {
            Maybe<Series> series = Maybe.nothing();
            if (containerClient != null) {
                series = containerClient.createSeries(broadcast.getSeriesId());
            }
            return series.hasValue() ? series.requireValue() : createSeries(broadcast);
        }

        Identified ided = maybeSeries.requireValue();
        if (ided instanceof Series) {
            return (Series) ided;
        }

        log.record(new AdapterLogEntry(Severity.WARN).withSource(getClass()).withDescription(
                String.format("Updating item %s, got %s when resolving for Series %s", itemUri, ided.getClass().getSimpleName(), seriesUri)));
        return null;
    }

    private Brand getOrCreateBrand(IonBroadcast broadcast, String itemUri) {
        String brandUri = SLASH_PROGRAMMES_ROOT + broadcast.getBrandId();

        Maybe<Identified> maybeIdentified = resolver.findByCanonicalUris(ImmutableList.of(brandUri)).get(brandUri);

        if (maybeIdentified.isNothing()) {
            Maybe<Brand> brand = Maybe.nothing();
            if (containerClient != null) {
                brand = containerClient.createBrand(broadcast.getBrandId());
            }
            return brand.hasValue() ? brand.requireValue() : createBrandFrom(broadcast);
        }

        Identified ided = maybeIdentified.requireValue();
        if (ided instanceof Brand) {
            return (Brand) ided;
        } else if (ided instanceof Series) {
            Brand brand = brandFromSeries((Series) ided); //Handle remote conversion of series to brand
            log.record(new AdapterLogEntry(Severity.INFO).withSource(getClass()).withDescription(
                    String.format("Updating item %s, got Series when looking for Brand %s, converted it to Brand", itemUri, brandUri)));
            return brand;
        }

        log.record(new AdapterLogEntry(Severity.WARN).withSource(getClass()).withDescription(
                String.format("Updating item %s, got %s when resolving for Brand %s", itemUri, ided.getClass().getSimpleName(), brandUri)));
        return null;
    }

    private void createOrUpdatePeople(Item item) {
        if (itemsPeopleWriter != null) {
            itemsPeopleWriter.createOrUpdatePeople(item);
        }
    }

    private Brand brandFromSeries(Series series) {
        Brand brand = new Brand(series.getCanonicalUri(), series.getCurie(), series.getPublisher());
        brand.setEquivalentTo(series.getEquivalentTo());
        brand.setTitle(series.getTitle());
        brand.setDescription(series.getDescription());
        brand.setImage(series.getImage());
        brand.setThumbnail(series.getThumbnail());
        brand.setFirstSeen(series.getFirstSeen());
        brand.setGenres(series.getGenres());
        brand.setMediaType(series.getMediaType());
        brand.setSpecialization(series.getSpecialization());

        return brand;
    }

    private void updateSeries(Series series, IonBroadcast broadcast) {
        series.setTitle(broadcast.getEpisode().getSeriesTitle());
    }

    private Series createSeries(IonBroadcast broadcast) {
        return new Series(SLASH_PROGRAMMES_ROOT + broadcast.getSeriesId(), BBC_CURIE_BASE + broadcast.getSeriesId(), Publisher.BBC);
    }

    private Brand createBrandFrom(IonBroadcast broadcast) {
        return new Brand(SLASH_PROGRAMMES_ROOT + broadcast.getBrandId(), BBC_CURIE_BASE + broadcast.getBrandId(), Publisher.BBC);
    }

    private void updateBrand(Brand brand, IonBroadcast broadcast) {
        brand.setTitle(broadcast.getEpisode().getBrandTitle());
        BbcImageUrlCreator.addImagesTo("http://www.bbc.co.uk/iplayer/images/progbrand/", broadcast.getBrandId(), brand);
    }

    private void addBroadcastToItem(Item item, IonBroadcast ionBroadcast) {
        Version broadcastVersion = getBroadcastVersion(item, ionBroadcast);
        if (broadcastVersion == null) {
            broadcastVersion = versionFrom(ionBroadcast);
            item.addVersion(broadcastVersion);
        }

        Broadcast broadcast = atlasBroadcastFrom(ionBroadcast);
        if (broadcast != null) {
            broadcastVersion.addBroadcast(broadcast);
        }
    }

    private Version versionFrom(IonBroadcast ionBroadcast) {
        Version version = new Version();

        version.setCanonicalUri(SLASH_PROGRAMMES_ROOT + ionBroadcast.getVersionId());
        version.setDuration(Duration.standardSeconds(ionBroadcast.getDuration()));
        version.setProvider(Publisher.BBC);

        return version;
    }

    private Version getBroadcastVersion(Item item, IonBroadcast ionBroadcast) {
        for (Version version : item.nativeVersions()) {
            if (version.getCanonicalUri().equals(SLASH_PROGRAMMES_ROOT + ionBroadcast.getVersionId())) {
                return version;
            }
        }
        return null;
    }

    private Broadcast atlasBroadcastFrom(IonBroadcast ionBroadcast) {
        String serviceUri = BbcIonServices.get(ionBroadcast.getService());
        if (serviceUri == null) {
            log.record(new AdapterLogEntry(WARN).withSource(getClass()).withDescription("Couldn't find service URI for Ion Service %s", ionBroadcast.getService()));
            return null;
        } else {
            Broadcast broadcast = new Broadcast(serviceUri, ionBroadcast.getStart(), ionBroadcast.getEnd());
            broadcast.withId(BBC_CURIE_BASE + ionBroadcast.getId()).setScheduleDate(ionBroadcast.getDate().toLocalDate());
            broadcast.setLastUpdated(ionBroadcast.getUpdated());
            return broadcast;
        }
    }
}
