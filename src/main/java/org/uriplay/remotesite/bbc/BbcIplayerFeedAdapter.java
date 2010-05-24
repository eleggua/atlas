/* Copyright 2009 British Broadcasting Corporation
   Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.uriplay.remotesite.bbc;

import java.util.regex.Pattern;

import javax.xml.bind.JAXBException;

import org.uriplay.media.entity.Playlist;
import org.uriplay.persistence.system.RemoteSiteClient;
import org.uriplay.remotesite.ContentExtractor;
import org.uriplay.remotesite.SiteSpecificAdapter;
import org.uriplay.remotesite.synd.SyndicationAdapter;
import org.uriplay.remotesite.synd.SyndicationFeedClient;
import org.uriplay.remotesite.synd.SyndicationSource;

import com.sun.syndication.feed.synd.SyndFeed;

/**
 * {@link SiteSpecificAdapter} processing iPlayer's RSS feeds.
 *  
 * @author Robert Chatley (robert@metabroadcast.com)
 * @author John Ayres (john@metabroadcast.com)
 */
public class BbcIplayerFeedAdapter extends SyndicationAdapter<Playlist> implements SiteSpecificAdapter<Playlist> {

	private final Pattern atozUriPattern = Pattern.compile("http://feeds.bbc.co.uk/iplayer/([^/]+|atoz/[a-z]|atoz/0-9)/list");
	private final Pattern highlightsFeedPattern = Pattern.compile("http://feeds.bbc.co.uk/iplayer/(popular|highlights)/(tv|radio)");
	
	public BbcIplayerFeedAdapter() throws JAXBException {
		this(new SyndicationFeedClient(), new BbcIplayerGraphExtractor());
	}
	
	protected BbcIplayerFeedAdapter(RemoteSiteClient<SyndFeed> feedClient, ContentExtractor<SyndicationSource, Playlist> contentExtractor) {
		super(feedClient, contentExtractor);
	}
	
	public boolean canFetch(String uri) {
		return atozUriPattern.matcher(uri).matches() || highlightsFeedPattern.matcher(uri).matches();
	}

}
