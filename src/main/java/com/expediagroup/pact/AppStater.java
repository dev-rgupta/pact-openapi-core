package com.expediagroup.pact;


import com.expediagroup.pact.service.PactGeneratorService;
import lombok.extern.slf4j.Slf4j;

import java.util.Random;

@Slf4j
public class AppStater {

    public static void main(String[] args) throws Exception {
        log.info("pact generation process start!!!");
        /*PactGeneratorService pactGeneratorService = PactGeneratorService.builder()
                .consumerName(System.getProperty("consumerName"))              // by default consumer
                .providerName(System.getProperty("providerName"))              // by default provider
                .pactSpecificationVersion("2.0.0")                             // by default V3
                .pactPath(null)                                                // by default 'target/pacts'
                .resourcePath(System.getProperty("resourcePath"))              // Mandatory field i.e https://petstore3.swagger.io/api/v3/openapi.json,W:/openapi.yaml
                .token(System.getProperty("token"))
                .build();
        pactGeneratorService.run();*/

        PactGeneratorService pactGeneratorService = PactGeneratorService.builder()
                .consumerName("partner_consumer")              // by default consumer "partner_consumer"
                .providerName("partner_provider")              // by default provider "partner_provider"
                .pactSpecificationVersion("2.0.0")                             // by default V3
                .pactPath(null)                                                // by default 'target/pacts'
                .resourcePath("src/main/resources/partner/partner-new.yaml")              // partner-new Mandatory field i.e https://petstore3.swagger.io/api/v3/openapi.json,W:/openapi.yaml
                .token(getToken())
                .build();
        pactGeneratorService.run();

        log.info("pact generation process end!!!");
    }

    private static String getToken(){
        int leftLimit = 97; // letter 'a'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 20;

        return new Random().ints(leftLimit, rightLimit + 1)
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}
