package org.atlasapi.equiv.results.filters;

import java.util.List;

import org.atlasapi.equiv.results.description.ResultDescription;
import org.atlasapi.equiv.results.scores.ScoredCandidate;

public interface EquivalenceFilter<T> {
    
    List<ScoredCandidate<T>> apply(Iterable<ScoredCandidate<T>> candidates, T subject, ResultDescription desc);
    
}
