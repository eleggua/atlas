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

package org.atlasapi.remotesite.bbc;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import java.util.NoSuchElementException;
import java.util.Set;

import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Description;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.SiteSpecificAdapter;
import org.atlasapi.remotesite.bbc.BbcIplayerGraphExtractor;
import org.atlasapi.remotesite.bbc.SlashProgrammesRdf;
import org.atlasapi.remotesite.bbc.SlashProgrammesVersionRdf;
import org.atlasapi.remotesite.bbc.SlashProgrammesRdf.SlashProgrammesContainerRef;
import org.atlasapi.remotesite.bbc.SlashProgrammesRdf.SlashProgrammesEpisode;
import org.atlasapi.remotesite.bbc.SlashProgrammesRdf.SlashProgrammesVersion;
import org.atlasapi.remotesite.synd.SyndicationSource;
import org.jmock.Expectations;
import org.jmock.integration.junit3.MockObjectTestCase;
import org.joda.time.DateTime;
import org.springframework.core.io.ClassPathResource;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.metabroadcast.common.base.Maybe;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;

/**
 * @author Robert Chatley (robert@metabroadcast.com)
 */
@SuppressWarnings("unchecked")
public class BbcIplayerGraphExtractorTest extends MockObjectTestCase {
	
	static final String FEED_URI = "http://feeds.bbc.co.uk/iplayer/atoz/a/list";
	
	static final String BRAND_URI = "http://www.bbc.co.uk/programmes/b006v04h";
	
	static final Brand BRAND = new Brand(BRAND_URI, "curie", Publisher.BBC); {{ BRAND.setTitle("Spotlight"); }}
	
	static final String EPISODE_2_URI = "http://www.bbc.co.uk/programmes/b00kjbrc";

	static final String ORPHAN_ITEM_URI = "http://www.bbc.co.uk/programmes/b00kfr9s";

	DateTime tuesday10pm = new DateTime(2009, 04, 21, 22, 00, 00, 00);
	
	RemoteSiteClient<SlashProgrammesRdf> episodeRdfClient = mock(RemoteSiteClient.class, "episodeClient");
	RemoteSiteClient<SlashProgrammesVersionRdf> versionRdfClient = mock(RemoteSiteClient.class, "versionClient");
	SiteSpecificAdapter<Content> brandClient = mock(SiteSpecificAdapter.class);
	
	SlashProgrammesVersion version = new SlashProgrammesVersion().withResourceUri("/programmes/b00k2vtr#programme");
			
	SlashProgrammesEpisode episode = new SlashProgrammesEpisode().inPosition(6)
																 .withVersion(version)
																 .withDescription("Claire Savage investigates a workers sit-in against those that shut the Visteon plant down")
	                                                             .withGenres("/programmes/genres/factual/politics#genre", "/programmes/genres/news#genre")
																 .withTitle("Shutdown");

	SlashProgrammesContainerRef brandRef = new SlashProgrammesContainerRef().withUri("/programmes/b001#programme");
	SlashProgrammesRdf episode2Rdf = new SlashProgrammesRdf().withEpisode(episode).withBrand(brandRef);
	SlashProgrammesVersionRdf episode2versionRdf = new SlashProgrammesVersionRdf().withLastTransmitted(tuesday10pm, "/bbctwo#service");
	
	BbcIplayerGraphExtractor extractor;
	SyndFeed feed;
	SyndicationSource source;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		feed = createFeed("bbc-one-feed.atom.xml");
		source = new SyndicationSource(feed, FEED_URI);
		BbcSeriesNumberResolver seriesResolver = new BbcSeriesNumberResolver() {
			
			@Override
			public Maybe<Integer> seriesNumberFor(String seriesUri) {
				return Maybe.nothing();
			}
		};
		
		BbcProgrammesPolicyClient policyClient = new BbcProgrammesPolicyClient() {
			@Override
			public Maybe<Policy> policyForUri(String episodeUri) {
				return Maybe.nothing();
			}
		};
		ContentExtractor<BbcProgrammeSource, Item> contentExtractor = new BbcProgrammeGraphExtractor(seriesResolver, policyClient);
		extractor = new BbcIplayerGraphExtractor(episodeRdfClient, versionRdfClient, brandClient, contentExtractor);
	}
	
	public void testCreatesEpisodesFromFeedEntries() throws Exception {
		
		checking(new Expectations() {{ 
			atLeast(1).of(episodeRdfClient).get(EPISODE_2_URI + ".rdf"); will(returnValue(episode2Rdf));
			allowing(episodeRdfClient).get(with(not(startsWith(EPISODE_2_URI)))); will(returnValue(new SlashProgrammesRdf().withEpisode(new SlashProgrammesEpisode())));

			atLeast(1).of(brandClient).fetch(BRAND_URI); will(returnValue(BRAND));
			allowing(brandClient).fetch(with(not(startsWith(BRAND_URI)))); will(returnValue(new Brand()));
			
			atLeast(1).of(versionRdfClient).get("http://www.bbc.co.uk/programmes/b00k2vtr.rdf"); will(returnValue(episode2versionRdf));
		}});
		
		Playlist playlist = extractor.extract(source);
		assertThat(playlist.getCanonicalUri(), is(FEED_URI));
		assertThat(playlist.getCurie(), is("bbc:atoz_a"));
		assertThat(playlist.getPublisher(), is(Publisher.BBC));

		System.out.println(playlist.getPlaylists());
		
		Brand brand = (Brand) byUri(BRAND_URI, playlist.getPlaylists());
		
		assertThat(brand.getCanonicalUri(), is(BRAND_URI));
		assertThat(brand.getTitle(), is("Spotlight"));
		
		Episode episode = (Episode) Iterables.getOnlyElement(brand.getItems());
		
		assertThat(episode.getCanonicalUri(), is(EPISODE_2_URI));
		assertThat(episode.getBrand(), is(brand));
		
		assertThat(episode.getTitle(), is("Shutdown"));
		assertThat(episode.getDescription(), is("Claire Savage investigates a workers sit-in against those that shut the Visteon plant down"));
		
		assertThat(episode.getEpisodeNumber(), is(6));
		
		assertThat(episode.getPublisher(), is(Publisher.BBC));
	
		
		Set<String> expectedGenres = bbcGenreUris("factual/politics", "news");
		expectedGenres.addAll(atlasGenreUris("news", "factual"));
		
		assertThat(episode.getGenres(), is(expectedGenres));
		assertThat(episode.getIsLongForm(), is(true));
		
		assertThat(episode.getThumbnail(), is("http://www.bbc.co.uk/iplayer/images/episode/b00kjbrc_150_84.jpg"));
		
		Version version = Iterables.getOnlyElement(episode.getVersions());
		Encoding encoding = Iterables.getOnlyElement(version.getManifestedAs());
		
		Broadcast broadcast = Iterables.getOnlyElement(version.getBroadcasts());
		
		assertThat(broadcast.getBroadcastOn(), is("http://www.bbc.co.uk/bbctwo"));
		
		Location location = Iterables.getOnlyElement(encoding.getAvailableAt());
		assertThat(location.getUri(), is("http://www.bbc.co.uk/iplayer/episode/b00kjbrc"));
		
		assertThat(location.getTransportType(), is(TransportType.LINK));

		Item orphan = byUri(ORPHAN_ITEM_URI, playlist.getItems());
		
		// Check not a subclass
		assertEquals(Item.class, orphan.getClass());
	}

	private <T extends Description> T byUri(String uri, Iterable<T> playlists) {
		for (T description : playlists) {
			if (uri.equals(description.getCanonicalUri())) {
				return description;
			}
		}
		throw new NoSuchElementException();
	}

	private Set<String> bbcGenreUris(String... genres) {
		Set<String> uris = Sets.newHashSetWithExpectedSize(genres.length);
		for (String genre : genres) {
			uris.add("http://www.bbc.co.uk/programmes/genres/" + genre);
		}
		return uris;
	}
	
	private Set<String> atlasGenreUris(String... genres) {
		Set<String> uris = Sets.newHashSetWithExpectedSize(genres.length);
		for (String genre : genres) {
			uris.add("http://ref.atlasapi.org/genres/atlas/" + genre);
		}
		return uris;
	}
	
	SyndFeed createFeed(String filename) throws Exception {
		SyndFeedInput input = new SyndFeedInput();
		SyndFeed feed = input.build(new XmlReader(new ClassPathResource(filename).getInputStream()));
		return feed; 
	}
}
