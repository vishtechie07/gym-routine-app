package com.gymtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymtracker.entity.LabYoutubeVideo;
import com.gymtracker.repository.LabYoutubeVideoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabSearchService {

    private static final int MAX_POOL = 2000;
    private static final int DEFAULT_LIMIT = 48;

    private final LabYoutubeVideoRepository videoRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper;

    public LabSearchService(
            LabYoutubeVideoRepository videoRepository,
            AIService aiService,
            ObjectMapper objectMapper) {
        this.videoRepository = videoRepository;
        this.aiService = aiService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> search(String query, String channelId, int limit) {
        int lim = limit <= 0 ? DEFAULT_LIMIT : Math.min(limit, 100);
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        boolean filterChannel = channelId != null && !channelId.isBlank();

        List<LabYoutubeVideo> pool;
        if (filterChannel) {
            pool = videoRepository.findByChannelIdOrderByPublishedAtDesc(
                    channelId.trim(), PageRequest.of(0, MAX_POOL));
        } else {
            pool = videoRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(0, MAX_POOL));
        }

        if (pool.isEmpty()) {
            return Map.of(
                    "videos", List.of(),
                    "smart", false,
                    "count", 0
            );
        }

        if (q.isEmpty()) {
            List<Map<String, Object>> recent = pool.stream()
                    .limit(lim)
                    .map(v -> toDto(v, 0.0, false))
                    .collect(Collectors.toList());
            return Map.of("videos", recent, "smart", false, "count", recent.size());
        }

        float[] queryVec = aiService.isAIConfigured() ? aiService.embedText(q) : null;
        boolean anyEmb = pool.stream().anyMatch(v -> v.getEmbeddingJson() != null && !v.getEmbeddingJson().isEmpty());

        List<Scored> scored = new ArrayList<>();
        for (LabYoutubeVideo v : pool) {
            double kw = keywordScore(q, v);
            double sem = 0.0;
            if (queryVec != null && anyEmb && v.getEmbeddingJson() != null) {
                try {
                    float[] doc = parseEmbedding(v.getEmbeddingJson());
                    if (doc != null && doc.length == queryVec.length) {
                        sem = (cosine(queryVec, doc) + 1.0) / 2.0;
                    }
                } catch (Exception ignored) {
                    // fall back to keyword
                }
            }
            double combined;
            if (queryVec != null && anyEmb && sem > 0) {
                combined = 0.62 * sem + 0.38 * kw;
            } else {
                combined = kw;
            }
            scored.add(new Scored(v, combined));
        }

        scored.sort(Comparator.comparingDouble((Scored s) -> s.score).reversed());
        boolean smart = queryVec != null && anyEmb;

        List<Map<String, Object>> out = scored.stream()
                .limit(lim)
                .map(s -> toDto(s.video, s.score, smart))
                .collect(Collectors.toList());

        return Map.of("videos", out, "smart", smart, "count", out.size());
    }

    private static double keywordScore(String q, LabYoutubeVideo v) {
        String hay = ((v.getTitle() == null ? "" : v.getTitle()) + " "
                + (v.getDescription() == null ? "" : v.getDescription())).toLowerCase(Locale.ROOT);
        if (hay.isEmpty()) {
            return 0.0;
        }
        if (hay.contains(q)) {
            return 1.0;
        }
        String[] parts = q.split("\\s+");
        if (parts.length == 0) {
            return 0.0;
        }
        int hit = 0;
        for (String p : parts) {
            if (p.length() >= 2 && hay.contains(p)) {
                hit++;
            }
        }
        return (double) hit / parts.length;
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private float[] parseEmbedding(String json) throws Exception {
        JsonNode n = objectMapper.readTree(json);
        if (!n.isArray()) {
            return null;
        }
        float[] f = new float[n.size()];
        for (int i = 0; i < n.size(); i++) {
            f[i] = (float) n.get(i).asDouble();
        }
        return f;
    }

    private static Map<String, Object> toDto(LabYoutubeVideo v, double score, boolean smart) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("videoId", v.getVideoId());
        m.put("title", v.getTitle());
        m.put("channelTitle", v.getChannelTitle());
        m.put("channelId", v.getChannelId());
        m.put("thumbUrl", v.getThumbnailUrl() != null && !v.getThumbnailUrl().isEmpty()
                ? v.getThumbnailUrl()
                : "https://i.ytimg.com/vi/" + v.getVideoId() + "/hqdefault.jpg");
        m.put("publishedAt", v.getPublishedAt() != null ? v.getPublishedAt().toString() : "");
        String snip = v.getDescription() == null ? "" : v.getDescription();
        if (snip.length() > 220) {
            snip = snip.substring(0, 217) + "…";
        }
        m.put("snippet", snip);
        m.put("score", Math.round(score * 1000.0) / 1000.0);
        m.put("smart", smart);
        return m;
    }

    public Map<String, Object> channelsMeta() {
        List<LabYoutubeVideo> all = videoRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(0, MAX_POOL));
        Map<String, String> idToTitle = new LinkedHashMap<>();
        for (LabYoutubeVideo v : all) {
            idToTitle.putIfAbsent(v.getChannelId(), v.getChannelTitle());
        }
        List<Map<String, String>> rows = new ArrayList<>();
        for (Map.Entry<String, String> e : idToTitle.entrySet()) {
            rows.add(Map.of("channelId", e.getKey(), "channelTitle", e.getValue()));
        }
        rows.sort(Comparator.comparing(m -> m.get("channelTitle")));
        return Map.of(
                "channels", rows,
                "totalVideos", videoRepository.count(),
                "missingEmbeddings", videoRepository.countByEmbeddingJsonIsNull()
        );
    }

    private record Scored(LabYoutubeVideo video, double score) {}
}
