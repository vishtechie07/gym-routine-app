package com.gymtracker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "lab.youtube")
public class LabYoutubeProperties {

    /** YouTube Data API v3 key */
    private String apiKey = "";

    /**
     * Channel handles for {@code channels.list?forHandle=} (no @ prefix).
     * Defaults: HASfit, Jeff Nippard, Renaissance Periodization, BEST EXERCISES.
     */
    private List<String> handles = new ArrayList<>(List.of(
            "HASfit",
            "jeffnippard",
            "RenaissancePeriodization",
            "BESTEXERCISESCHANNEL"
    ));

    private int maxVideosPerChannel = 100;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public List<String> getHandles() {
        return handles;
    }

    public void setHandles(List<String> handles) {
        this.handles = handles;
    }

    public int getMaxVideosPerChannel() {
        return maxVideosPerChannel;
    }

    public void setMaxVideosPerChannel(int maxVideosPerChannel) {
        this.maxVideosPerChannel = maxVideosPerChannel;
    }
}
