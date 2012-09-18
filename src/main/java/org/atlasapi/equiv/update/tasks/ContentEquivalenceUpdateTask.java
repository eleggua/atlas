package org.atlasapi.equiv.update.tasks;

import static org.atlasapi.persistence.content.ContentCategory.CONTAINER;
import static org.atlasapi.persistence.content.ContentCategory.TOP_LEVEL_ITEM;
import static org.atlasapi.persistence.content.listing.ContentListingCriteria.defaultCriteria;
import static org.atlasapi.persistence.content.listing.ContentListingProgress.progressFrom;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;

import org.atlasapi.equiv.update.EquivalenceUpdater;
import org.atlasapi.equiv.update.RootEquivalenceUpdater;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.listing.ContentListingCriteria;
import org.atlasapi.persistence.content.listing.ContentListingProgress;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public final class ContentEquivalenceUpdateTask extends AbstractContentListingTask<Content> {

    private final ScheduleTaskProgressStore progressStore;
    private final EquivalenceUpdater<Content> updater;    
    private final Set<String> ignored;

    private String schedulingKey = "equivalence";
    private List<Publisher> publishers;
    private int processed = 0;
    
    public ContentEquivalenceUpdateTask(ContentLister contentLister, ContentResolver contentResolver, ScheduleTaskProgressStore progressStore, EquivalenceUpdater<Content> updater, Set<String> ignored) {
        super(contentLister);
        this.progressStore = progressStore;
        this.updater = new RootEquivalenceUpdater(contentResolver, updater);
        this.ignored = ignored;
    }

    public ContentEquivalenceUpdateTask forPublishers(Publisher... publishers) {
        this.publishers = ImmutableList.copyOf(publishers);
        this.schedulingKey = Joiner.on("-").join(Iterables.transform(this.publishers, Publisher.TO_KEY))+"-equivalence";
        return this;
    }

    @Override
    protected ContentListingProgress getProgress() {
        return progressStore.progressForTask(schedulingKey);
    }

    @Override
    protected Iterator<Content> filter(Iterator<Content> rawIterator) {
        return rawIterator;
    }

    @Override
    protected ContentListingCriteria listingCriteria(ContentListingProgress progress) {
        return defaultCriteria().forPublishers(publishers).forContent(CONTAINER, TOP_LEVEL_ITEM).startingAt(progress).build();
    }
    
    @Override
    protected void onStart(ContentListingProgress progress) {
        log.info("Started: {} from {}", schedulingKey, describe(progress));
        processed  = 0;
    }

    private String describe(ContentListingProgress progress) {
        if (progress == null || ContentListingProgress.START.equals(progress)) {
            return "start";
        }
        return String.format("%s %s %s", progress.getCategory(), progress.getPublisher(), progress.getUri());
    }
    
    @Override
    protected boolean handle(Content content) {
        if (!ignored.contains(content.getCanonicalUri())) {
            try {
                reportStatus(String.format("Processed %d. Processing %s.", processed, content));
                updater.updateEquivalences(content);
                if (++processed % 10 == 0) {
                    updateProgress(progressFrom(content));
                }
            } catch (Exception e) {
                log.error(content.toString(), e);
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onFinish(boolean finished, @Nullable Content lastProcessed) {
        persistProgress(finished, lastProcessed);
    }

    private void persistProgress(boolean finished, Content content) {
        if (finished) {
            updateProgress(ContentListingProgress.START);
            log.info("Finished: {}", schedulingKey);
        } else {
            if (content != null) {
                updateProgress(progressFrom(content));
                log.info("Stopped: %s at %s", schedulingKey, content);
            }
        }
    }

    private void updateProgress(ContentListingProgress progress) {
        progressStore.storeProgress(schedulingKey, progress);
    }

}