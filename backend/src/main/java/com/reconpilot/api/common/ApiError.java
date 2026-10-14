package com.reconpilot.api.common;

import java.time.Instant;

public record ApiError(
        String type,
        String title,
        int status,
        String code,
        String detail,
        String traceId,
        Instant timestamp) {
}
