package org.atlasapi.remotesite.channel4;

import java.util.Set;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.query.content.PerPublisherCurieExpander;
import org.atlasapi.remotesite.ContentExtractor;
import org.jdom.Element;
import org.joda.time.DateTime;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.metabroadcast.common.time.DateTimeZones;
import com.sun.syndication.feed.atom.Category;
import com.sun.syndication.feed.atom.Feed;
import com.sun.syndication.feed.atom.Link;

public class C4BrandBasicDetailsExtractor implements ContentExtractor<Feed, Brand> {

	private static final String PRESENTATION_BRAND = "relation.presentationBrand";
	private final C4AtomApi c4AtomApi;

	public C4BrandBasicDetailsExtractor(ChannelResolver channelResolver) {
		this.c4AtomApi = new C4AtomApi(channelResolver);
	}
	
    @Override
	public Brand extract(Feed source) {
		
		Preconditions.checkArgument(C4AtomApi.isABrandFeed(source), "Not a brand feed");
		
		String brandUri = brandUriFrom(source);
		
		Preconditions.checkArgument(brandUri != null && C4AtomApi.isACanonicalBrandUri(brandUri), "URI of feed is not a canonical Brand URI, got: " + brandUri);

		Brand brand = new Brand(brandUri, PerPublisherCurieExpander.CurieAlgorithm.C4.compact(brandUri), Publisher.C4);

		brand.addAlias(brandUri + "/4od");
		brand.addAlias(source.getId());

		brand.setTitle(source.getTitle());
		brand.setDescription(source.getSubtitle().getValue());
		
		brand.setLastUpdated(new DateTime(source.getUpdated(), DateTimeZones.UTC));
		brand.setMediaType(MediaType.VIDEO);
		brand.setSpecialization(Specialization.TV);
		
		Set<String> genres = Sets.newHashSet();
		for (Object cat : source.getCategories()) {
            Category category = (Category) cat;
            genres.add(category.getTerm().replaceAll(".atom", "").replaceAll("/pmlsd/", "/programmes/"));
        }
		brand.setGenres(new C4CategoryGenreMap().mapRecognised(genres));
		
		String presentationBrand = getPresentationBrand(source);
		if(presentationBrand != null) {
		    brand.setPresentationChannel(c4AtomApi.getChannelMap().get(presentationBrand));
		}
		
		C4AtomApi.addImages(brand, source.getLogo());
		
		return brand;
	}
	
	@SuppressWarnings("unchecked")
    private String getPresentationBrand(Feed source) {
	    Iterable<Element> markup = (Iterable<Element>) source.getForeignMarkup();
        for (Element element : markup) {
            if (PRESENTATION_BRAND.equals(element.getName())) {
                return element.getValue();
            }
        }
        return null;
    }

    private String brandUriFrom(Feed source) {
		if (source.getAlternateLinks().isEmpty()) {
			return null;
		}
		return ((Link) source.getAlternateLinks().get(0)).getHref();
	}
}
