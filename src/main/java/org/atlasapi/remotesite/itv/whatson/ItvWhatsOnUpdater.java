package org.atlasapi.remotesite.itv.whatson;

import static org.atlasapi.persistence.logging.AdapterLogEntry.Severity.ERROR;
import java.util.List;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.joda.time.LocalDate;

public class ItvWhatsOnUpdater {
    private final String feedUrl;
    private final RemoteSiteClient<List<ItvWhatsOnEntry>> itvWhatsOnClient;
    private final ItvWhatsOnEntryProcessor processor;
    private final AdapterLog log;
    
    public ItvWhatsOnUpdater(String feedUrl, RemoteSiteClient<List<ItvWhatsOnEntry>> itvWhatsOnClient, ItvWhatsOnEntryProcessor processor, AdapterLog log) {
        this.feedUrl = feedUrl;
        this.itvWhatsOnClient = itvWhatsOnClient;
        this.processor = processor;
        this.log = log;
    }
    
    private String getFeedForDate(LocalDate date) {
        return feedUrl + date.toString("YYYY/MM/dd");
    }
    
    private List<ItvWhatsOnEntry> getFeed(String uri) {
        try {
            return itvWhatsOnClient.get(uri);
        } catch (Exception e) {
            log.record(new AdapterLogEntry(ERROR).withCause(e).withSource(getClass()).withDescription("Exception fetching feed at " + uri));
            return null;
        }
    }

    public void run(LocalDate date) {
        List<ItvWhatsOnEntry> entries = getFeed(getFeedForDate(date));
        for (ItvWhatsOnEntry entry : entries) {
            processor.process(entry);
        }
       
    }

}
