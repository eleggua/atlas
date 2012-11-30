package org.atlasapi.output;

import java.util.Set;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.media.entity.ChannelSchedule;
import org.atlasapi.media.entity.simple.ScheduleQueryResult;
import org.atlasapi.output.simple.ChannelSimplifier;
import org.atlasapi.output.simple.ItemModelSimplifier;

import com.google.common.collect.ImmutableList;

/**
 * {@link AtlasModelWriter} that translates the full URIplay object model
 * into a simplified form and renders that as XML.
 *  
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class SimpleScheduleModelWriter extends TransformingModelWriter<Iterable<ChannelSchedule>, ScheduleQueryResult> {

    private final ItemModelSimplifier itemModelSimplifier;
    private final ChannelSimplifier channelSimplifier;

	public SimpleScheduleModelWriter(AtlasModelWriter<ScheduleQueryResult> outputter, ItemModelSimplifier itemModelSimplifier, ChannelSimplifier channelSimplifier) {
		super(outputter);
        this.itemModelSimplifier = itemModelSimplifier;
        this.channelSimplifier = channelSimplifier;
	}
	
	@Override
    protected ScheduleQueryResult transform(Iterable<ChannelSchedule> fullGraph, Set<Annotation> annotations, ApplicationConfiguration config) {
        ScheduleQueryResult outputGraph = new ScheduleQueryResult();
	    for (ChannelSchedule scheduleChannel : fullGraph) {
	        outputGraph.add(scheduleChannelFrom(scheduleChannel, annotations, config));
	    }
	    return outputGraph;
	}

	org.atlasapi.media.entity.simple.ScheduleChannel scheduleChannelFrom(ChannelSchedule scheduleChannel, Set<Annotation> annotations, ApplicationConfiguration config) {
	    org.atlasapi.media.entity.simple.ScheduleChannel newScheduleChannel = new org.atlasapi.media.entity.simple.ScheduleChannel();
	    newScheduleChannel.setChannelUri(scheduleChannel.channel().uri());
	    newScheduleChannel.setChannelKey(scheduleChannel.channel().key());
	    newScheduleChannel.setChannelTitle(scheduleChannel.channel().title());
	    
	    if (annotations.contains(Annotation.CHANNEL)) {
	        newScheduleChannel.setChannel(channelSimplifier.simplify(scheduleChannel.channel(), false, false, false));
	    }
	    
	    ImmutableList.Builder<org.atlasapi.media.entity.simple.Item> items = ImmutableList.builder();
	    for (org.atlasapi.media.entity.Item item: scheduleChannel.items()) {
	        items.add(itemModelSimplifier.simplify(item, annotations, config));
	    }
	    
	    newScheduleChannel.setItems(items.build());
	    return newScheduleChannel;
	}

}
