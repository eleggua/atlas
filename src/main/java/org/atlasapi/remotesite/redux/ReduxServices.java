package org.atlasapi.remotesite.redux;

import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableBiMap.Builder;

public final class ReduxServices {

	private static String REDUX_URI_PREFIX = "http://devapi.bbcredux.com/channels/";
	private static Pattern REDUX_URI_MATCHER = Pattern.compile("^" + REDUX_URI_PREFIX + "(\\w+)$");

	private final BiMap<String, Channel> reduxChannelMap;
	
	public ReduxServices(ChannelResolver channelResolver) {
		Builder<String, Channel> map = ImmutableBiMap.builder();
		for (Entry<String, Channel> entry: channelResolver.forAliases(REDUX_URI_PREFIX).entrySet()) {
        	Matcher m = REDUX_URI_MATCHER.matcher(entry.getKey());
        	if(m.matches()) {
        		String channel = m.group(1);
        		map.put(channel, entry.getValue());
        	}
        }
		reduxChannelMap = map.build();
	}
	
	public BiMap<String, Channel> channelMap() {
		return reduxChannelMap;
	}
    
}
