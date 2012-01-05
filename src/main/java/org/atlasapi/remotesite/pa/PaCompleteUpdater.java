package org.atlasapi.remotesite.pa;

import java.io.File;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.remotesite.pa.data.PaProgrammeDataStore;

import com.google.common.base.Predicates;

public class PaCompleteUpdater extends PaBaseProgrammeUpdater implements Runnable {
    
    private final PaProgrammeDataStore fileManager;

    public PaCompleteUpdater(PaChannelProcessor processor, PaProgrammeDataStore fileManager, ChannelResolver channelResolver, AdapterLog log) {
        super(processor, fileManager, channelResolver, log);
        this.fileManager = fileManager;
    }
    
    @Override
    public void runTask() {
        this.processFiles(fileManager.localFiles(Predicates.<File>alwaysTrue()));
    }
    
}
