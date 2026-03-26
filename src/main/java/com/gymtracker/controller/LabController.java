package com.gymtracker.controller;

import com.gymtracker.service.LabIngestService;
import com.gymtracker.service.LabSearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/lab")
@CrossOrigin(origins = "*")
public class LabController {

    private final LabIngestService ingestService;
    private final LabSearchService searchService;

    public LabController(LabIngestService ingestService, LabSearchService searchService) {
        this.ingestService = ingestService;
        this.searchService = searchService;
    }

    @PostMapping("/sync")
    public ResponseEntity<?> sync() {
        try {
            return ResponseEntity.ok(ingestService.syncAll());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(503).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String channelId,
            @RequestParam(required = false, defaultValue = "48") int limit) {
        return ResponseEntity.ok(searchService.search(q, channelId, limit));
    }

    @GetMapping("/channels")
    public ResponseEntity<Map<String, Object>> channels() {
        return ResponseEntity.ok(searchService.channelsMeta());
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "youtubeConfigured", ingestService.youtubeConfigured()
        ));
    }
}
