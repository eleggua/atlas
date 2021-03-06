package org.atlasapi.output.simple;

import static com.metabroadcast.common.base.MorePredicates.transformingPredicate;

import java.util.Set;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.ChildRef;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.EntityType;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.SeriesRef;
import org.atlasapi.media.entity.simple.ContentIdentifier;
import org.atlasapi.media.entity.simple.Playlist;
import org.atlasapi.media.entity.simple.ContentIdentifier.SeriesIdentifier;
import org.atlasapi.media.product.ProductResolver;
import org.atlasapi.output.Annotation;
import org.atlasapi.persistence.output.AvailableItemsResolver;
import org.atlasapi.persistence.output.RecentlyBroadcastChildrenResolver;
import org.atlasapi.persistence.output.UpcomingItemsResolver;
import org.atlasapi.persistence.topic.TopicQueryResolver;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import org.atlasapi.persistence.content.ContentGroupResolver;
import org.atlasapi.persistence.content.PeopleQueryResolver;

public class ContainerModelSimplifier extends ContentModelSimplifier<Container, Playlist> {

    private final ModelSimplifier<Item, org.atlasapi.media.entity.simple.Item> itemSimplifier;
    private final AvailableItemsResolver availableItemsResolver;
    private final UpcomingItemsResolver upcomingItemsResolver;
    private final RecentlyBroadcastChildrenResolver recentlyBroadcastResolver;
    private final Function<ChildRef, ContentIdentifier> toContentIdentifier = new Function<ChildRef, ContentIdentifier>() {

        @Override
        public ContentIdentifier apply(ChildRef input) {
            return ContentIdentifier.identifierFor(input, idCodec);
        }
    };
    
    private final Function<SeriesRef, SeriesIdentifier> toSeriesIdentifier = new Function<SeriesRef, SeriesIdentifier>() {

        @Override
        public SeriesIdentifier apply(SeriesRef input) {
            return ContentIdentifier.seriesIdentifierFor(input, idCodec);
        }
    };

    public ContainerModelSimplifier(ModelSimplifier<Item, org.atlasapi.media.entity.simple.Item> itemSimplifier, String localHostName, 
            ContentGroupResolver contentGroupResolver, TopicQueryResolver topicResolver, AvailableItemsResolver availableResovler, 
            UpcomingItemsResolver upcomingResolver, ProductResolver productResolver, RecentlyBroadcastChildrenResolver recentChildren,
            ImageSimplifier imageSimplifier, PeopleQueryResolver peopleResolver) {
        super(localHostName, contentGroupResolver, topicResolver, productResolver, imageSimplifier, peopleResolver, upcomingResolver, availableResovler);
        this.itemSimplifier = itemSimplifier;
        this.availableItemsResolver = availableResovler;
        this.upcomingItemsResolver = upcomingResolver;
        this.recentlyBroadcastResolver = recentChildren;
    }

    @Override
    public Playlist simplify(Container fullPlayList, Set<Annotation> annotations, ApplicationConfiguration config) {

        Playlist simplePlaylist = new Playlist();

        copyBasicContentAttributes(fullPlayList, simplePlaylist, annotations, config);
        simplePlaylist.setType(EntityType.from(fullPlayList).toString());

        if (annotations.contains(Annotation.EXTENDED_DESCRIPTION)) {
            if (fullPlayList instanceof Series) {
                Series series = (Series) fullPlayList;
                simplePlaylist.setSeriesNumber(series.getSeriesNumber());
                simplePlaylist.setTotalEpisodes(series.getTotalEpisodes());
            }
        }

        if (annotations.contains(Annotation.SUB_ITEMS)) {
            simplePlaylist.setContent(Lists.transform(fullPlayList.getChildRefs(), toContentIdentifier));
        }
        
        if (annotations.contains(Annotation.SERIES)) {
            if (fullPlayList instanceof Brand) {
                Brand brand = (Brand) fullPlayList;
                simplePlaylist.setSeriesList(Lists.transform(brand.getSeriesRefs(), toSeriesIdentifier));
            }
        }

        if (annotations.contains(Annotation.AVAILABLE_LOCATIONS)) {
            simplePlaylist.setAvailableContent(filterAndTransformChildRefs(fullPlayList, availableFilter(fullPlayList, config)));
        }

        if (annotations.contains(Annotation.UPCOMING)) {
            simplePlaylist.setUpcomingContent(filterAndTransformChildRefs(fullPlayList, upcomingFilter(fullPlayList)));;
        }
        
        if (annotations.contains(Annotation.RECENTLY_BROADCAST)) {
            simplePlaylist.setRecentContent(filterAndTransformChildRefs(fullPlayList, recentlyBroadcastFilter(fullPlayList)));
        }

        return simplePlaylist;
    }

    private Iterable<ContentIdentifier> filterAndTransformChildRefs(Container fullPlayList, Predicate<ChildRef> filter) {
        return Iterables.transform(Iterables.filter(fullPlayList.getChildRefs(), filter), toContentIdentifier);
    }

    private Predicate<ChildRef> availableFilter(Container fullPlayList, ApplicationConfiguration config) {
        return asChildRefFilter(availableItemsResolver.availableItemsFor(fullPlayList, config));
    }

    private Predicate<ChildRef> upcomingFilter(Container fullPlayList) {
        return asChildRefFilter(upcomingItemsResolver.upcomingItemsFor(fullPlayList));
    }

    private Predicate<ChildRef> recentlyBroadcastFilter(Container fullPlayList) {
        return asChildRefFilter(recentlyBroadcastResolver.recentlyBroadcastChildrenFor(fullPlayList, 3));
    }
    
    private Predicate<ChildRef> asChildRefFilter(Iterable<String> childRefUris) {
        return transformingPredicate(ChildRef.TO_URI, Predicates.in(ImmutableSet.copyOf(childRefUris)));
    }

    @Override
    protected org.atlasapi.media.entity.simple.Item simplify(org.atlasapi.media.entity.Item item, Set<Annotation> annotations, ApplicationConfiguration config) {
        return itemSimplifier.simplify(item, annotations, config);
    }
}
