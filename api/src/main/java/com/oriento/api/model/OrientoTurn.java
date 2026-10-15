package com.oriento.api.model;

import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "oriento_turnos")
public record OrientoTurn(@Id String id, String conversationId, long ordem,
                          String pergunta, String resposta, Instant criadoEm) {}
