package org.atlasapi.remotesite.pa;

import java.io.File;

import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.remotesite.pa.data.PaProgrammeDataStore;
import org.springframework.stereotype.Controller;

import com.google.common.base.Predicate;

@Controller
public class PaSingleDateUpdater extends PaBaseProgrammeUpdater {
    
    private final String dateString;
    private final PaProgrammeDataStore fileManager;

    public PaSingleDateUpdater(PaChannelProcessor channelProcessor, AdapterLog log, PaProgrammeDataStore fileManager, ChannelResolver channelResolver, String dateString) {
        super(channelProcessor, fileManager, channelResolver, log);
        this.fileManager = fileManager;
        this.dateString = dateString;
    }

    @Override
    public void runTask() {
    	final String filenameContains = dateString + "_tvdata";
        processFiles(fileManager.localFiles(new Predicate<File>() {
            @Override
            public boolean apply(File input) {
                return input.getName().contains(filenameContains);
            }
        }));
    }
}
