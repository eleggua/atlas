package org.atlasapi.remotesite.itv.whatson;

import org.joda.time.DateTime;

import com.google.common.base.Objects;


public class ItvWhatsOnEntry {
    private String Channel;
    private DateTime BroadcastDate;
    private ItvWhatsOnEntryDuration Duration;
    private String ProgrammeTitle;
    private String EpisodeTitle;
    private String Synopsis;
    private String ImageUri;
    private String Vodcrid;
    private DateTime AvailabilityStart;
    private DateTime AvailabilityEnd;
    private boolean Repeat;
    private boolean ComingSoon;
    private String ProductionId;
    private String ProgrammeId;
    private String SeriesId;
    private String EpisodeId;
    
    public String getChannel() {
        return Channel;
    }
    
    public DateTime getBroadcastDate() {
        return BroadcastDate;
    }
    
    public ItvWhatsOnEntryDuration getDuration() {
        return Duration;
    }
    
    public String getProgrammeTitle() {
        return ProgrammeTitle;
    }
    
    public String getEpisodeTitle() {
        return EpisodeTitle;
    }
    
    public String getSynopsis() {
        return Synopsis;
    }
    
    public String getImageUri() {
        return ImageUri;
    }
    
    public String getVodcrid() {
        return Vodcrid;
    }
    
    public DateTime getAvailabilityStart() {
        return AvailabilityStart;
    }
    
    public DateTime getAvailabilityEnd() {
        return AvailabilityEnd;
    }
    
    public boolean isRepeat() {
        return Repeat;
    }
    
    public boolean isComingSoon() {
        return ComingSoon;
    }
    
    public String getProductionId() {
        return ProductionId;
    }
    
    public String getProgrammeId() {
        return ProgrammeId;
    }
    
    public String getSeriesId() {
        return SeriesId;
    }
    
    public String getEpisodeId() {
        return EpisodeId;
    }
    
    public void setChannel(String channel) {
        Channel = channel;
    }
    
    public void setBroadcastDate(DateTime broadcastDate) {
        BroadcastDate = broadcastDate;
    }
    
    public void setDuration(ItvWhatsOnEntryDuration duration) {
        Duration = duration;
    }
    
    public void setProgrammeTitle(String programmeTitle) {
        ProgrammeTitle = programmeTitle;
    }
    
    public void setEpisodeTitle(String episodeTitle) {
        EpisodeTitle = episodeTitle;
    }
    
    public void setSynopsis(String synopsis) {
        Synopsis = synopsis;
    }
    
    public void setImageUri(String imageUri) {
        ImageUri = imageUri;
    }
    
    public void setVodcrid(String vodcrid) {
        Vodcrid = vodcrid;
    }
    
    public void setAvailabilityStart(DateTime availabilityStart) {
        AvailabilityStart = availabilityStart;
    }
    
    public void setAvailabilityEnd(DateTime availabilityEnd) {
        AvailabilityEnd = availabilityEnd;
    }
    
    public void setRepeat(boolean repeat) {
        Repeat = repeat;
    }
    
    public void setComingSoon(boolean comingSoon) {
        ComingSoon = comingSoon;
    }
    
    public void setProductionId(String productionId) {
        ProductionId = productionId;
    }
    
    public void setProgrammeId(String programmeId) {
        ProgrammeId = programmeId;
    }
    
    public void setSeriesId(String seriesId) {
        SeriesId = seriesId;
    }
    
    public void setEpisodeId(String episodeId) {
        EpisodeId = episodeId;
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(getClass())
                .add("Channel", getChannel())
                .add("BroadcastDate", getBroadcastDate())
                .add("Duration", getDuration())
                .add("ProgrammeTitle", getProgrammeTitle())
                .add("EpisodeTitle", getEpisodeTitle())
                .add("Synopsis", getSynopsis())
                .add("ImageUri", getImageUri())
                .add("Vodcrid", getVodcrid())
                .add("AvailabilityStart", getAvailabilityStart())
                .add("AvailabilityEnd", getAvailabilityEnd())
                .add("Repeat", isRepeat())
                .add("ComingSoon", isComingSoon())
                .add("ProductionId", getProductionId())
                .add("ProgrammeId", getProgrammeId())
                .add("SeriesId", getSeriesId())
                .add("EpisodeId", getEpisodeId())
                .toString();
    }

}
