package com.reconpilot.api.reconciliation;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reconciliations")
public class ReconciliationController {
    private final ReconciliationService service;

    public ReconciliationController(ReconciliationService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ReconciliationDtos.CreateResponse> create(@Valid @RequestBody ReconciliationDtos.CreateRequest request) {
        ReconciliationDtos.CreateResponse result = service.create(request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result);
    }

    @GetMapping
    public List<ReconciliationDtos.RunListItem> list() { return service.listRuns(); }

    @GetMapping("/{runId}/items")
    public ReconciliationDtos.PageResponse<ReconciliationDtos.ItemView> items(@PathVariable UUID runId,
            @RequestParam(required = false) ReconciliationStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return service.items(runId, status, page, size);
    }

    @GetMapping("/{runId}/summary")
    public ReconciliationDtos.Summary summary(@PathVariable UUID runId) { return service.summary(runId); }
}
