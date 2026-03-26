package com.gymtracker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.config.LabYoutubeProperties;
import com.gymtracker.entity.LabYoutubeVideo;
import com.gymtracker.repository.LabYoutubeVideoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class LabIngestService {

    private final LabYoutubeProperties properties;
    private final YouTubeApiClient youTubeApiClient;
    private final LabYoutubeVideoRepository videoRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public LabIngestService(
            LabYoutubeProperties properties,
            YouTubeApiClient youTubeApiClient,
            LabYoutubeVideoRepository videoRepository,
            AIService aiService,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.youTubeApiClient = youTubeApiClient;
        this.videoRepository = videoRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    public boolean youtubeConfigured() {
        return properties.getApiKey() != null && !properties.getApiKey().isBlank();
    }

    @Transactional
    public Map<String, Object> syncAll() throws Exception {
        if (!youtubeConfigured()) {
            throw new IllegalStateException("Set lab.youtube.api-key (or YOUTUBE_API_KEY) in configuration.");
        }
        String key = properties.getApiKey().trim();
        int max = Math.max(10, properties.getMaxVideosPerChannel());
        int saved = 0;
        List<String> errors = new ArrayList<>();

        for (String handle : properties.getHandles()) {
            if (handle == null || handle.isBlank()) {
                continue;
            }
            try {
                YouTubeApiClient.ResolvedChannel ch = youTubeApiClient.resolveChannelByHandle(handle.trim(), key);
                List<String> ids = youTubeApiClient.listPlaylistVideoIds(ch.uploadsPlaylistId(), key, max);
                List<Map<String, Object>> details = youTubeApiClient.fetchVideoDetails(ids, key);
                for (Map<String, Object> d : details) {
                    String vid = (String) d.get("videoId");
                    LabYoutubeVideo v = videoRepository.findById(vid).orElseGet(LabYoutubeVideo::new);
                    String newTitle = truncate((String) d.get("title"), 500);
                    String newDesc = truncate((String) d.get("description"), 8000);
                    boolean sameMeta = v.getVideoId() != null
                            && Objects.equals(v.getTitle(), newTitle)
                            && Objects.equals(v.getDescription(), newDesc);
                    String keepEmb = sameMeta ? v.getEmbeddingJson() : null;
                    v.setVideoId(vid);
                    v.setChannelId((String) d.get("channelId"));
                    v.setChannelTitle((String) d.get("channelTitle"));
                    v.setTitle(newTitle);
                    v.setDescription(newDesc);
                    v.setPublishedAt(parseInstant((String) d.get("publishedAt")));
                    String thumb = (String) d.get("thumbnailUrl");
                    if (thumb == null || thumb.isEmpty()) {
                        thumb = "https://i.ytimg.com/vi/" + vid + "/hqdefault.jpg";
                    }
                    v.setThumbnailUrl(truncate(thumb, 1024));
                    v.setEmbeddingJson(keepEmb);
                    videoRepository.save(v);
                    saved++;
                }
            } catch (Exception e) {
                errors.add(handle + ": " + e.getMessage());
            }
        }

        int embedded = 0;
        if (aiService.isAIConfigured()) {
            embedded = embedMissingInternal();
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("videosUpserted", saved);
        out.put("embeddingsUpdated", embedded);
        out.put("totalInDb", videoRepository.count());
        out.put("errors", errors);
        return out;
    }

    private int embedMissingInternal() {
        List<LabYoutubeVideo> missing = videoRepository.findByEmbeddingJsonIsNull();
        if (missing.isEmpty()) {
            return 0;
        }
        int done = 0;
        int batch = 20;
        for (int i = 0; i < missing.size(); i += batch) {
            int end = Math.min(i + batch, missing.size());
            List<LabYoutubeVideo> slice = missing.subList(i, end);
            List<String> texts = slice.stream().map(this::embeddingText).toList();
            List<float[]> vectors = aiService.embedTextsBatch(texts);
            if (vectors.size() != slice.size()) {
                break;
            }
            for (int j = 0; j < slice.size(); j++) {
                try {
                    slice.get(j).setEmbeddingJson(floatArrayToJson(vectors.get(j)));
                    videoRepository.save(slice.get(j));
                    done++;
                } catch (Exception ignored) {
                    // skip row
                }
            }
        }
        return done;
    }

    private String embeddingText(LabYoutubeVideo v) {
        String d = v.getDescription() == null ? "" : v.getDescription();
        if (d.length() > 2000) {
            d = d.substring(0, 2000);
        }
        return (v.getTitle() == null ? "" : v.getTitle()) + "\n" + d;
    }

    private String floatArrayToJson(float[] f) throws Exception {
        return objectMapper.writeValueAsString(f);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max);
    }

    private static Instant parseInstant(String iso) {
        if (iso == null || iso.isEmpty()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(iso);
        } catch (DateTimeParseException e) {
            return Instant.EPOCH;
        }
    }
}
