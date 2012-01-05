package org.atlasapi.query.v2;

import java.io.IOException;
import java.io.OutputStreamWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.media.channel.ChannelGroup;
import org.atlasapi.media.channel.ChannelGroupStore;
import org.atlasapi.media.entity.simple.ChannelGroupQueryResult;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.google.common.base.Charsets;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.Flushables;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.metabroadcast.common.http.HttpStatusCode;
import com.metabroadcast.common.ids.NumberToShortStringCodec;
import com.metabroadcast.common.query.Selection;
import com.metabroadcast.common.query.Selection.SelectionBuilder;

@Controller
// TODO transplant on to basecontroller when annotations are available.
public class ChannelGroupController {

    private static final SelectionBuilder SELECTION_BUILDER = Selection.builder().withMaxLimit(50).withDefaultLimit(10);
    private static final Splitter CSV_SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

    private final ChannelGroupStore channelGroupResolver;
    private final ChannelSimplifier simplifier;
    private final NumberToShortStringCodec idCodec;

    private final Gson gson;

    public ChannelGroupController(ChannelGroupStore channelGroupResolver, NumberToShortStringCodec idCodec, ChannelSimplifier simplifier) {
        this.channelGroupResolver = channelGroupResolver;
        this.idCodec = idCodec;
        this.simplifier = simplifier;
        this.gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();
    }

    @RequestMapping("/3.0/channels_groups.json")
    public void listChannels(HttpServletRequest request, HttpServletResponse response) throws IOException {

        Selection selection = SELECTION_BUILDER.build(request);
        
        writeOut(response, request, new ChannelGroupQueryResult(simplifier.simplify(selection.applyTo(channelGroupResolver.channelGroups()), showChannels(request))));
    }

    public boolean showChannels(HttpServletRequest request) {
        return ImmutableSet.copyOf(CSV_SPLITTER.split(Strings.nullToEmpty(request.getParameter("annotations")))).contains("channels");
    }
    
    @RequestMapping("/3.0/channels_groups/{id}.json")
    public void listChannel(HttpServletRequest request, HttpServletResponse response, @PathVariable("id") String id) throws IOException {

        Optional<ChannelGroup> possibleChannelGroup = channelGroupResolver.channelGroupFor(idCodec.decode(id).longValue());
        
        if (!possibleChannelGroup.isPresent()) {
            response.sendError(HttpStatusCode.NOT_FOUND.code());
        }
        
        writeOut(response, request, new ChannelGroupQueryResult(simplifier.simplify(ImmutableList.of(possibleChannelGroup.get()), showChannels(request))));
    }
    
    private void writeOut(HttpServletResponse response, HttpServletRequest request, ChannelGroupQueryResult channelGroupQueryResult) throws IOException {
        OutputStreamWriter writer = new OutputStreamWriter(response.getOutputStream(), Charsets.UTF_8);
        boolean ignoreEx = true;
        try {
            gson.toJson(channelGroupQueryResult, writer);
            ignoreEx = false;
        } finally {
            Flushables.flush(writer, ignoreEx);
        }
    }

}
