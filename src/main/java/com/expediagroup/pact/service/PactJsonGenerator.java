package com.expediagroup.pact.service;

import com.expediagroup.pact.exception.PactGenerationException;
import com.expediagroup.pact.model.Pact;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

@Slf4j
public class PactJsonGenerator {

    private ObjectMapper objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public void writePactFiles(File destinationDir, Collection<Pact> pacts) {
        pacts.forEach(pact -> writePactFile(destinationDir, pact));
    }

    private void writePactFile(File destinationDir, Pact pact) {
        if (destinationDir != null && !destinationDir.exists()) {
            destinationDir.mkdirs();
        }

        final String pactFileName = pact.getConsumer().getName() + "-" + pact.getProvider().getName() + ".json";
        try {
            objectMapper.configure(SerializationFeature.INDENT_OUTPUT,true);
            objectMapper.writeValue(new File(destinationDir, pactFileName), pact);
        } catch (IOException ex) {
            log.error("Unable to write {} to json file", pact);
            throw new PactGenerationException("Unable to write pact to json file", ex);
        }
    }
}
