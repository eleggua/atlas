package org.atlasapi.remotesite.bbc.ion;

import java.util.List;

import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Network;
import org.atlasapi.media.entity.Policy.Platform;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.media.MimeType;

public enum IonService {

	IPLAYER_INTL_STREAM_MP3 {
		@Override
		public void applyTo(Encoding encoding) {
			encoding.setDataContainerFormat(MimeType.AUDIO_MP3);
			encoding.setAudioCoding(MimeType.AUDIO_MP3);
		}
        
        @Override
        public void applyTo(Policy policy) {
            policy.addAvailableCountry(Countries.ALL);
        }

		@Override
		public List<Policy> policies() {
		    return ImmutableList.copyOf(Iterables.transform(Lists.newArrayList(MediaSet.PC), MEDIASETS_TO_POLICIES));
		}
	},	
	IPLAYER_INTL_STREAM_AAC_WS_CONCRETE {
		@Override
		public void applyTo(Encoding encoding) {
			encoding.setDataContainerFormat(MimeType.AUDIO_AAC);
			encoding.setAudioCoding(MimeType.AUDIO_AAC);
		}
        
        @Override
        public void applyTo(Policy policy) {
            policy.addAvailableCountry(Countries.ALL);
        }

        @Override
        public List<Policy> policies() {
			return ImmutableList.copyOf(Iterables.transform(Lists.newArrayList(MediaSet.PC, MediaSet.APPLE_IPHONE4_HLS, MediaSet.APPLE_PHONE4_IPAD_HLS_3G), MEDIASETS_TO_POLICIES));
		}
	},	
	IPLAYER_STREAMING_H264_FLV_LO {
		
		@Override
		public void applyTo(Encoding encoding) {
			encoding.setDataContainerFormat(MimeType.APPLICATION_XSHOCKWAVEFLASH);
			encoding.setVideoCoding(MimeType.VIDEO_H264);
		}
        
        @Override
        public void applyTo(Policy policy) {
            policy.addAvailableCountry(Countries.GB);
        }

        @Override
        public List<Policy> policies() {
			return ImmutableList.copyOf(Iterables.transform(Lists.newArrayList(MediaSet.PC), MEDIASETS_TO_POLICIES));

		}
	},	
	IPLAYER_STREAMING_H264_FLV_VLO {
        
        @Override
        public void applyTo(Encoding encoding) {
            encoding.setDataContainerFormat(MimeType.APPLICATION_XSHOCKWAVEFLASH);
            encoding.setVideoCoding(MimeType.VIDEO_H264);
        }
        
        @Override
        public void applyTo(Policy policy) {
            policy.addAvailableCountry(Countries.GB);
        }

        @Override
        public List<Policy> policies() {          
            return ImmutableList.copyOf(Iterables.transform(Lists.newArrayList(MediaSet.APPLE_IPHONE4_HLS, MediaSet.APPLE_PHONE4_IPAD_HLS_3G), MEDIASETS_TO_POLICIES));
        }
    },	
	IPLAYER_UK_STREAM_AAC_RTMP_CONCRETE {

		@Override
		public void applyTo(Encoding encoding) {
			encoding.setDataContainerFormat(MimeType.AUDIO_AAC);
			encoding.setAudioCoding(MimeType.AUDIO_AAC);
		}
        
        @Override
        public void applyTo(Policy policy) {
            policy.addAvailableCountry(Countries.GB);
        }

        @Override
        public List<Policy> policies() {
			 return ImmutableList.copyOf(Iterables.transform(Lists.newArrayList(MediaSet.PC), MEDIASETS_TO_POLICIES));
		}
	},
	IPLAYER_INTL_STREAM_AAC_RTMP_CONCRETE {

		@Override
		public void applyTo(Encoding encoding) {
			encoding.setDataContainerFormat(MimeType.AUDIO_AAC);
			encoding.setAudioCoding(MimeType.AUDIO_AAC);
		}
        
        @Override
        public void applyTo(Policy policy) {
            policy.addAvailableCountry(Countries.ALL);
        }

        @Override
        public List<Policy> policies() {
			return ImmutableList.copyOf(Iterables.transform(Lists.newArrayList(MediaSet.PC), MEDIASETS_TO_POLICIES));
		}
	},	
	IPLAYER_STREAMING_H264_FLV {
        @Override
        public void applyTo(Encoding encoding) {
            encoding.setDataContainerFormat(MimeType.APPLICATION_XSHOCKWAVEFLASH);
            encoding.setVideoCoding(MimeType.VIDEO_H264);
            
        }
        
        @Override
        public void applyTo(Policy policy) {
            policy.addAvailableCountry(Countries.GB);
        }

        @Override
        public List<Policy> policies() {
            return ImmutableList.copyOf(Iterables.transform(Lists.newArrayList(MediaSet.PC), MEDIASETS_TO_POLICIES));
        }
    },    
    IPLAYER_STB_UK_STREAM_AAC_CONCRETE {
        @Override
        public void applyTo(Encoding encoding) {
            encoding.setDataContainerFormat(MimeType.AUDIO_AAC);
            encoding.setVideoCoding(MimeType.AUDIO_AAC);
        }
        
        @Override
        public void applyTo(Policy policy) {
            policy.addAvailableCountry(Countries.GB);
        }

        @Override
        public List<Policy> policies() {
            return ImmutableList.copyOf(Iterables.transform(Lists.newArrayList(MediaSet.APPLE_IPHONE4_HLS), MEDIASETS_TO_POLICIES));
        }
    },    
    IPLAYER_UK_STREAM_AAC_RTMP_LO_CONCRETE {
        @Override
        public void applyTo(Encoding encoding) {
            encoding.setDataContainerFormat(MimeType.AUDIO_AAC);
            encoding.setVideoCoding(MimeType.AUDIO_AAC);
        }
        
        @Override
        public void applyTo(Policy policy) {
            policy.addAvailableCountry(Countries.GB);
        }

        @Override
        public List<Policy> policies() {
            return ImmutableList.copyOf(Iterables.transform(Lists.newArrayList(MediaSet.APPLE_PHONE4_IPAD_HLS_3G), MEDIASETS_TO_POLICIES));
        }
    };

	
	protected abstract void applyTo(Encoding encoding);
	
	protected abstract void applyTo(Policy policy);

	protected abstract List<Policy> policies();
	
	private static Function<MediaSet, Policy> MEDIASETS_TO_POLICIES = new Function<MediaSet, Policy>() {
        @Override
        public Policy apply(MediaSet input) {
            switch (input) {
            case PC :
                Policy pc = new Policy();
                pc.setPlatform(Platform.PC);
                return pc;
            case APPLE_IPHONE4_HLS :
                Policy iosWifi = new Policy();
                iosWifi.setPlatform(Platform.IOS);
                iosWifi.setNetwork(Network.WIFI);
                return iosWifi;
            case APPLE_PHONE4_IPAD_HLS_3G :
                Policy ios3G = new Policy();
                ios3G.setPlatform(Platform.IOS);
                ios3G.setNetwork(Network.THREE_G);
                return ios3G;
            default :
                return null;
            }
        }
    };
    
    private enum MediaSet {
        PC,
        APPLE_IPHONE4_HLS,
        APPLE_PHONE4_IPAD_HLS_3G;
    }
	
	public void applyToEncoding(Encoding encoding) {
		applyTo(encoding);
		List<Policy> policies = policies();
		for (Policy policy : policies) {
		    // create matching location for each policy
		    Location location = new Location();
	        location.setPolicy(policy);
	        applyToLocation(location);
	        encoding.addAvailableAt(location);
		}
	}
	
	public List<Location> locations() {
	    List<Policy> policies = policies();
	    List<Location> locations = Lists.newArrayList();
	    for (Policy policy : policies) {
	        Location location = new Location();
	        location.setPolicy(policy);
	        applyToLocation(location);
	        locations.add(location);
	    }
	    return locations;
	}

	private void applyToLocation(Location location) {
		Policy policy = location.getPolicy();
		applyTo(policy);
	}

	public static Maybe<IonService> fromString(String s) {
		for (IonService service : values()) {
			if (service.name().equalsIgnoreCase(s)) {
				return Maybe.just(service);
			}
		}
		return Maybe.nothing();
	}
}
