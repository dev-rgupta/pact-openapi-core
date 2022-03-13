package com.expediagroup.pact.service;

import com.expediagroup.pact.model.Pact;
import com.expediagroup.pact.model.Services;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.swagger.v3.oas.models.OpenAPI;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class PactGenerator {

    private final PactFactory pactFactory;
    private final PactJsonGenerator pactJsonGenerator;

    public PactGenerator() {
        super();
        this.pactFactory = new PactFactory();
        this.pactJsonGenerator = new PactJsonGenerator();
    }

    public void writePactFiles(@NotNull OpenAPI oapi,
                               @NotNull PactGeneratorService pactGeneratorService
                             /*  @NotNull String consumerName,
                               @NotNull String producerName,
                               @NotNull String version,
                               @NotNull File pactFilesDestinationDir*/) {
        this.write(oapi, pactGeneratorService);
    }

    private void write(OpenAPI oapi, PactGeneratorService pactGeneratorService) {
        //Generates Pacts
        Multimap<Services, Pact> providerToPactMap = generatePacts(oapi, pactGeneratorService);
        List<Pact> pacts = providerToPactMap.keySet().stream()
                .map(providerToPactMap::get)
                .map(this::combinePactsToOne)
                .collect(Collectors.toList());

        pactJsonGenerator.writePactFiles(new File(pactGeneratorService.getPactPath()), pacts);
    }

    private Multimap<Services, Pact> generatePacts(OpenAPI oapi, PactGeneratorService pactGeneratorService) {
        Multimap<Services, Pact> providerToPactMap = HashMultimap.create();
        Pact pact = pactFactory.createPacts(oapi, pactGeneratorService);
        providerToPactMap.put(pact.getProvider(), pact);
        return providerToPactMap;
    }

    private Pact combinePactsToOne(Collection<Pact> pacts) {
        if (pacts == null || pacts.isEmpty()) {
            return null;
        }
        Pact referencePact = pacts.iterator().next();

        Pact combinedPact = Pact.builder()
                .metadata(referencePact.getMetadata())
                .consumer(referencePact.getConsumer())
                .provider(referencePact.getProvider())
                .interactions(new ArrayList<>())
                .build();
        pacts.forEach(pact -> combinedPact.getInteractions().addAll(pact.getInteractions()));

        return combinedPact;
    }

}
