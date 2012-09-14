package org.atlasapi.equiv.generators;

import java.util.Map;
import java.util.Set;

import org.atlasapi.equiv.results.EquivalenceResult;
import org.atlasapi.equiv.results.scores.DefaultScoredEquivalents;
import org.atlasapi.equiv.results.scores.DefaultScoredEquivalents.ScoredEquivalentsBuilder;
import org.atlasapi.equiv.results.scores.Score;
import org.atlasapi.equiv.results.scores.ScoredCandidate;
import org.atlasapi.equiv.results.scores.ScoredCandidates;
import org.atlasapi.media.entity.Container;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.persistence.content.ContentResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.metabroadcast.common.base.Maybe;

public class ItemResultContainerResolver {
    
    private static final Logger log = LoggerFactory.getLogger(ItemResultContainerResolver.class);

    private final ContentResolver contentResolver;
    private final String source;

    public ItemResultContainerResolver(ContentResolver contentResolver, String source) {
        this.contentResolver = contentResolver;
        this.source = source;
    }
    
    /* Calculates equivalence scores for the containers of items that are strongly equivalent to the items of the subject container.
     * Scores are normalized by the number of items in the container. 
     */
    public ScoredCandidates<Container> extractContainersFrom(Set<EquivalenceResult<Item>> childResults) {

        //Local cache, hopefully the same containers will be resolved multiple times.
        Map<String, Maybe<Container>> containerCache = Maps.newHashMap();
        
        ScoredEquivalentsBuilder<Container> results = DefaultScoredEquivalents.fromSource(source);
        
        for (EquivalenceResult<Item> equivalenceResult : childResults) {
            for (ScoredCandidate<Item> strongEquivalent : equivalenceResult.strongEquivalences().values()) {
                Score score = strongEquivalent.score();
                if (score.isRealScore()) {
                    ParentRef parentEquivalent = strongEquivalent.candidate().getContainer();
                    Maybe<Container> resolvedContainer = resolve(parentEquivalent, containerCache);

                    if (resolvedContainer.hasValue()) {
                        Container container = resolvedContainer.requireValue();
                        results.addEquivalent(container, Score.valueOf(score.asDouble() / container.getChildRefs().size()));
                    }
                }
            }
        }
        
        return results.build();
    }
    
    //Resolve a container, looking in the cache first.
    private Maybe<Container> resolve(ParentRef parentEquivalent, Map<String, Maybe<Container>> containerCache) {
        if(parentEquivalent == null) {
            return Maybe.nothing();
        }
        
        String uri = parentEquivalent.getUri();
        
        Maybe<Container> cached = containerCache.get(uri);
        
        if (cached != null) {
            return cached;
        }
        
        Maybe<Identified> resolved = contentResolver.findByCanonicalUris(ImmutableList.of(uri)).get(uri);
        
        if(resolved.isNothing() || !(resolved.requireValue() instanceof Container)) {
            log.warn("Couldn't resolve container {}", uri);
            return Maybe.nothing();
        }
        
        Maybe<Container> result = Maybe.just((Container) resolved.requireValue());
        containerCache.put(uri, result);
        
        return result;
    }
    
}
