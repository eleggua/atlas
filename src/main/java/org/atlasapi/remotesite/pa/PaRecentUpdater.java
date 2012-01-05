package org.atlasapi.remotesite.pa;

import java.io.File;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.remotesite.pa.data.PaProgrammeDataStore;
import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.metabroadcast.common.time.DateTimeZones;

public class PaRecentUpdater extends PaBaseProgrammeUpdater implements Runnable {
    
    private final PaProgrammeDataStore fileManager;

    public PaRecentUpdater(PaChannelProcessor channelProcessor, PaProgrammeDataStore fileManager, ChannelResolver channelResolver, AdapterLog log) {
        super(channelProcessor, fileManager, channelResolver, log);
        this.fileManager = fileManager;
    }
    
    @Override
    public void runTask() {
        final Long since = new DateTime(DateTimeZones.UTC).minusDays(2).getMillis();
        this.processFiles(fileManager.localFiles(new Predicate<File>() {
            @Override
            public boolean apply(File input) {
                return input.lastModified() > since;
            }
        }));
    }
}
