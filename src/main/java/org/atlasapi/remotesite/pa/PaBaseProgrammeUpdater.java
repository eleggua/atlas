package org.atlasapi.remotesite.pa;

import static org.atlasapi.persistence.logging.AdapterLogEntry.errorEntry;
import static org.atlasapi.persistence.logging.AdapterLogEntry.warnEntry;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PreDestroy;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.remotesite.pa.bindings.ChannelData;
import org.atlasapi.remotesite.pa.bindings.ProgData;
import org.atlasapi.remotesite.pa.data.PaProgrammeDataStore;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.scheduling.ScheduledTask;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.Timestamp;

public abstract class PaBaseProgrammeUpdater extends ScheduledTask {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd-HH:mm").withZone(DateTimeZones.LONDON);
    private static final Pattern FILEDATE = Pattern.compile("^.*(\\d{8})_tvdata.xml$");

    private final PaChannelMap channelMap;
    private final Set<String> currentlyProcessing = Sets.newHashSet();
    
    private List<Channel> supportedChannels = ImmutableList.of();

    private final ExecutorService executor;
    private final PaProgrammeDataStore dataStore;
    private final PaChannelProcessor processor;
    private final AdapterLog log;

    public PaBaseProgrammeUpdater(ExecutorService executor, PaChannelProcessor processor, PaProgrammeDataStore dataStore, ChannelResolver channelResolver, AdapterLog log) {
        this.executor = executor;
        this.processor = processor;
        this.dataStore = dataStore;
        this.channelMap = new PaChannelMap(channelResolver);
        this.log = log;
    }
    
    public PaBaseProgrammeUpdater(PaChannelProcessor processor, PaProgrammeDataStore dataStore, ChannelResolver channelResolver, AdapterLog log) {
        this(Executors.newFixedThreadPool(10, new ThreadFactoryBuilder().setNameFormat("pa-updater-%s").build()), processor, dataStore, channelResolver, log);
    }
    
    public void supportChannels(Iterable<Channel> channels) {
        supportedChannels = ImmutableList.copyOf(channels);
    }
    
    private boolean isSupported(Channel channel) {
        if (supportedChannels.isEmpty() || supportedChannels.contains(channel)) {
            return true;
        }
        return false;
    }
    
    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
    
    protected void processFiles(Iterable<File> files) {
    	
        try {

        	// Files for different days can be processed in parallel. However, files for a given
        	// day must be processed sequentially, the full file followed by its deltas, in order. 
        	// Therefore we produce a list of lists of files,
        	// one per day, and we can process the head of each list in parallel.
        	
        	Set<Queue<File>> groupedFiles = groupAndOrderFilesByDay(files);

        	boolean finished = false;
        	int filesProcessed = 0;
        	while (shouldContinue() && !finished) {
        		Builder<File> batch = ImmutableSet.builder();
        		for(Queue<File> day : groupedFiles) {
        			if(!day.isEmpty()) {
        				batch.add(day.remove());
        			}
        		}
        		Set<File> thisBatch = batch.build();
        		
        		if(!thisBatch.isEmpty()) {
        			reportStatus(String.format("%s/%s files processed. %s files in current batch", filesProcessed, Iterables.size(files), thisBatch.size()));
        			processBatch(thisBatch);
        			filesProcessed += thisBatch.size();
        		}
        		else {
        			finished = true;
        		}
        	}
            
        } catch (Exception e) {
            log.record(errorEntry().withCause(e).withSource(getClass()).withDescription("Exception running PA updater"));
        }
    }

	private void processBatch(Iterable<File> files) throws JAXBException,
			SAXException, ParserConfigurationException, InterruptedException {
		final CompletionService<Integer> completion = new ExecutorCompletionService<Integer>(executor);
		
		JAXBContext context = JAXBContext.newInstance("org.atlasapi.remotesite.pa.bindings");
		Unmarshaller unmarshaller = context.createUnmarshaller();

		SAXParserFactory factory = SAXParserFactory.newInstance();
		factory.setNamespaceAware(true);
		XMLReader reader = factory.newSAXParser().getXMLReader();
		reader.setContentHandler(unmarshaller.getUnmarshallerHandler());
		
		List<Future<Integer>> submitted = Lists.newArrayList();

		for (File file : files) {
		    if(!shouldContinue()) {
		        break;
		    }
		    try {
		        final String filename = file.toURI().toString();
		        Matcher matcher = FILEDATE.matcher(filename);
		        
		        if (matcher.matches()) {
		            final File fileToProcess = dataStore.copyForProcessing(file);
		            final Timestamp lastModified = Timestamp.of(fileToProcess.lastModified());
		            final String fileDate = matcher.group(1);

		            unmarshaller.setListener(channelDataProcessingListener(completion, submitted, lastModified, fileDate, filename));
		            
		            reader.parse(fileToProcess.toURI().toString());
		        }
		    } catch (Exception e) {
		        log.record(errorEntry().withCause(e).withSource(getClass()).withDescription("Error processing file " + file.toString()));
		    }
		}
		
		if (!shouldContinue()) {
		    cancelTasks(submitted);
		    return;
		}
		
		int programmesProcessed = 0;
		int submitCount = submitted.size();
		for (int i = 0; i < submitCount && shouldContinue();) {
		    Future<Integer> processed = completion.poll(5, TimeUnit.SECONDS);
		    if(processed != null) {
		        i++;
		        try {
		            if (!processed.isCancelled()) {
		                programmesProcessed += processed.get();
		            }
		        } catch (Exception e) {
		            log.record(errorEntry().withCause(e).withSource(getClass()).withDescription("Exception processing PA updater results"));
		        }
		        reportStatus(String.format("%s files. Processed %s/%s jobs. %s programmes processed", Iterables.size(files), i, submitCount, programmesProcessed));
		    }
		}
		
		if (!shouldContinue()) {
		    cancelTasks(submitted);
		    return;
		}
	}

    public Set<Queue<File>> groupAndOrderFilesByDay(Iterable<File> files) {
    	
    	java.util.Map<String, LinkedList<File>> filesByDay = new HashMap<String, LinkedList<File> >();
    	
    	for(File file : files) {
    		 Matcher matcher = FILEDATE.matcher(file.toURI().toString());
		        
		    if (matcher.matches()) {
		    	final String fileDate = matcher.group(1);
		    	
		    	if(!filesByDay.containsKey(fileDate)) {
	    			filesByDay.put(fileDate, new LinkedList<File>());
	    		}
		    	filesByDay.get(fileDate).add(file);
		    }
		    else {
		    	log.record(warnEntry().withDescription("Ignoring file " + file.toURI().toString() + " as we were unable to parse for date"));
		    }
    	}
    	
    	// Files are to be processed by full file first, followed by deltas in time
    	// sequence. Full files are named: YYYYMMDD_tvdata.xml, deltas 
    	// yyyymmddhhmm_YYYYMMDD_tvdata.xml, where yyyymmddhhmm is the timestamp of
    	// the delta, and YYYYMMDD is the schedule date.
    	
    	for(List<File> filesForDay : filesByDay.values()) {
    		Collections.sort(filesForDay, new Comparator<File>() {

				public int compare(File o1, File o2) {
					if(o1.getName().length() != o2.getName().length()) {
						return o1.getName().length() < o2.getName().length() ? -1 : 1;
					}
					else {
						return o1.getName().compareTo(o2.getName());
					}
				}
    			
    		});
    	}
    	return new HashSet<Queue<File>>(filesByDay.values());
	}

    private void cancelTasks(List<Future<Integer>> submitted) {
        reportStatus("Cancelling jobs");
        for (Future<Integer> future : submitted) {
            future.cancel(false);
        }
        reportStatus("Jobs cancelled");
    }

    private Listener channelDataProcessingListener(final CompletionService<Integer> completion, final List<Future<Integer>> submitted, final Timestamp lastModified, final String fileDate, final String filename) {
        return new Unmarshaller.Listener() {
            public void beforeUnmarshal(Object target, Object parent) {
            }

            public void afterUnmarshal(Object target, Object parent) {
                if (target instanceof ChannelData) {
                    
                    ChannelData channelData = (ChannelData) target;
                    
                    Maybe<Channel> channel = channelMap.getChannel(Integer.valueOf(channelData.getChannelId()));
                    if (channel.hasValue() && isSupported(channel.requireValue()) && shouldContinue()) {
                        try {
                            final PaChannelData data = new PaChannelData(
                                    channel.requireValue(),
                                    channelData.getProgData(), 
                                    DATE_FORMAT.parseDateTime(fileDate+"-06:00"), 
                                    getTimeZone(fileDate), 
                                    lastModified
                            );
                            Future<Integer> future = completion.submit(new Callable<Integer>() {
                                @Override
                                public Integer call() {
                                    return processor.process(data, currentlyProcessing);
                                }
                            });
                            submitted.add(future);
                        } catch (Throwable e) {
                            log.record(errorEntry().withCause(new Exception(e)).withSource(getClass()).withDescription("Exception submit PA channel update job in file " + filename));
                        }
                    }
                }
            }
        };
    }

    public static class PaChannelData {
        
        private final Channel channel;
        private final Iterable<ProgData> programmes;
        private final DateTime day;
        private final DateTimeZone zone;
        private final Timestamp lastUpdated;

        public PaChannelData(Channel channel, Iterable<ProgData> programmes, DateTime day, DateTimeZone zone, Timestamp lastUpdated) {
            this.channel = channel;
            this.programmes = programmes;
            this.day = day;
            this.zone = zone;
            this.lastUpdated = lastUpdated;
        }

        public Channel channel() {
            return channel;
        }

        public Iterable<ProgData> programmes() {
            return programmes;
        }

        public DateTime day() {
            return day;
        }

        public DateTimeZone zone() {
            return zone;
        }

        public Timestamp lastUpdated() {
            return lastUpdated;
        }
        
    }

    /**
     * The PA do not supply timezone information with times. Instead all times for a file are consistently either GMT or BST,
     * based on the timezone of the listings_date. For example, for listings_date of the Saturday of the switch from BST to 
     * GMT, all times will be GMT+1h, even those after 0200. So 0500 London time on the Sunday is really in GMT, but a 
     * programme starting at 0500 in London will be listed as starting it 0600 (GMT+1)/
     * 
     * @param date
     * @return
     */
    protected static DateTimeZone getTimeZone(String date) {
        String timezoneDateString = date + "-11:00";
        DateTime timezoneDateTime = DATE_FORMAT.parseDateTime(timezoneDateString);
        DateTimeZone zone = timezoneDateTime.getZone();
        return DateTimeZone.forOffsetMillis(zone.getOffset(timezoneDateTime));
    }
}
