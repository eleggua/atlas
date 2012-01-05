/* Copyright 2010 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */


package org.atlasapi.remotesite.channel4;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.system.RemoteSiteClient;

import com.metabroadcast.common.http.HttpStatusCodeException;
import com.sun.syndication.feed.atom.Feed;

public class C4AtomBackedBrandUpdater implements C4BrandUpdater {

	private static final Pattern BRAND_PAGE_PATTERN = Pattern.compile("http://www.channel4.com/programmes/([^/\\s]+)(/4od)?");

	private final Log log = LogFactory.getLog(getClass());
	
	private final RemoteSiteClient<Feed> feedClient;
	private final C4BrandExtractor extractor;
	
	public C4AtomBackedBrandUpdater(RemoteSiteClient<Feed> atomClient, ContentResolver contentResolver, ContentWriter contentStore, ChannelResolver channelResolver, C4LakeviewOnDemandFetcher lakeviewFetcher, AdapterLog log) {
		this(atomClient, new C4BrandExtractor(atomClient, contentResolver, contentStore, channelResolver, lakeviewFetcher, log));
	}
	
	public C4AtomBackedBrandUpdater(RemoteSiteClient<Feed> feedClient, C4BrandExtractor extractor) {
		this.feedClient = feedClient;
		this.extractor = extractor;
	}
	
	@Override
	public boolean canFetch(String uri) {
		return BRAND_PAGE_PATTERN.matcher(uri).matches();
	}

	public Brand createOrUpdateBrand(String uri) {
		if (!canFetch(uri)) {
			throw new IllegalArgumentException("Cannot fetch C4 uri: " + uri + " as it is not in the expected format: " + BRAND_PAGE_PATTERN.toString());
		}
		try {
			log.info("Fetching C4 brand " + uri);
			return extractor.write(feedClient.get(atomUrl(uri)));
		} catch (HttpStatusCodeException e) {
			if (HttpServletResponse.SC_NOT_FOUND == e.getStatusCode()) {
				// ignore
			}
			throw new RuntimeException(e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String atomUrl(String uri) {
		Matcher matcher = BRAND_PAGE_PATTERN.matcher(uri);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Cannot fetch " + uri + " because it is not a valid C4 brand uri");
		}
		String webSafeName = matcher.group(1);
		return C4AtomApi.createBrandRequest(webSafeName, ".atom");
	}
}
