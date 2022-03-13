package com.expediagroup.pact.service;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;
import io.swagger.v3.parser.core.models.ParseOptions;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PactGeneratorService {
    private final PactGenerator pactGenerator = new PactGenerator();
    private String consumerName;
    private String providerName;
    private String pactSpecificationVersion;
    private String resourcePath;
    private String pactPath;
    private String token;
    private List<AuthorizationValue> authenticationList;

    public void generatePactFromOpenAPI(PactGeneratorService pactGeneratorService) throws Exception {
        ParseOptions parseOptions = new ParseOptions();
        parseOptions.setResolve(true);
        parseOptions.setResolveFully(true);
        parseOptions.setResolveCombinators(false);
        authenticationList = pactGeneratorService.getAuthenticationList() != null
                ? pactGeneratorService.getAuthenticationList()
                : null;

       String apidocPath =  getDownloadedAPIDocPath(pactGeneratorService);
        OpenAPI openAPI = new OpenAPIV3Parser().read(apidocPath, authenticationList, parseOptions);
        log.info("--- openAPI ---" + openAPI.toString());
        pactGenerator.writePactFiles(openAPI, pactGeneratorService);
    }

    private String getDownloadedAPIDocPath(PactGeneratorService pactGeneratorService) {
        log.info("start Downloading api-doc !!!");
        String apidocPath=null;
        try {
           /* try {
                Path path = Paths.get("src/main/resources/" + pactGeneratorService.providerName + "/");
                Files.createDirectory(path);
                apidocPath = ConnectHttps.downloadFile(new URL(pactGeneratorService.getResourcePath() ),
                        "src/main/resources/"+pactGeneratorService.providerName +"/partner.yml");
            }catch(Exception e){}*/

            log.info("finished Downloading api-doc !!!");
            apidocPath =pactGeneratorService.resourcePath/*"src/main/resources/partner_provider/partner-new.yaml"*/;

        }catch (Exception e){
            e.printStackTrace();
        }
        return apidocPath;
    }

    /**
     * commandLind Args
     * String consumerName,
     * String producerName,
     * String pactFilesDestinationDir
     */
    public void run() throws Exception {
        try {
            validateArgs(this);
            generatePactFromOpenAPI(this);
            log.info(String.format("pact file generated in %s directory ", this.getPactPath()));
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            log.error(String.format("%s is not a file or is an invalid URL", resourcePath));
        } catch (Exception e) {
            e.printStackTrace();
            log.error(":::: Something Went wrong Error Stack Trace ::::" + e);
        }
    }

    private void validateArgs(PactGeneratorService pactGeneratorService) throws Exception {
        if (StringUtils.isEmpty(pactGeneratorService.getResourcePath())) {
            throw new Exception("Argument 'resourcePath' can't be null!");
        }
        if (StringUtils.isEmpty(pactGeneratorService.getConsumerName())) {
            throw new Exception("Argument 'consumerName' can't be null!");
        }
        if (StringUtils.isEmpty(pactGeneratorService.getProviderName())) {
            throw new Exception("Argument 'providerName' can't be null!");
        }
        if (StringUtils.isEmpty(pactGeneratorService.getPactPath())) {
            this.pactPath = "target/pacts";
        }
        if (StringUtils.isEmpty(pactGeneratorService.getPactSpecificationVersion())) {
            this.pactSpecificationVersion = "2.0.0"/*PactSpecVersion.V3.toString()*/;
        }
        log.info("::::::::::::::Arguments List ::::::::::::" + this.toString());
    }

    @Override
    public String toString() {
        return "PactGeneratorService{" +
                "pactGenerator=" + pactGenerator +
                ", consumerName='" + consumerName + '\'' +
                ", providerName='" + providerName + '\'' +
                ", pactSpecificationVersion='" + pactSpecificationVersion + '\'' +
                ", resourcePath='" + resourcePath + '\'' +
                ", pactPath='" + pactPath + '\'' +
                ", token='" + token + '\'' +
                ", authenticationList=" + authenticationList +
                '}';
    }
}
