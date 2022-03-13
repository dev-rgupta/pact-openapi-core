# pact-openapi-core
This code base can be used in a project as a library to create pact JSON file from an Open API Specification file that contains HTTP request/response interactions by parsing the given Open API specifications.

NOTE: This is a prototype application. We covered only happy cases and are working to add more functionalities like MatchingRules and Interactions with other response codes. For now, some things are hard coded thing but will later be made to be configurable such as generated file name, directory creation path, etc. 

Kindly validate's file format using https://editor.swagger.io/. It will automatically generate Pact file in json format in "target/pacts/".

# Requirements
- Java 1.8 or higher
- Maven

# Step to configure this library

step 1: Add Maven dependency

	<dependency>
            <groupId>com.expediagroup</groupId>
            <artifactId>pact-core</artifactId>
            <version>0.0.9-SNAPSHOT</version>
        </dependency>


step 2: Add maven pact plugin
        https://docs.pact.io/implementation_guides/jvm/provider/maven/  


	<plugin>
                <!--publish newly generated file to pact broker after each successful build-->
                <groupId>au.com.dius.pact.provider</groupId>
                <artifactId>maven</artifactId>
                <version>4.1.11</version>
                <configuration>
                    <reports>
                        <report>console</report>
                        <report>json</report>
                        <report>markdown</report>
                    </reports>
                    <pactBrokerUrl>${pactBrokerUrl}</pactBrokerUrl>                                 <!--http://10.38.98.32:8080/-->
                    <!-- <pactBrokerToken>***************</pactBrokerToken>                         <!-- Replace TOKEN with the actual token -->
                     <pactBrokerAuthenticationScheme>Bearer</pactBrokerAuthenticationScheme>-->

                    <!--validate pact against provider api's-->
                    <serviceProviders>
                        <serviceProvider>
                            <stateChangeUrl>${stateChangeUrl}/v1/pact-state</stateChangeUrl>        <!--the same pact-state-change end point exposed in your Application-->
                            <name>${providerName}</name>
                        </serviceProvider>
                    </serviceProviders>
                    <configuration>
                        <pact.showStacktrace>false</pact.showStacktrace>
                        <pact.verifier.publishResults>true</pact.verifier.publishResults>
                    </configuration>
                </configuration>
            </plugin>


this plugin has the configuration below

    - a: pact broker            ## https://******.pactflow.io/ to pull pact file for verification 
    - b: state change url       ## http://localhost:8080/v1/pact-state
    - c: provider name          ## provider
    - d: to publish the result  ## true

step 3: now come to java part, invoke below code snippet to kick start pact file creation


 	override fun run(vararg args: String?) {
       		 val pactGeneratorService = PactGeneratorService.builder()
          	  			.consumerName("partner_consumer")     			 // by default consumer
           				.providerName("partner_provider")    			 // by default provider
           				.pactSpecificationVersion("2.0.0")   			 // by default V3
            				.authenticationList(null)             			 // by default null
           				.pactPath(null)                     			 // by default 'target/pacts'
           				.resourcePath("src/main/resources/partner.yaml")		 // *Mandatory field* "http://localhost:8080/api-docs" "/Users/ratngupta/github/openapi.yaml"
           				.build()

       		 pactGeneratorService.run()
	
       		 val tryingCoroutine = runBlocking {
         		  		 coroutineScope { 
            		   			 launch {
              		 			  var result =  providerSetUp()                      // providerSetUp method used to create dummy data for the application, you can use DB script but it must have insert statements to insert test entries in underline Database on the basis of requirement. 
               					 }
          				  }
       				      }
    		      }


step 4: Add a endpoint/controller for pact state change url

The pact state change URL is a hook that you have to create on the provider to allow Pact to tell the provider what state it should be in at the start of the test. Before each test runs, the mock consumer taps the state change URL on your provider, and tells it the name of the state the test expects.


	@PostMapping("/v1/pact-state")
    	suspend fun providerState(@RequestBody state: PactState): Any? {
       			 PactStateChangeController.LOGGER.info("Pact State Change >> set ... $state")
        			return state
  		  }


step 5: use below command to upload pact file to pact broker

 	mvn pact:publish -DpactBrokerUrl=${pactBrokerUrl} -DstateChangeUrl=${stateChangeUrl}  -DproviderName=${providerName}

Congratulations!!! Your pact file is published onto the provided pact broker.


step 6: use below command to verify pact file 

Now we have to verify same pact generated from OAS document against provider service implementations.

 	mvn pact:verify -DpactBrokerUrl=${pactBrokerUrl} -DstateChangeUrl=${stateChangeUrl}  -DproviderName=${providerName}

It will verify the pact. This command replays each Interaction from the Pact file against the provider API in real-time, records the response, and then compares both responses (the first one that is already in the pact file and the second response we received from the provider API). It generates the report and will update the result status back to the pact broker.



*Make sure you have to add same ID's in specification file by using Schema's variable named 'example' property i.e.

```xml
parameters:
      - name: partner_id
        in: path
        required: true
        example: 0a1ea7ec-d847-441e-8db6-b60b524f23a7
        schema:
          type: string
```

https://github.expedia.biz/eg-platform-test-automation/pact-openapi-core/tree/master/src/resource/data.js