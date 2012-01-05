package org.atlasapi.remotesite.pa;

import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;

import com.google.common.collect.Maps;
import com.metabroadcast.common.base.Maybe;


public class PaChannelMap {

	private static final String PA_URI_PREFIX = "http://pressassociation.com/channels/";
	private static Pattern PA_URI_MATCHER = Pattern.compile("^" + PA_URI_PREFIX + "(\\d+)$");

    private final Map<Integer, Channel> channelMap = Maps.newHashMap();
    private final Map<Channel, Integer> reverseMap = Maps.newHashMap();
    
    
    public PaChannelMap(ChannelResolver channelResolver) {
        for (Entry<String, Channel> entry: channelResolver.forAliases(PA_URI_PREFIX).entrySet()) {
        	Matcher m = PA_URI_MATCHER.matcher(entry.getKey());
        	if(m.matches()) {
        		Integer channel = Integer.decode(m.group(1));
        		channelMap.put(channel, entry.getValue());
        		reverseMap.put(entry.getValue(), channel);
        	}
        }
    }
    
    public String getChannelUri(int channelId) {
        if (channelMap.containsKey(channelId)) {
            return channelMap.get(channelId).uri();
        }
        return null;
    }
    
    public Maybe<Channel> getChannel(int channelId) {
        if (channelMap.containsKey(channelId)) {
            return Maybe.fromPossibleNullValue(channelMap.get(channelId));
        }
        return Maybe.nothing();
    }
}
