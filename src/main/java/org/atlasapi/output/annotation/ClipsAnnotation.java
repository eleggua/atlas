package org.atlasapi.output.annotation;


import java.io.IOException;

import org.atlasapi.media.entity.Content;
import org.atlasapi.query.v4.schedule.FieldWriter;
import org.atlasapi.query.v4.schedule.OutputContext;


public class ClipsAnnotation extends OutputAnnotation<Content> {

    public ClipsAnnotation() {
        super(Content.class);
    }

    @Override
    public void write(Content entity, FieldWriter format, OutputContext ctxt) throws IOException {
        
    }

}
