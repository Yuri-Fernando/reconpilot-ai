package com.reconpilot.api.review;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReviewController {
    private final ReviewService service;

    public ReviewController(ReviewService service) { this.service = service; }

    @PostMapping("/reconciliation-items/{itemId}/reviews")
    public ReviewDtos.Response review(@PathVariable UUID itemId, @Valid @RequestBody ReviewDtos.Request request) {
        return service.review(itemId, request);
    }

    @GetMapping("/reconciliation-items/{itemId}/audit")
    public List<ReviewDtos.EventView> audit(@PathVariable UUID itemId) { return service.audit(itemId); }

    @GetMapping("/audit")
    public List<ReviewDtos.EventView> auditByRun(@RequestParam UUID runId) { return service.auditByRun(runId); }
}
