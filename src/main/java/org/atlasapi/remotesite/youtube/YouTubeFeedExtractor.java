package org.atlasapi.remotesite.youtube;

import org.atlasapi.media.entity.ContentGroup;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.remotesite.ContentExtractor;
import org.atlasapi.remotesite.youtube.YouTubeFeedClient.VideoEntry;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

public class YouTubeFeedExtractor implements ContentExtractor<YouTubeFeedSource, ContentGroup> {

    private final ContentExtractor<YouTubeSource, Item> itemExtractor;
    
    public YouTubeFeedExtractor() {
        this(new YouTubeGraphExtractor());
    }

    public YouTubeFeedExtractor(ContentExtractor<YouTubeSource, Item> itemExtractor) {
        this.itemExtractor = itemExtractor;
    }

    @Override
    public ContentGroup extract(YouTubeFeedSource source) {
        YouTubeFeedClient.VideoFeed feed = source.getVideoFeed();

        ContentGroup playlist = new ContentGroup(source.getUri(), YouTubeFeedCanonicaliser.curieFor(source.getUri()), Publisher.YOUTUBE);
        playlist.setMediaType(MediaType.VIDEO);
        
        Iterable<Item> items = Iterables.transform(feed.videos, new Function<VideoEntry, Item>() {

			@Override
			public Item apply(VideoEntry video) {
				return  itemExtractor.extract(new YouTubeSource(video, new YoutubeUriCanonicaliser().canonicalUriFor(video.id)));
			}
        });
        
        playlist.setContents(items);
        return playlist;
    }
}
