package org.atlasapi.output.annotation;


import java.io.IOException;

import org.atlasapi.media.content.Content;
import org.atlasapi.output.FieldWriter;
import org.atlasapi.output.OutputContext;


public class SegmentEventsAnnotation extends OutputAnnotation<Content> {

    public SegmentEventsAnnotation() {
        super(Content.class);
    }

    @Override
    public void write(Content entity, FieldWriter format, OutputContext ctxt) throws IOException {
        // TODO Auto-generated method stub
        
    }

}