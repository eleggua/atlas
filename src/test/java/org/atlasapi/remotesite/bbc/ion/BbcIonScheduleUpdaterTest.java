package org.atlasapi.remotesite.bbc.ion;

import static org.atlasapi.remotesite.bbc.ion.BbcIonDeserializers.deserializerForClass;
import static org.hamcrest.core.AllOf.allOf;
import junit.framework.TestCase;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.content.people.DummyItemsPeopleWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.SystemOutAdapterLog;
import org.atlasapi.remotesite.bbc.ion.BbcIonDeserializers.BbcIonDeserializer;
import org.atlasapi.remotesite.bbc.ion.model.IonSchedule;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.io.Resources;
import com.metabroadcast.common.http.SimpleHttpClient;

public class BbcIonScheduleUpdaterTest extends TestCase {

    private static final String SLASH_PROGRAMMES_ROOT = "http://www.bbc.co.uk/programmes/";
    
    private Mockery context = new Mockery();
    
    private final BbcIonDeserializer<IonSchedule> deserialiser = deserializerForClass(IonSchedule.class);
    
    private final SimpleHttpClient httpClient = context.mock(SimpleHttpClient.class);
    private final ContentResolver resolver = context.mock(ContentResolver.class);
    private final ContentWriter writer = context.mock(ContentWriter.class);
    private final AdapterLog log = new SystemOutAdapterLog(); 
    private final DummyItemsPeopleWriter itemsPeopleWriter = new DummyItemsPeopleWriter();
    
    @SuppressWarnings("unchecked")
    public void testProcessNewItemWithNoBrandOrSeries() throws Exception {
        final String newItemNoBrandNoSeriesJson = Resources.toString(Resources.getResource("ion-item-no-brand-no-series.json"), Charsets.UTF_8);

        context.checking(new Expectations(){{
            one(httpClient).getContentsOf("uri");will(returnValue(newItemNoBrandNoSeriesJson));
            one(resolver).findByCanonicalUri(SLASH_PROGRAMMES_ROOT + "b00y377q");will(returnValue(null));
            one(writer).createOrUpdate((Item)with(allOf(
                    uri(SLASH_PROGRAMMES_ROOT+"b00y377q"),
                    title("Pleasure and Pain with Michael Mosley"),
                    version(uri(SLASH_PROGRAMMES_ROOT+"b00y3770")))));
        }});

        new BbcIonUriSourceScheduleUpdater(ImmutableList.of("uri"), resolver, writer, deserialiser, itemsPeopleWriter, log).withHttpClient(httpClient).run();
        
    }
    
    @SuppressWarnings("unchecked")
    public void testProcessNewEpisodeWithBrandNoSeries() throws Exception {
        final String newItemNoBrandNoSeriesJson = Resources.toString(Resources.getResource("ion-item-brand-no-series.json"), Charsets.UTF_8);

        context.checking(new Expectations(){{
            one(httpClient).getContentsOf("uri");will(returnValue(newItemNoBrandNoSeriesJson));
            one(resolver).findByCanonicalUri(SLASH_PROGRAMMES_ROOT + "b00y1w9h");will(returnValue(null));
            one(resolver).findByCanonicalUri(SLASH_PROGRAMMES_ROOT + "b006m86d");will(returnValue(null));
            one(writer).createOrUpdate((Item)with(allOf(
                    uri(SLASH_PROGRAMMES_ROOT+"b00y1w9h"),
                    title("28/01/2011"),
                    version(uri(SLASH_PROGRAMMES_ROOT+"b00y1w7k"))
            )));
            one(writer).createOrUpdate((Brand) with(allOf(
                    uri(SLASH_PROGRAMMES_ROOT+"b006m86d")
            )), with(true));
        }});

        new BbcIonUriSourceScheduleUpdater(ImmutableList.of("uri"), resolver, writer, deserialiser, itemsPeopleWriter, log).withHttpClient(httpClient).run();
    }

    @SuppressWarnings("unchecked")
    public void testProcessNewEpisodeWithBrandAndSeries() throws Exception {
        final String newItemNoBrandNoSeriesJson = Resources.toString(Resources.getResource("ion-item-brand-series.json"), Charsets.UTF_8);

        context.checking(new Expectations(){{
            one(httpClient).getContentsOf("uri");will(returnValue(newItemNoBrandNoSeriesJson));
            one(resolver).findByCanonicalUri(SLASH_PROGRAMMES_ROOT + "b00y439c");will(returnValue(null));
            one(resolver).findByCanonicalUri(SLASH_PROGRAMMES_ROOT + "b00xb44r");will(returnValue(null));
            one(resolver).findByCanonicalUri(SLASH_PROGRAMMES_ROOT + "b007gf9k");will(returnValue(null));
            one(writer).createOrUpdate((Item)with(allOf(
                    uri(SLASH_PROGRAMMES_ROOT+"b00y1w9h"),
                    title("Episode 4"),
                    version(uri(SLASH_PROGRAMMES_ROOT+"b00y4336"))
            )));
            one(writer).createOrUpdate((Brand)with(allOf(
                    uri(SLASH_PROGRAMMES_ROOT+"b00xb44r")
            )), with(true));
            one(writer).createOrUpdate((Brand)with(allOf(
                    uri(SLASH_PROGRAMMES_ROOT+"b007gf9k")
            )), with(true));
        }});

        new BbcIonUriSourceScheduleUpdater(ImmutableList.of("uri"), resolver, writer, deserialiser, itemsPeopleWriter, log).withHttpClient(httpClient).run();
    }
    
    private Matcher<Item> version(final Matcher<? super Version> versionMatcher) {
        return new FunctionBasedDescriptionMatcher<Item>("item with version" + versionMatcher, new Function<Item,Boolean>() {
            @Override
            public Boolean apply(Item input) {
                return Iterables.any(input.getVersions(), new Predicate<Version>() {
                    @Override
                    public boolean apply(Version input) {
                        return versionMatcher.matches(input);
                    }
                });
            }
        });
    }
    
    private Matcher<Item> title(final String title) {
        return new FunctionBasedDescriptionMatcher<Item>("item with title " + title, new Function<Item, Boolean>() {
            @Override
            public Boolean apply(Item item) {
                return title.equals(item.getTitle());
            }
        });
    }
    
    private static <T extends Identified> Matcher<T> uri(final String uri) {
        return new FunctionBasedDescriptionMatcher<T>("item with uri " + uri, new Function<T, Boolean>() {
            @Override
            public Boolean apply(T item) {
                return uri.equals(item.getCanonicalUri());
            }
        });
    }
    
    private static class FunctionBasedDescriptionMatcher<T extends Identified> extends TypeSafeMatcher<T> {
        
        private final String desc;
        private final Function<? super T, Boolean> tester;

        public FunctionBasedDescriptionMatcher(String desc, Function<? super T, Boolean> tester) {
            this.desc = desc;
            this.tester = tester;
        }
        
        @Override
        public void describeTo(org.hamcrest.Description desc) {
            desc.appendText(this.desc);
        }

        @Override
        public boolean matchesSafely(T item) {
            return tester.apply(item);
        }
        
    }
}
