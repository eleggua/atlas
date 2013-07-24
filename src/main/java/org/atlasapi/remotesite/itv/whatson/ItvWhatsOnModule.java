package org.atlasapi.remotesite.itv.whatson;

import java.util.List;
import javax.annotation.PostConstruct;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ContentWriter;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;
import org.atlasapi.persistence.logging.AdapterLogEntry.Severity;
import org.atlasapi.persistence.system.RemoteSiteClient;
import org.joda.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.metabroadcast.common.http.SimpleHttpClient;
import com.metabroadcast.common.scheduling.RepetitionRules;
import com.metabroadcast.common.scheduling.RepetitionRules.Every;
import com.metabroadcast.common.scheduling.SimpleScheduler;
import com.metabroadcast.common.social.http.HttpClients;

@Configuration
public class ItvWhatsOnModule {
    private @Autowired SimpleScheduler scheduler;
    private @Autowired @Qualifier("contentResolver") ContentResolver contentResolver;
    private @Autowired @Qualifier("contentWriter") ContentWriter contentWriter;
    private @Value("${itv.whatson.schedule.url}") String feedUrl;
    
    private static final Every EVERY_FIFTEEN_MINUTES = RepetitionRules.every(Duration.standardMinutes(15));
    private static final Every EVERY_HOUR = RepetitionRules.every(Duration.standardHours(1));
    
    private @Autowired AdapterLog log;
    
    @PostConstruct
    public void startBackgroundTasks() {
        scheduler.schedule(itvWhatsOnUpdaterDaily(), EVERY_FIFTEEN_MINUTES);
        scheduler.schedule(itvWhatsOnUpdaterPlusMinus7Day(), EVERY_HOUR);
        log.record(new AdapterLogEntry(Severity.INFO).withDescription("ITV What's On Schedule task installed.").withSource(getClass()));
    }
    
    @Bean
    public SimpleHttpClient httpClient() {
        return HttpClients.webserviceClient();
    }
    
    @Bean
    public RemoteSiteClient<List<ItvWhatsOnEntry>> itvWhatsOnClient() {
        return new ItvWhatsOnClient(httpClient());
    }
    
    @Bean
    public ItvWhatsOnEntryProcessor processor() {
        return new ItvWhatsOnEntryProcessor(contentResolver, contentWriter);
    }
    
    @Bean
    public ItvWhatsOnUpdater itvWhatsOnUpdater() {
        return new ItvWhatsOnUpdater(feedUrl, itvWhatsOnClient(), processor(), log);
    }
    
    @Bean
    public ItvWhatsOnUpdaterDaily itvWhatsOnUpdaterDaily() {
        return new ItvWhatsOnUpdaterDaily(itvWhatsOnUpdater());
    }
    
    @Bean
    public ItvWhatsOnUpdaterPlusMinus7Day itvWhatsOnUpdaterPlusMinus7Day() {
        return new ItvWhatsOnUpdaterPlusMinus7Day(itvWhatsOnUpdater());
    }   
}
