package com.oriento.api.dto.ai;

import java.util.List;
import java.util.UUID;

public record OrientoAskRequest(UUID conversationId, String prompt, Integer empresaId,
                                List<HistoryMessage> history) {}
