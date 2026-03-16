package com.electrahub.charger.api.error;

import java.time.OffsetDateTime;
import java.util.List;

public record ApiError(
        String code,
        String message,
        OffsetDateTime timestamp,
        List<String> details
) {
}
