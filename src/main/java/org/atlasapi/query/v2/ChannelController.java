package org.atlasapi.query.v2;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.simple.ChannelQueryResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Flushables;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.ids.NumberToShortStringCodec;
import com.metabroadcast.common.query.Selection;
import com.metabroadcast.common.query.Selection.SelectionBuilder;

@Controller
// TODO transplant on to BaseController when annotations are available.
public class ChannelController {

    private static final SelectionBuilder SELECTION_BUILDER = Selection.builder().withMaxLimit(50).withDefaultLimit(10);
    private static final Splitter CSV_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    private final ChannelResolver channelResolver;
    private final NumberToShortStringCodec idCodec;
    private final ChannelSimplifier channelSimplifier;
    
    private Gson gson;

    public ChannelController(ChannelResolver channelResolver, NumberToShortStringCodec idCodec, ChannelSimplifier channelSimplifier) {
        this.channelResolver = channelResolver;
        this.idCodec = idCodec;
        this.channelSimplifier = channelSimplifier;
        this.gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    @RequestMapping("/3.0/channels.json")
    public void listChannels(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Selection selection = SELECTION_BUILDER.build(request);

        writeOut(response, request, new ChannelQueryResult(channelSimplifier.simplify(selection.applyTo(channelResolver.all()),showChannelGroups(request))));
    }
    
    @RequestMapping("/3.0/channels/{id}.json")
    public void listChannel(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String id) throws IOException {

        Maybe<Channel> possibleChannel = channelResolver.fromId(idCodec.decode(id).longValue());
        
        if (possibleChannel.isNothing()) {
            response.sendError(HttpStatusCode.NOT_FOUND.code());
        }
        
        writeOut(response, request, new ChannelQueryResult(channelSimplifier.simplify(ImmutableList.of(possibleChannel.requireValue()),showChannelGroups(request))));
    }
    
    public boolean showChannelGroups(HttpServletRequest request) {
        return ImmutableSet.copyOf(CSV_SPLITTER.split(Strings.nullToEmpty(request.getParameter("annotations")))).contains("channel_groups");
    }
    
    private void writeOut(HttpServletResponse response, HttpServletRequest request, ChannelQueryResult channelQueryResult) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), Charsets.UTF_8);
        boolean ignoreEx = true;
        try {
            gson.toJson(channelQueryResult, writer);
            ignoreEx = false;
        } finally {
            Flushables.flush(writer, ignoreEx);
        }
    }

    

}
