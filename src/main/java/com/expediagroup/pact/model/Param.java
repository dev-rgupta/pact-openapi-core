package com.expediagroup.pact.model;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class Param {
    private final String name;
    private final String type;
    private final Class<?> genericArgumentType;
    private final Object childNode;
    private final String ref;
}
