package com.reconpilot.api.review;

import com.reconpilot.api.reconciliation.ReviewStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ReviewDtos {
    private ReviewDtos() {}

    public record Request(@NotNull ReviewAction action,
            @NotBlank @Size(min = 5, max = 1000) String justification) {}
    public record Response(UUID reviewEventId, UUID itemId, ReviewStatus reviewStatus, Instant createdAt) {}
    public record EventView(UUID id, UUID itemId, ReviewAction action, String actor, String justification,
            ReviewStatus previousStatus, ReviewStatus newStatus, Instant createdAt) {
        static EventView from(ReviewEvent event) {
            return new EventView(event.getId(), event.getItem().getId(), event.getAction(), event.getActor(),
                    event.getJustification(), event.getPreviousStatus(), event.getNewStatus(), event.getCreatedAt());
        }
    }
}
