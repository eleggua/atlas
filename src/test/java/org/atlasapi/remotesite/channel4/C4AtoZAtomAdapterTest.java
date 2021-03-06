/* Copyright 2009 Meta Broadcast Ltd

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

import junit.framework.TestCase;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.io.Resources;
import com.sun.syndication.feed.atom.Feed;
/**
 * Unit test for {@link C4AtoZAtomContentLoader}.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
@RunWith(JMock.class)
public class C4AtoZAtomAdapterTest extends TestCase {

    private final Mockery context = new Mockery();
    
	String uri = "http://www.channel4.com/programmes/atoz/a";
	
	private C4BrandUpdater brandAdapter;
	private RemoteSiteClient<Feed> itemClient;
	private C4AtoZAtomContentLoader adapter;
	private ContentWriter writer;
	
	Brand brand101 = new Brand("http://www.channel4.com/programmes/a-bipolar-expedition", "curie:101", Publisher.C4);
	Brand brand202 = new Brand("http://www.channel4.com/programmes/a-bipolar-expedition-part-2", "curie:202", Publisher.C4);
	
	private final AtomFeedBuilder atoza = new AtomFeedBuilder(Resources.getResource(getClass(), "a.atom"));
	private final AtomFeedBuilder atoza2 = new AtomFeedBuilder(Resources.getResource(getClass(), "a2.atom"));

	
	@SuppressWarnings("unchecked")
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		brandAdapter = context.mock(C4BrandUpdater.class);
		itemClient = context.mock(RemoteSiteClient.class);
		writer = context.mock(ContentWriter.class);
		adapter = new C4AtoZAtomContentLoader(itemClient, brandAdapter, new NullAdapterLog());
	}

    @Test
	public void testPerformsGetCorrespondingGivenUriAndPassesResultToExtractor() throws Exception {
		
		context.checking(new Expectations() {{
			one(itemClient).get("http://api.channel4.com/pmlsd/atoz/a.atom"); will(returnValue(atoza.build()));
			allowing(brandAdapter).canFetch("http://www.channel4.com/programmes/a-bipolar-expedition"); will(returnValue(true));
			allowing(brandAdapter).createOrUpdateBrand("http://www.channel4.com/programmes/a-bipolar-expedition"); //will(returnValue(brand101));

			one(itemClient).get("http://api.channel4.com/pmlsd/atoz/a/page-2.atom"); will(returnValue(atoza2.build()));
			allowing(brandAdapter).canFetch("http://www.channel4.com/programmes/a-bipolar-expedition-part-2"); will(returnValue(true));
			allowing(brandAdapter).createOrUpdateBrand("http://www.channel4.com/programmes/a-bipolar-expedition-part-2"); //will(returnValue(brand202));
		}});
		
		adapter.loadAndSaveByLetter("a");
	}
}
