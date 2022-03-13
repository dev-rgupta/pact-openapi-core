package com.expediagroup.pact.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class InteractionResponse {

    private final String status;
    private final String statusCode;
    private final Map<String, String> headers;
    private final JsonNode body;
    private final JsonNode matchingRules;
}
