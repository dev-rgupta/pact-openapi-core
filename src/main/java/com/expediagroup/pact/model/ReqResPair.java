package com.expediagroup.pact.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReqResPair {
        private final InteractionRequest req;
        private final InteractionResponse res;
}
