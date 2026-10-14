package com.reconpilot.api.review;

import com.reconpilot.api.common.ApiException;
import com.reconpilot.api.reconciliation.ReconciliationItem;
import com.reconpilot.api.reconciliation.ReconciliationItemRepository;
import com.reconpilot.api.reconciliation.ReviewStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private static final String DEMO_ACTOR = "DEMO_ANALYST";
    private final ReconciliationItemRepository itemRepository;
    private final ReviewEventRepository eventRepository;

    public ReviewService(ReconciliationItemRepository itemRepository, ReviewEventRepository eventRepository) {
        this.itemRepository = itemRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public ReviewDtos.Response review(UUID itemId, ReviewDtos.Request request) {
        // Lock de escrita: duas decisoes concorrentes no mesmo item nao podem gerar dois eventos de auditoria.
        ReconciliationItem item = itemRepository.findByIdForUpdate(itemId).orElseThrow(() ->
                new ApiException(HttpStatus.NOT_FOUND, "RECONCILIATION_ITEM_NOT_FOUND", "Item nao encontrado: " + itemId));
        if (item.getReviewStatus() != ReviewStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "REVIEW_NOT_ALLOWED",
                    "O item nao esta pendente ou ja recebeu decisao definitiva");
        }
        ReviewStatus target = request.action() == ReviewAction.APPROVE ? ReviewStatus.APPROVED : ReviewStatus.REJECTED;
        ReviewStatus previous = item.applyReview(target);
        ReviewEvent event = eventRepository.save(new ReviewEvent(item, request.action(), DEMO_ACTOR,
                request.justification().trim(), previous, target));
        return new ReviewDtos.Response(event.getId(), itemId, target, event.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.EventView> audit(UUID itemId) {
        if (!itemRepository.existsById(itemId)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "RECONCILIATION_ITEM_NOT_FOUND", "Item nao encontrado: " + itemId);
        }
        return eventRepository.findByItemIdOrderByCreatedAtAsc(itemId).stream().map(ReviewDtos.EventView::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewDtos.EventView> auditByRun(UUID runId) {
        return eventRepository.findByItemRunIdOrderByCreatedAtDesc(runId).stream().map(ReviewDtos.EventView::from).toList();
    }
}
