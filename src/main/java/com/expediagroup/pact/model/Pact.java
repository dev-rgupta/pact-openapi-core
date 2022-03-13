package com.expediagroup.pact.model;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class Pact {

    private final Services provider;
    private final Services consumer;
    private final List<Interaction> interactions;
    private final Metadata metadata;
}
