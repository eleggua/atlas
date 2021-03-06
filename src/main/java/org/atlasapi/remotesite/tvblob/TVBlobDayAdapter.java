package org.atlasapi.remotesite.tvblob;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;

public class TVBlobDayAdapter {

    private static final Pattern URL_PATTERN = Pattern
                    .compile("http://epgadmin.tvblob.com/api/(\\w+)/programmes/schedules/(.+)(\\.\\w*)?");
    private final ContentWriter contentStore;
    private final ContentResolver contentResolver;
    
    static final Log LOG = LogFactory.getLog(TVBlobDayAdapter.class);
    
    public TVBlobDayAdapter(ContentWriter contentStore, ContentResolver contentResolver) {
        this.contentStore = contentStore;
        this.contentResolver = contentResolver;
    }

    public boolean canPopulate(String uri) {
        return URL_PATTERN.matcher(uri).matches();
    }

    public void populate(String uri) {
        Matcher matcher = URL_PATTERN.matcher(uri);
        String channelSlug = null;
        if (matcher.matches()) {
            channelSlug = matcher.group(1);
        }

        InputStream is = null;
        TVBlobDayPopulator populator = new TVBlobDayPopulator(contentStore, contentResolver, channelSlug);
        populator.setExpireAfterDays(30);
        try {
            if (LOG.isInfoEnabled()) {
                LOG.info("Retrieving "+uri);
            }
            
            is = new URL(uri).openStream();
            populator.populate(is);
        } catch (Exception e) {
            LOG.warn("Unable to retrieve source "+uri, e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {}
            }
        }
    }
}
