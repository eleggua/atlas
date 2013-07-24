package org.atlasapi.remotesite.itv.whatson;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;


public class ItvWhatsOnUpdaterDaily extends ScheduledTask {
    private Clock clock;
    private ItvWhatsOnUpdater itvUpdater;
    
    public ItvWhatsOnUpdaterDaily(ItvWhatsOnUpdater itvUpdater) {
        this(new SystemClock(), itvUpdater);
    }
    
    public ItvWhatsOnUpdaterDaily(Clock clock, ItvWhatsOnUpdater itvUpdater) {
       this.clock = clock;
       this.itvUpdater = itvUpdater;
    }
    
    

    @Override
    protected void runTask() {
        itvUpdater.run(clock.now().toLocalDate());
    }
}
