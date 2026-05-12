package com.finai.dto.alert;

import java.util.List;

/** Wrapper per {@code GET /api/alerts}: separa alert attivi da history. */
public record AlertsResponse(
        List<AlertDto> active,
        List<AlertDto> history
) {}
