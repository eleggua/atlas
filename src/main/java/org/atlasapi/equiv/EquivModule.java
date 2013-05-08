/* Copyright 2010 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.equiv;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;
import static org.atlasapi.equiv.generators.AliasResolvingEquivalenceGenerator.aliasResolvingGenerator;
import static org.atlasapi.media.entity.Publisher.AMAZON_UK;
import static org.atlasapi.media.entity.Publisher.BBC;
import static org.atlasapi.media.entity.Publisher.BBC_MUSIC;
import static org.atlasapi.media.entity.Publisher.BBC_REDUX;
import static org.atlasapi.media.entity.Publisher.FACEBOOK;
import static org.atlasapi.media.entity.Publisher.ITUNES;
import static org.atlasapi.media.entity.Publisher.LOVEFILM;
import static org.atlasapi.media.entity.Publisher.NETFLIX;
import static org.atlasapi.media.entity.Publisher.PA;
import static org.atlasapi.media.entity.Publisher.PREVIEW_NETWORKS;
import static org.atlasapi.media.entity.Publisher.RADIO_TIMES;
import static org.atlasapi.media.entity.Publisher.RDIO;
import static org.atlasapi.media.entity.Publisher.SOUNDCLOUD;
import static org.atlasapi.media.entity.Publisher.SPOTIFY;
import static org.atlasapi.media.entity.Publisher.YOUTUBE;
import static org.atlasapi.media.entity.Publisher.YOUVIEW;

import java.io.File;
import java.util.Set;

import org.atlasapi.equiv.generators.BroadcastMatchingItemEquivalenceGenerator;
import org.atlasapi.equiv.generators.CandidateContainerEquivalenceGenerator;
import org.atlasapi.equiv.generators.ContainerChildEquivalenceGenerator;
import org.atlasapi.equiv.generators.EquivalenceGenerator;
import org.atlasapi.equiv.generators.FilmEquivalenceGenerator;
import org.atlasapi.equiv.generators.RadioTimesFilmEquivalenceGenerator;
import org.atlasapi.equiv.generators.ScalingEquivalenceGenerator;
import org.atlasapi.equiv.generators.SongTitleTransform;
import org.atlasapi.equiv.generators.TitleSearchGenerator;
import org.atlasapi.equiv.handlers.BroadcastingEquivalenceResultHandler;
import org.atlasapi.equiv.handlers.EpisodeFilteringEquivalenceResultHandler;
import org.atlasapi.equiv.handlers.EpisodeMatchingEquivalenceHandler;
import org.atlasapi.equiv.handlers.EquivalenceResultHandler;
import org.atlasapi.equiv.handlers.EquivalenceSummaryWritingHandler;
import org.atlasapi.equiv.handlers.LookupWritingEquivalenceHandler;
import org.atlasapi.equiv.handlers.ResultWritingEquivalenceHandler;
import org.atlasapi.equiv.results.combining.NullScoreAwareAveragingCombiner;
import org.atlasapi.equiv.results.combining.RequiredScoreFilteringCombiner;
import org.atlasapi.equiv.results.extractors.MusicEquivalenceExtractor;
import org.atlasapi.equiv.results.extractors.PercentThresholdEquivalenceExtractor;
import org.atlasapi.equiv.results.filters.AlwaysTrueFilter;
import org.atlasapi.equiv.results.filters.ConjunctiveFilter;
import org.atlasapi.equiv.results.filters.EquivalenceFilter;
import org.atlasapi.equiv.results.filters.MediaTypeFilter;
import org.atlasapi.equiv.results.filters.MinimumScoreFilter;
import org.atlasapi.equiv.results.filters.PublisherFilter;
import org.atlasapi.equiv.results.filters.SpecializationFilter;
import org.atlasapi.equiv.results.persistence.FileEquivalenceResultStore;
import org.atlasapi.equiv.results.persistence.RecentEquivalenceResultStore;
import org.atlasapi.equiv.scorers.ContainerHierarchyMatchingScorer;
import org.atlasapi.equiv.scorers.CrewMemberScorer;
import org.atlasapi.equiv.scorers.EquivalenceScorer;
import org.atlasapi.equiv.scorers.SequenceItemScorer;
import org.atlasapi.equiv.scorers.SongCrewMemberExtractor;
import org.atlasapi.equiv.scorers.TitleMatchingContainerScorer;
import org.atlasapi.equiv.scorers.TitleMatchingItemScorer;
import org.atlasapi.equiv.update.ContentEquivalenceUpdater;
import org.atlasapi.equiv.update.EquivalenceUpdater;
import org.atlasapi.equiv.update.EquivalenceUpdaters;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Song;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.content.SearchResolver;
import org.atlasapi.persistence.lookup.LookupWriter;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

@Configuration
public class EquivModule {

	private @Value("${equiv.results.directory}") String equivResultsDirectory;
    
    private @Autowired ScheduleResolver scheduleResolver;
    private @Autowired SearchResolver searchResolver;
    private @Autowired ContentResolver contentResolver;
    private @Autowired ChannelResolver channelResolver;
    private @Autowired EquivalenceSummaryStore equivSummaryStore;
    private @Autowired LookupWriter lookupWriter;

    public @Bean RecentEquivalenceResultStore equivalenceResultStore() {
        return new RecentEquivalenceResultStore(new FileEquivalenceResultStore(new File(equivResultsDirectory)));
    }

    private EquivalenceResultHandler<Container> containerResultHandlers(Iterable<Publisher> publishers) {
        return new BroadcastingEquivalenceResultHandler<Container>(ImmutableList.of(
            new LookupWritingEquivalenceHandler<Container>(lookupWriter, publishers),
            new EpisodeMatchingEquivalenceHandler(contentResolver, equivSummaryStore, lookupWriter, publishers),
            new ResultWritingEquivalenceHandler<Container>(equivalenceResultStore()),
            new EquivalenceSummaryWritingHandler<Container>(equivSummaryStore)
        ));
    }
    
    private <T extends Content> EquivalenceFilter<T> standardFilter() {
        return ConjunctiveFilter.valueOf(ImmutableList.of(
            new MinimumScoreFilter<T>(0.2),
            new MediaTypeFilter<T>(),
            new SpecializationFilter<T>(),
            new PublisherFilter<T>()
        ));
    }
    
    private EquivalenceUpdater<Item> standardItemUpdater(Set<Publisher> acceptablePublishers) {
        return ContentEquivalenceUpdater.<Item> builder()
            .withGenerators(ImmutableSet.<EquivalenceGenerator<Item>> of(
                new BroadcastMatchingItemEquivalenceGenerator(scheduleResolver, 
                    channelResolver, acceptablePublishers, Duration.standardMinutes(10))
            ))
            .withScorers(ImmutableSet.of(
                new TitleMatchingItemScorer(), new SequenceItemScorer()
            ))
            .withCombiner(new NullScoreAwareAveragingCombiner<Item>())
            .withFilter(this.<Item>standardFilter())
            .withExtractor(PercentThresholdEquivalenceExtractor.<Item> moreThanPercent(90))
            .withHandler(new BroadcastingEquivalenceResultHandler<Item>(ImmutableList.of(
                EpisodeFilteringEquivalenceResultHandler.relaxed(
                    new LookupWritingEquivalenceHandler<Item>(lookupWriter, acceptablePublishers),
                    equivSummaryStore
                ),
                new ResultWritingEquivalenceHandler<Item>(equivalenceResultStore()),
                new EquivalenceSummaryWritingHandler<Item>(equivSummaryStore)
            )))
            .build();
    }
    
    private EquivalenceUpdater<Container> standardContainerUpdater(Set<Publisher> publishers) {
        return ContentEquivalenceUpdater.<Container> builder()
            .withGenerators(ImmutableSet.of(
                TitleSearchGenerator.create(searchResolver, Container.class, publishers),
                ScalingEquivalenceGenerator.scale(
                    new ContainerChildEquivalenceGenerator(contentResolver, equivSummaryStore),
                    20)
                ))
            .withScorers(ImmutableSet.<EquivalenceScorer<Container>> of(
                new TitleMatchingContainerScorer()
            ))
            .withCombiner(new RequiredScoreFilteringCombiner<Container>(
                new NullScoreAwareAveragingCombiner<Container>(),
                ContainerChildEquivalenceGenerator.NAME
            ))
            .withFilter(this.<Container>standardFilter())
            .withExtractor(PercentThresholdEquivalenceExtractor.<Container>moreThanPercent(90))
            .withHandler(containerResultHandlers(publishers))
            .build();
    }

    @Bean 
    public EquivalenceUpdater<Content> contentUpdater() {
        
        Set<Publisher> musicPublishers = ImmutableSet.of(BBC_MUSIC, YOUTUBE, 
            SPOTIFY, SOUNDCLOUD, RDIO, AMAZON_UK);
        
        //Generally acceptable publishers.
        Set<Publisher> acceptablePublishers = Sets.difference(
            Publisher.all(), 
            Sets.union(ImmutableSet.of(PREVIEW_NETWORKS, BBC_REDUX, RADIO_TIMES, LOVEFILM, NETFLIX, YOUVIEW), musicPublishers)
        );
        
        EquivalenceUpdater<Item> standardItemUpdater = standardItemUpdater(acceptablePublishers);
        EquivalenceUpdater<Container> standardContainerUpdater = standardContainerUpdater(acceptablePublishers);

        Set<Publisher> nonStandardPublishers = Sets.union(ImmutableSet.of(ITUNES, BBC_REDUX, RADIO_TIMES, FACEBOOK, LOVEFILM, NETFLIX, YOUVIEW), musicPublishers);
        final EquivalenceUpdaters updaters = new EquivalenceUpdaters();
        for (Publisher publisher : Iterables.filter(Publisher.all(), not(in(nonStandardPublishers)))) {
            updaters.register(publisher, Item.class, standardItemUpdater);    
            updaters.register(publisher, Container.class, standardContainerUpdater);
        }
        
        updaters.register(RADIO_TIMES, Item.class, rtItemEquivalenceUpdater());
        
        Set<Publisher> youViewPublishers = Sets.union(acceptablePublishers, ImmutableSet.of(YOUVIEW));
        updaters.register(YOUVIEW, Item.class, broadcastItemEquivalenceUpdater(youViewPublishers));
        updaters.register(YOUVIEW, Container.class, broadcastItemContainerEquivalenceUpdater(youViewPublishers));

        Set<Publisher> reduxPublishers = Sets.union(acceptablePublishers, ImmutableSet.of(BBC_REDUX));

        updaters.register(BBC_REDUX, Item.class, broadcastItemEquivalenceUpdater(reduxPublishers));
        updaters.register(BBC_REDUX, Container.class, broadcastItemContainerEquivalenceUpdater(reduxPublishers));
        
        Set<Publisher> facebookAcceptablePublishers = Sets.union(acceptablePublishers, ImmutableSet.of(FACEBOOK));
        updaters.register(FACEBOOK, Container.class, facebookContainerEquivalenceUpdater(facebookAcceptablePublishers));

        updaters.register(ITUNES, Item.class, vodItemUpdater(acceptablePublishers));
        updaters.register(ITUNES, Container.class, vodContainerUpdater(acceptablePublishers));

        Set<Publisher> lfPublishers = Sets.union(acceptablePublishers, ImmutableSet.of(LOVEFILM));
        updaters.register(LOVEFILM, Item.class, vodItemUpdater(lfPublishers));
        updaters.register(LOVEFILM, Container.class, vodContainerUpdater(lfPublishers));
        
        Set<Publisher> netflixPublishers = ImmutableSet.of(BBC, NETFLIX);
        updaters.register(NETFLIX, Item.class, vodItemUpdater(netflixPublishers));
        updaters.register(NETFLIX, Container.class, vodContainerUpdater(netflixPublishers));

        Set<Publisher> itunesAndMusicPublishers = Sets.union(musicPublishers, ImmutableSet.of(ITUNES));
        ContentEquivalenceUpdater<Item> muiscPublisherUpdater = ContentEquivalenceUpdater.<Item>builder()
            .withGenerator(
                new TitleSearchGenerator<Item>(searchResolver, Song.class, itunesAndMusicPublishers, new SongTitleTransform(), 100)
            ).withScorer(new CrewMemberScorer(new SongCrewMemberExtractor()))
            .withCombiner(new NullScoreAwareAveragingCombiner<Item>())
            .withFilter(AlwaysTrueFilter.<Item>get())
            .withExtractor(new MusicEquivalenceExtractor())
            .withHandler((EquivalenceResultHandler<Item>) new BroadcastingEquivalenceResultHandler<Item>(ImmutableList.of(
                EpisodeFilteringEquivalenceResultHandler.relaxed(
                    new LookupWritingEquivalenceHandler<Item>(lookupWriter, itunesAndMusicPublishers),
                    equivSummaryStore
                ),
                new ResultWritingEquivalenceHandler<Item>(equivalenceResultStore()),
                new EquivalenceSummaryWritingHandler<Item>(equivSummaryStore)
            )))
            .build();
        
        for (Publisher publisher : musicPublishers) {
            updaters.register(publisher, Item.class, muiscPublisherUpdater);
        }
        
        return updaters; 
    }

    private EquivalenceUpdater<Container> facebookContainerEquivalenceUpdater(Set<Publisher> facebookAcceptablePublishers) {
        return ContentEquivalenceUpdater.<Container> builder()
            .withGenerators(ImmutableSet.of(
                TitleSearchGenerator.create(searchResolver, Container.class, facebookAcceptablePublishers),
                aliasResolvingGenerator(contentResolver, Container.class)
            ))
            .withScorers(ImmutableSet.<EquivalenceScorer<Container>> of())
            .withCombiner(NullScoreAwareAveragingCombiner.<Container> get())
            .withFilter(ConjunctiveFilter.valueOf(ImmutableList.of(
                new MinimumScoreFilter<Container>(0.2),
                new SpecializationFilter<Container>()
            )))
            .withExtractor(PercentThresholdEquivalenceExtractor.<Container> moreThanPercent(90))
            .withHandler(containerResultHandlers(facebookAcceptablePublishers))
            .build();
    }

    private EquivalenceUpdater<Container> vodContainerUpdater(Set<Publisher> acceptablePublishers) {
        return ContentEquivalenceUpdater.<Container> builder()
            .withGenerator(
                TitleSearchGenerator.create(searchResolver, Container.class, acceptablePublishers)
            )
            .withScorers(ImmutableSet.of(
                new TitleMatchingContainerScorer(),
                new ContainerHierarchyMatchingScorer(contentResolver)
            ))
            .withCombiner(new RequiredScoreFilteringCombiner<Container>(
                new NullScoreAwareAveragingCombiner<Container>(),
                TitleMatchingContainerScorer.NAME)
            )
            .withFilter(this.<Container> standardFilter())
            .withExtractor(PercentThresholdEquivalenceExtractor.<Container> moreThanPercent(90))
            .withHandler(containerResultHandlers(acceptablePublishers))
            .build();
    }

    private EquivalenceUpdater<Item> vodItemUpdater(Set<Publisher> acceptablePublishers) {
        return ContentEquivalenceUpdater.<Item> builder()
            .withGenerator(
                new CandidateContainerEquivalenceGenerator(contentResolver, equivSummaryStore)
            )
            .withScorers(ImmutableSet.of(
                new TitleMatchingItemScorer(),
                new SequenceItemScorer()
            ))
            .withCombiner(new RequiredScoreFilteringCombiner<Item>(
                new NullScoreAwareAveragingCombiner<Item>(),
                TitleMatchingItemScorer.NAME
            ))
            .withFilter(this.<Item>standardFilter())
            .withExtractor(PercentThresholdEquivalenceExtractor.<Item> moreThanPercent(90))
            .withHandler(new BroadcastingEquivalenceResultHandler<Item>(ImmutableList.of(
                EpisodeFilteringEquivalenceResultHandler.strict(
                    new LookupWritingEquivalenceHandler<Item>(lookupWriter, acceptablePublishers),
                    equivSummaryStore
                ),
                new ResultWritingEquivalenceHandler<Item>(equivalenceResultStore()),
                new EquivalenceSummaryWritingHandler<Item>(equivSummaryStore)
            )))
            .build();
    }

    private EquivalenceUpdater<Container> broadcastItemContainerEquivalenceUpdater(Set<Publisher> reduxPublishers) {
        return ContentEquivalenceUpdater.<Container> builder()
            .withGenerators(ImmutableSet.of(
                TitleSearchGenerator.create(searchResolver, Container.class, reduxPublishers),
                new ContainerChildEquivalenceGenerator(contentResolver, equivSummaryStore)
            ))
            .withScorers(ImmutableSet.of(new TitleMatchingContainerScorer()))
            .withCombiner(new RequiredScoreFilteringCombiner<Container>(
                new NullScoreAwareAveragingCombiner<Container>(),
                ContainerChildEquivalenceGenerator.NAME))
            .withFilter(this.<Container>standardFilter())
            .withExtractor(PercentThresholdEquivalenceExtractor.<Container> moreThanPercent(90))
            .withHandler(containerResultHandlers(reduxPublishers))
            .build();
    }

    private EquivalenceUpdater<Item> broadcastItemEquivalenceUpdater(Set<Publisher> reduxPublishers) {
        return standardItemUpdater(reduxPublishers);
    }

    private EquivalenceUpdater<Item> rtItemEquivalenceUpdater() {
        return ContentEquivalenceUpdater.<Item> builder()
            .withGenerators(ImmutableSet.of(
                new RadioTimesFilmEquivalenceGenerator(contentResolver),
                new FilmEquivalenceGenerator(searchResolver)
            ))
            .withScorers(ImmutableSet.<EquivalenceScorer<Item>> of())
            .withCombiner(new NullScoreAwareAveragingCombiner<Item>())
            .withFilter(this.<Item>standardFilter())
            .withExtractor(PercentThresholdEquivalenceExtractor.<Item> moreThanPercent(90))
            .withHandler(new BroadcastingEquivalenceResultHandler<Item>(ImmutableList.of(
                EpisodeFilteringEquivalenceResultHandler.relaxed(
                    new LookupWritingEquivalenceHandler<Item>(lookupWriter, 
                        ImmutableSet.of(RADIO_TIMES, PA, PREVIEW_NETWORKS)),
                        equivSummaryStore
                ),
                new ResultWritingEquivalenceHandler<Item>(equivalenceResultStore()),
                new EquivalenceSummaryWritingHandler<Item>(equivSummaryStore)
            )))
            .build();
    }

}
