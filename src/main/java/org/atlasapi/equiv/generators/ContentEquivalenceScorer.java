package org.atlasapi.equiv.generators;

import org.atlasapi.equiv.results.ScoredEquivalents;
import org.atlasapi.media.entity.Content;

public interface ContentEquivalenceScorer<T extends Content> {

    ScoredEquivalents<T> score(T content, Iterable<T> suggestions);
    
}
