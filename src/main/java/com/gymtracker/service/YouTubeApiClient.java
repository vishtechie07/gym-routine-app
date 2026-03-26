package com.gymtracker.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class YouTubeApiClient {

    private static final String BASE = "https://www.googleapis.com/youtube/v3";

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper;

    public YouTubeApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ResolvedChannel(String channelId, String channelTitle, String uploadsPlaylistId) {}

    public ResolvedChannel resolveChannelByHandle(String handle, String apiKey) throws Exception {
        String h = handle.startsWith("@") ? handle.substring(1) : handle;
        String url = UriComponentsBuilder.fromHttpUrl(BASE + "/channels")
                .queryParam("part", "id,snippet,contentDetails")
                .queryParam("forHandle", h)
                .queryParam("key", apiKey)
                .build(true)
                .toUriString();
        String body = restClient.get().uri(url).retrieve().body(String.class);
        JsonNode root = objectMapper.readTree(body);
        JsonNode items = root.get("items");
        if (items == null || !items.isArray() || items.isEmpty()) {
            throw new IllegalStateException("Channel not found for handle: " + handle);
        }
        JsonNode ch = items.get(0);
        String id = ch.get("id").asText();
        String title = ch.path("snippet").path("title").asText("");
        String uploads = ch.path("contentDetails").path("relatedPlaylists").path("uploads").asText(null);
        if (uploads == null || uploads.isEmpty()) {
            throw new IllegalStateException("No uploads playlist for: " + handle);
        }
        return new ResolvedChannel(id, title, uploads);
    }

    public List<String> listPlaylistVideoIds(String playlistId, String apiKey, int maxTotal) throws Exception {
        List<String> ids = new ArrayList<>();
        String pageToken = null;
        while (ids.size() < maxTotal) {
            int pageSize = Math.min(50, maxTotal - ids.size());
            UriComponentsBuilder ub = UriComponentsBuilder.fromHttpUrl(BASE + "/playlistItems")
                    .queryParam("part", "contentDetails")
                    .queryParam("playlistId", playlistId)
                    .queryParam("maxResults", pageSize)
                    .queryParam("key", apiKey);
            if (pageToken != null) {
                ub.queryParam("pageToken", pageToken);
            }
            String url = ub.build(true).toUriString();
            String body = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.get("items");
            if (items == null || !items.isArray()) {
                break;
            }
            for (JsonNode it : items) {
                String vid = it.path("contentDetails").path("videoId").asText(null);
                if (vid != null && !vid.isEmpty()) {
                    ids.add(vid);
                }
            }
            JsonNode next = root.get("nextPageToken");
            if (next == null || next.isNull() || items.isEmpty()) {
                break;
            }
            pageToken = next.asText();
        }
        return ids;
    }

    public List<Map<String, Object>> fetchVideoDetails(List<String> videoIds, String apiKey) throws Exception {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < videoIds.size(); i += 50) {
            int end = Math.min(i + 50, videoIds.size());
            String csv = String.join(",", videoIds.subList(i, end));
            String url = UriComponentsBuilder.fromHttpUrl(BASE + "/videos")
                    .queryParam("part", "snippet")
                    .queryParam("id", csv)
                    .queryParam("key", apiKey)
                    .build(true)
                    .toUriString();
            String body = restClient.get().uri(url).retrieve().body(String.class);
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.get("items");
            if (items == null || !items.isArray()) {
                continue;
            }
            for (JsonNode v : items) {
                String id = v.get("id").asText();
                JsonNode sn = v.path("snippet");
                String title = sn.path("title").asText("");
                String desc = sn.path("description").asText("");
                String channelId = sn.path("channelId").asText("");
                String channelTitle = sn.path("channelTitle").asText("");
                String published = sn.path("publishedAt").asText("");
                String thumb = bestThumbnail(sn.path("thumbnails"));
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("videoId", id);
                m.put("title", title);
                m.put("description", desc);
                m.put("channelId", channelId);
                m.put("channelTitle", channelTitle);
                m.put("publishedAt", published);
                m.put("thumbnailUrl", thumb);
                out.add(m);
            }
        }
        return out;
    }

    private static String bestThumbnail(JsonNode thumbs) {
        if (thumbs == null || thumbs.isMissingNode()) {
            return null;
        }
        for (String k : List.of("maxres", "standard", "high", "medium", "default")) {
            JsonNode t = thumbs.get(k);
            if (t != null && t.has("url")) {
                return t.get("url").asText();
            }
        }
        return null;
    }
}
