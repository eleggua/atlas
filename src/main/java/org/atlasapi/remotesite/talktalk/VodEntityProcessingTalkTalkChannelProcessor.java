package org.atlasapi.remotesite.talktalk;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import org.atlasapi.media.entity.Content;
import org.atlasapi.remotesite.talktalk.vod.bindings.ChannelType;
import org.atlasapi.remotesite.talktalk.vod.bindings.ItemTypeType;
import org.atlasapi.remotesite.talktalk.vod.bindings.VODEntityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.metabroadcast.common.scheduling.UpdateProgress;

/**
 * TalkTalkChannelProcessor which fetches the VOD entity list for the channel
 * and processes each entity using the given TalkTalkContentEntityProcessor.
 */
public class VodEntityProcessingTalkTalkChannelProcessor implements
        TalkTalkChannelProcessor<UpdateProgress> {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private static final int _500_ITEMS_PER_PAGE = 500;
    
    private final int NO_FAILURES = 0;
    
    private final TalkTalkClient client;
    private final TalkTalkContentEntityProcessor<List<Content>> processor;

    public VodEntityProcessingTalkTalkChannelProcessor(TalkTalkClient client, TalkTalkContentEntityProcessor<List<Content>> processor) {
        this.client = checkNotNull(client);
        this.processor = checkNotNull(processor);
    }
    
    @Override
    public UpdateProgress process(ChannelType channel) throws TalkTalkException {
        return client.processVodList(ItemTypeType.CHANNEL, channel.getId(), new TalkTalkVodEntityProcessor<UpdateProgress>() {
            
            private UpdateProgress progress = UpdateProgress.START;
            
            @Override
            public UpdateProgress getResult() {
                return progress;
            }

            @Override
            public void process(VODEntityType entity) {
                logProcessing(entity);
                switch (entity.getItemType()) {
                case BRAND:
                    checkProgress(processor.processBrandEntity(entity));
                    break;
                case SERIES:
                    checkProgress(processor.processSeriesEntity(entity));
                    break;
                case EPISODE:
                    checkProgress(processor.processEpisodeEntity(entity));
                    break;
                default:
                    log.warn("Not processing unexpected entity type {}", entity.getItemType());
                    break;
                }
            }
        }, _500_ITEMS_PER_PAGE);
    }
    
    private void logProcessing(VODEntityType entity) {
        log.debug("processing {} {}", entity.getItemType(), entity.getId());
    }
    
    private UpdateProgress checkProgress(List<Content> contentList){
        try { 
            checkNotNull(contentList);
            if(contentList.isEmpty()){
                return UpdateProgress.FAILURE;
            }
        } catch (NullPointerException npe) {
            return UpdateProgress.FAILURE;
        }
        return new UpdateProgress(contentList.size(), NO_FAILURES);
    }
}
