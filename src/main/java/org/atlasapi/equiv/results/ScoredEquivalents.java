package org.atlasapi.equiv.results;

import java.util.Map;

import org.atlasapi.media.entity.Content;

import com.google.common.base.Function;

public interface ScoredEquivalents<T extends Content> {

    String source();

    Map<T, Score> equivalents();
    
    public static final Function<ScoredEquivalents<?>, String> TO_SOURCE = new Function<ScoredEquivalents<?>, String>() {
        @Override
        public String apply(ScoredEquivalents<?> input) {
            return input.source();
        }
    };
 
}