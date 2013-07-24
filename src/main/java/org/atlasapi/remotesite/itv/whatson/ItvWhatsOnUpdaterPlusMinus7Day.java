package org.atlasapi.remotesite.itv.whatson;
import org.joda.time.LocalDate;

import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.DayRange;
import com.metabroadcast.common.time.DayRangeGenerator;
import com.metabroadcast.common.time.SystemClock;


public class ItvWhatsOnUpdaterPlusMinus7Day extends ScheduledTask {
    private Clock clock;
    private ItvWhatsOnUpdater itvUpdater;
    
    public ItvWhatsOnUpdaterPlusMinus7Day(ItvWhatsOnUpdater itvUpdater) {
        this(new SystemClock(), itvUpdater);
    }
    
    public ItvWhatsOnUpdaterPlusMinus7Day(Clock clock, ItvWhatsOnUpdater itvUpdater) {
       this.clock = clock;
       this.itvUpdater = itvUpdater;
    }

    @Override
    protected void runTask() {
        DayRangeGenerator dateRangeGenerator = new DayRangeGenerator()
                                        .withLookBack(7)
                                        .withLookAhead(7);
        DayRange dayRange = dateRangeGenerator.generate(clock.now().toLocalDate());
        for (LocalDate date : dayRange) {
            itvUpdater.run(date);
        }
    }

}
