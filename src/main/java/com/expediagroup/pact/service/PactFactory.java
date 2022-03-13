package com.expediagroup.pact.service;

import com.expediagroup.pact.exception.PactGenerationException;
import com.expediagroup.pact.exception.PropertiesNotFountException;
import com.expediagroup.pact.model.*;
import com.expediagroup.pact.podam.BigDecimalManufacturer;
import com.expediagroup.pact.podam.BigIntegerManufacturer;
import com.expediagroup.pact.podam.EnumStringManufacturer;
import com.expediagroup.pact.utilities.HttpStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Slf4j
public class PactFactory {

    private static final PodamFactory podamFactory;

    private static final boolean isOnlyHappyCase = false;
    private  boolean auth=true;

    static {
        podamFactory = new PodamFactoryImpl();
        podamFactory.getStrategy().addOrReplaceTypeManufacturer(String.class, new EnumStringManufacturer());
        podamFactory.getStrategy().addOrReplaceTypeManufacturer(BigInteger.class, new BigIntegerManufacturer());
        podamFactory.getStrategy().addOrReplaceTypeManufacturer(BigDecimal.class, new BigDecimalManufacturer());
        podamFactory.getStrategy().setDefaultNumberOfCollectionElements(1);
    }

    private  String requestQueryParamGenerator(Operation operation) {
        List<Parameter> parameterList = operation.getParameters();
        if (parameterList == null) {
            return null;
        }
        StringBuilder queryBuilder = new StringBuilder();
        for (Parameter parameter : parameterList) {
            if ("query".equalsIgnoreCase(parameter.getIn())) {
                String queryValue=null;
                Schema schema = parameter.getSchema();
                if(schema !=null && schema instanceof ArraySchema ) {
                    if (((ArraySchema) schema).getItems().getExample() != null){
                        queryValue = ((ArraySchema) schema).getItems().getExample() != null ? ((ArraySchema) schema).getItems().getExample().toString() : String.valueOf(manufacturePojo(getClassType(((ArraySchema) schema).getItems().getType())));
                     }else if(((ArraySchema) schema).getItems().getEnum() !=null){
                        queryValue = ((ArraySchema) schema).getItems().getEnum().get(0) != null ? ((ArraySchema) schema).getItems().getEnum().get(0).toString() : String.valueOf(manufacturePojo(getClassType(((ArraySchema) schema).getItems().getType())));
                    }
                }else if(schema !=null && schema.getExample()!=null){
                    queryValue = schema.getExample()!=null? schema.getExample().toString():String.valueOf(manufacturePojo(getClassType(schema.getType())));
                }else {
                    queryValue = parameter.getExample()!= null ? parameter.getExample().toString() : String.valueOf(manufacturePojo(getClassType(parameter.getSchema().getType())));
                }
                queryBuilder.append(parameter.getName()).append("=").append(queryValue).append("&");
            }
        }
        if (queryBuilder.length() != 0) {
            queryBuilder.deleteCharAt(queryBuilder.length() - 1);
        }

        return queryBuilder.toString();
    }

    private  Map<String, String> requestHeaderParamGenerator(Operation operation,String token) {
        List<Parameter> parameterList = operation.getParameters();
        Param param = null;
        List<Param> headerList = new LinkedList<>();
        if (parameterList != null) {
          for (Parameter parameter : parameterList) {
            if ("header".equalsIgnoreCase(parameter.getIn())) {
                param = Param.builder().name(parameter.getName()).type(parameter.getSchema().getType())
                        .childNode(manufacturePojo(getClassType(parameter.getSchema().getType())))
                        .ref(parameter.getSchema().get$ref()).build();
                headerList.add(param);
            }
          }
        }
        if(auth){
            param = Param.builder().name("Authorization").type("string")
                    .childNode("Bearer "+token)
                    .build();
            headerList.add(param);
        }

        return mapHeaders(headerList);
    }

    private  Map<String, String> responseHeaderParamGenerator(Map<String, Header> headerMap) {
        if (Objects.nonNull(headerMap)) {
            Iterator<Entry<String, Header>> headerEntry = headerMap.entrySet().iterator();
            List<Param> headerList = new LinkedList<>();
            Param param = null;
            while (headerEntry.hasNext()) {
                Entry<String, Header> entry = headerEntry.next();
                param = Param.builder().name(entry.getKey()).type(entry.getValue().getSchema().getType())
                        .childNode(entry.getValue().getExample())
                        .ref(entry.getValue().getSchema().get$ref()).build();
                headerList.add(param);
            }
            return mapHeaders(headerList);
        }
        return null;
    }

    private  String requestPathParamGenerator(Operation operation, String path) {
        List<Parameter> parameterList = operation.getParameters();
        if (parameterList != null) {
            for (Parameter parameter : parameterList) {
                if ("path".equalsIgnoreCase(parameter.getIn())) {
                    if(parameter.getExamples()!=null){
                        Map<String, Example> exampleMap = parameter.getExamples();
                        for (Map.Entry<String, Example> entry : exampleMap.entrySet()) {
                            System.out.println("Key = " + entry.getKey() + ", Value = " + entry.getValue());
                            Example example =  entry.getValue();
                            String pathValue = example.getValue() != null ? example.getValue().toString() : String.valueOf(manufacturePojo(getClassType(parameter.getSchema().getType())));
                            path = path.replace("{" + entry.getKey() + "}", pathValue);
                        }
                    } else{
                        String pathValue = parameter.getExample() != null ? parameter.getExample().toString() : String.valueOf(manufacturePojo(getClassType(parameter.getSchema().getType())));
                        path = path.replace("{" + parameter.getName() + "}", pathValue);
                    }
                }
            }
        }
        return path;
    }

    @SuppressWarnings("unchecked")
    private  JsonNode requestBodyGenerator(Operation operation, ObjectMapper objectMapper) {
        List<Param> resultList = new LinkedList<>();
        try {
            RequestBody requestBody = operation.getRequestBody();
            if (Objects.isNull(requestBody)) {
                return null;
            }
            for (Entry<String, MediaType> mapMediaType : requestBody.getContent().entrySet()) {
                Schema schema = mapMediaType.getValue().getSchema();
                if (Objects.isNull(schema.getProperties())) {
                    log.error(":::: No properties Found in schema :::: " + schema.getName());
                } else {
                    Iterator<Entry<String, Schema>> itr = schema.getProperties().entrySet().iterator();
                    while (itr.hasNext()) {
                        resultList = extractedParamList(resultList, itr);
                    }
                }
            }
            String jsonString = objectMapper.writeValueAsString(parseParametersToBody(resultList));
            //	log.info("::::::::Request Body String :::"+jsonString);
            //	log.info("::::::::Request Body JSON :::"+jsonNode);
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return objectMapper.nullNode();
    }

    private  List<Param> getParams(List<Param> resultList, ArraySchema arrayModel) {
        Schema<?> schema1 = arrayModel.getItems();
        if(schema1 instanceof ObjectSchema) {
            if (schema1.getProperties() != null) {
                Iterator<Entry<String, Schema>> arrayProp = schema1.getProperties().entrySet().iterator();
                while (arrayProp.hasNext()) {
                    resultList = extractedParamList(resultList, arrayProp);
                }
            } else {
                throw new PropertiesNotFountException();
            }
        }else if(schema1 instanceof ComposedSchema){
            ComposedSchema composedModel = (ComposedSchema) schema1;
            List<Schema> schema = composedModel.getAllOf();
            if(schema==null){
                schema = composedModel.getAnyOf();
            }
            if(schema==null){
                schema = composedModel.getOneOf();
            }
            if(Objects.nonNull(schema)) {
                Schema<?> schema11 = schema.get(0);
                Iterator<Map.Entry<String, Schema>> itr = schema11.getProperties().entrySet().iterator();
                while (itr.hasNext()) {
                    resultList = extractedParamList(resultList, itr);
                }
            }
        }
        return resultList;
    }

    private  List<Param> extractedElementFromArraySchema(Entry<String, Schema> entry) throws PropertiesNotFountException {
        List<Param> resultList = new ArrayList<>();
        ArraySchema arrayModel = (ArraySchema) entry.getValue();
        resultList = getParams(resultList, arrayModel);
        return resultList;
    }

    private  List<Param> extractedElementFromComposedSchema(Entry<String, Schema> entry1) {
        List<Param> resultList = new LinkedList<>();
        ComposedSchema composedModel = (ComposedSchema) entry1.getValue();
        List<Schema> schema = composedModel.getAllOf();
        if(schema==null){
            schema = composedModel.getAnyOf();
        }
        if(schema==null){
            schema = composedModel.getOneOf();
        }
        if(Objects.nonNull(schema)) {
            Schema<?> schema1 = schema.get(0);
            Iterator<Map.Entry<String, Schema>> itr = schema1.getProperties().entrySet().iterator();
            while (itr.hasNext()) {
                resultList = extractedParamList(resultList, itr);
            }
        }
        return resultList;
    }

    @SuppressWarnings("unchecked")
    private  List<Param> extractedElementFromObjectSchema(Entry<String, Schema> entry) {
        List<Param> resultList = new LinkedList<>();
        Param param = null;
        if( entry.getValue() instanceof ArraySchema){
            Iterator<Map.Entry<String, Schema>> itr=  ((ArraySchema) entry.getValue()).getItems().getProperties().entrySet().iterator();
            while (itr.hasNext()) {
              //  Entry<String, Schema> entry1 = itr.next();
                log.info("::::::::::::::::in extractedElementFromObjectSchema-if:::::::::key:::::"+entry.getKey());
                /*param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
                        .childNode(entry1.getValue().getExample()).ref(entry1.getValue().get$ref()).build();
                resultList.add(param);*/
                resultList = extractedParamList(resultList, itr);
            }
        }else if( entry.getValue() instanceof ObjectSchema && entry.getValue().getProperties()==null){

            param = Param.builder().name(entry.getKey()).type(entry.getValue().getType())
                    .childNode(entry.getValue().getExample()).ref(entry.getValue().get$ref()).build();
            resultList.add(param);
        }else{
            log.info("::::::::::::::::in extractedElementFromObjectSchema-else:::::::::key:::::"+entry.getKey());
            Iterator<Map.Entry<String, Schema>> itr = entry.getValue().getProperties().entrySet().iterator();
            while (itr.hasNext()) {
                resultList = extractedParamList(resultList, itr);
            }
        }
        return resultList;
    }

    private  Object extractedElementFromMapSchema(Entry<String, Schema> entry1) {
        List<Param> resultList = new LinkedList<>();
        Param param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
                .childNode(entry1.getValue().getExample()).ref(entry1.getValue().get$ref()).build();
        resultList.add(param);
        return resultList;
    }
    private  List<Param> extractedParamList(List<Param> resultList, Iterator<Map.Entry<String, Schema>> entry) {
        Entry<String, Schema> entry1 = entry.next();
        Param param = null;
        if (entry1.getValue() instanceof ArraySchema) {
             log.info("::::::::::::::::in ArraySchema::::::::::::::");
            if(((ArraySchema) entry1.getValue()).getItems()  instanceof StringSchema) {
                log.info("::::::::extractedParamList-ArraySchema  StringSchema:::"+entry1.toString());
                if (((ArraySchema) entry1.getValue()).getItems().getProperties() == null) {
                    param = Param.builder().name(entry1.getKey()).type(((ArraySchema) entry1.getValue()).getType())
                            .childNode(((ArraySchema) entry1.getValue()).getItems().getExample()).ref(((ArraySchema) entry1.getValue()).getItems().get$ref()).build();
                    resultList.add(param);
                } else {
                    throw new PropertiesNotFountException();
                }
            }else if(((ArraySchema) entry1.getValue()).getItems()  instanceof ObjectSchema) {
                log.info("::::::::extractedParamList-ArraySchema  ObjectSchema:::"+entry1.toString());
                if (((ArraySchema) entry1.getValue()).getItems().getProperties() != null) {
                    param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
                            .childNode(extractedElementFromObjectSchema(entry1)).ref(entry1.getValue().get$ref()).build();
                    resultList.add(param);
                } else {
                    throw new PropertiesNotFountException();
                }
            }else {
                try {
                    log.info("::::::::extractedParamList-default:::"+entry1.toString());
                    param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
                            .childNode(extractedElementFromArraySchema(entry1)).ref(entry1.getValue().get$ref()).build();
                } catch (PropertiesNotFountException pnfe) {
                     log.info("::::::No properties in ArraySchema:::::::"+entry1.getKey());
                    param = Param.builder().name(entry1.getKey()).type(((ArraySchema) entry1.getValue()).getItems().getType())
                            .childNode(new ArrayList<>())
                            .genericArgumentType(Array.class)
                            .ref(entry1.getValue().get$ref()).build();
                }
                resultList.add(param);
            }
        } else if (entry1.getValue() instanceof MapSchema && entry1.getValue().getAdditionalProperties() !=null) {
             log.info("::::::::::::::::in MapSchema::::::::::::::");
            param = Param.builder().name(entry1.getKey()).type("String")
                    .childNode(entry1.getValue().getExample())
                    .ref(entry1.getValue().get$ref()).build();
            resultList.add(param);
        } else if (entry1.getValue() instanceof ObjectSchema) {
             log.info("::::::::::::::::in ObjectSchema::::::::::::::");
            param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
                    .childNode(extractedElementFromObjectSchema(entry1)).ref(entry1.getValue().get$ref()).build();
            resultList.add(param);
        } else if (entry1.getValue() instanceof ComposedSchema) {
             log.info("::::::::::::::::in ComposedSchema::::::::::::::");
            param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
                    .childNode(extractedElementFromComposedSchema(entry1)).ref(entry1.getValue().get$ref()).build();
            resultList.add(param);
        } else {
            param = Param.builder().name(entry1.getKey()).type(entry1.getValue().getType())
                    .childNode(entry1.getValue().getExample()).ref(entry1.getValue().get$ref()).build();
            // log.info("::::::::::::::::param value::::::::::::"+entry1.getValue());
             log.info("::::::::::::::::in param:::::::::key:::::"+entry1.getKey()+":::::value::::::"+entry1.getValue().getExample()+":::::::type::::::"+entry1.getValue().getType());
            resultList.add(param);
        }
        return resultList;
    }

    private  List<Map<String, Operation>> getListRequestOperations(PathItem p) {
        List<Map<String, Operation>> opsList = new LinkedList<>();
        if (p.getGet() != null) {
            Map<String, Operation> opsMap = new HashMap<>();
            opsMap.put("GET", p.getGet());
            opsList.add(opsMap);
        }
        if (p.getPut() != null) {
            Map<String, Operation> opsMap = new HashMap<>();
            opsMap.put("PUT", p.getPut());
            opsList.add(opsMap);
        }
        if (p.getPost() != null) {
            Map<String, Operation> opsMap = new HashMap<>();
            opsMap.put("POST", p.getPost());
            opsList.add(opsMap);
        }
        if (p.getDelete() != null) {
            Map<String, Operation> opsMap = new HashMap<>();
            opsMap.put("DELETE", p.getDelete());
            opsList.add(opsMap);
        }
        if (p.getOptions() != null) {
            Map<String, Operation> opsMap = new HashMap<>();
            opsMap.put("OPTIONS", p.getOptions());
            opsList.add(opsMap);
        }
        if (p.getHead() != null) {
            Map<String, Operation> opsMap = new HashMap<>();
            opsMap.put("HEAD", p.getHead());
            opsList.add(opsMap);
        }
        if (p.getPatch() != null) {
            Map<String, Operation> opsMap = new HashMap<>();
            opsMap.put("PATCH", p.getPatch());
            opsList.add(opsMap);
        }
        if (p.getTrace() != null) {
            Map<String, Operation> opsMap = new HashMap<>();
            opsMap.put("TRACE", p.getTrace());
            opsList.add(opsMap);
        }
        return opsList;
    }

    private  Object getParamValue(Param param) {
        if (param.getChildNode() != null) {
            return param.getChildNode();
        }
        return manufacturePojo(getClassType(param.getType()));
    }

    @SuppressWarnings("unchecked")
    private  Object getParamValueFromArray(Param param) {
        if (param.getChildNode() instanceof String) {
            // log.info("::::::getParamValueFromArray:::::String:::" + param.getChildNode());
            List<Object> object = new ArrayList<>();
            object.add(param.getChildNode());
            return object;
        }
        if (param.getChildNode() instanceof ArrayNode) {
            return param.getChildNode();
        }
        Map<String, Object> objMap = parseParametersToBody((List<Param>) param.getChildNode());
        List<Object> object = new ArrayList<>();
        object.add(objMap);
        // log.info("::::::getParamValue:::::jsonArray:::" + objMap);
        return object;
    }

    @SuppressWarnings("unchecked")
    private  Object getParamValueFromObject(Param param) {
       // log.warn("::::::::::::::getParamValueFromObject ::::::::param:::::::" + param.toString());
        return parseParametersToBody((List<Param>) param.getChildNode());
    }

    private  Object manufacturePojo(Class<?> type) {
        Object manufacturedPojo = podamFactory.manufacturePojo(type);
        if (manufacturedPojo == null) {
            throw new PactGenerationException("Podam manufacturing failed");
        }
        return manufacturedPojo;
    }

    private  Map<String, Object> parseParametersToBody(List<Param> requestParameters) {
        Map<String, Object> mapParam = new LinkedHashMap<>();
        if (Objects.nonNull(requestParameters)) {
            requestParameters.forEach(param -> {
                if (param.getType() == null && param.getRef() != null) {
                    //	log.warn("::::::::::::::param.getRef():::::::::::::::" + param.getRef());
                    mapParam.put(param.getName(), getParamValue(param));
                } else if ("object".equals(param.getType()) || param.getType() == null) {
                    	//log.warn("::::::::::::::object Body:::::::::::::::" + param.getType());
                    mapParam.put(param.getName(), getParamValueFromObject(param));
                } else if ("array".equals(param.getType())) {
                    	//log.warn(":::::::::::::array:getChildNode:::::::::::::::" + param.getChildNode());
                    if (param.getChildNode() != null){
                        mapParam.put(param.getName(), getParamValueFromArray(param));
                    } else {
                        mapParam.put(param.getName(), getParamValue(param));
                    }
                } else {
                    	//log.warn("::::::::::::::default:::::::::::::::" + param.getName());
                    mapParam.put(param.getName(), getParamValue(param));
                }
                // log.info("::::::parseParametersToBody:::::Key:::" + param.getName() +"::::Value:::::"+ mapParam.get(param.getName()));
            });
        }
        Map<String, Object> newMapParam =  getFinalMap(mapParam,"ignore");
        return newMapParam;
    }
    public  Map<String, Object> getFinalMap(Map<String, Object> map, Object value) {
        Map<String, Object> filterMap = new LinkedHashMap<>();
        for (Entry<String, Object> entry : map.entrySet()) {
            if (!entry.getValue().equals(value)) {
                filterMap.put((String) entry.getKey(),entry.getValue());
            }
        }
        // log.info("::::::getFinalMap:::::filterMap:::" + filterMap.toString());
        return  filterMap;
    }

    private  Map<String, String> mapHeaders(List<Param> headers) {
        Map<String, List<String>> mappedHeadersWithDuplicates = mapHeadersWithDuplicates(headers);

        Map<String, String> resultingHeaders = new HashMap<>();
        for (String key : mappedHeadersWithDuplicates.keySet()) {
            if (mappedHeadersWithDuplicates.get(key).size() > 1) {
                log.warn("More than one value for header: {}", key);
            }
            resultingHeaders.put(key, mappedHeadersWithDuplicates.get(key).get(0));
        }
        return resultingHeaders;
    }

    private  Map<String, List<String>> mapHeadersWithDuplicates(List<Param> headers) {
        return headers.stream().collect(Collectors.groupingBy(Param::getName,
                Collectors.mapping(param -> String.valueOf(getParamValue(param)), Collectors.toList())));
    }

    private  Class<?> getClassType(String str) {
        if ("boolean".equalsIgnoreCase(str)) {
            return Boolean.class;
        }
        if ("string".equalsIgnoreCase(str)) {
            return String.class;
        }
        if ("integer".equalsIgnoreCase(str)) {
            return Integer.class;
        }
        if ("number".equalsIgnoreCase(str)) {
            return Integer.class;
        } else {
            return String.class;
        }
    }

    public Pact createPacts(OpenAPI openAPI, PactGeneratorService pactGeneratorService) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);

        return Pact.builder()
                .provider(new Services(pactGeneratorService.getProviderName())) // configure
                .consumer(new Services(pactGeneratorService.getConsumerName())) // configure
                .interactions(createInteractionsFromMethods(openAPI, objectMapper,pactGeneratorService.getToken()))
                .metadata(new Metadata(new PactSpecification(pactGeneratorService.getPactSpecificationVersion()))) // configure
                .build();
    }

    private List<Interaction> createInteractionsFromMethods(OpenAPI openAPI, ObjectMapper objectMapper,String token) {
        List<Interaction> interactionResults = new ArrayList<>();
        Paths paths = openAPI.getPaths();
        for (Map.Entry<String, PathItem> pathItem : paths.entrySet()) {
            for (Map<String, Operation> opsMap : getListRequestOperations(pathItem.getValue())) {
                for (Entry<String, Operation> stringOperationEntry : opsMap.entrySet()) {
                    List<Interaction> interactionList = getInteractionsForEveryResponseCode(stringOperationEntry, pathItem.getKey(), objectMapper,token);
                    interactionResults.addAll(interactionList);
                }
            }
        }
        return interactionResults;
    }

    private List<Interaction> getInteractionsForEveryResponseCode(Entry<String, Operation> entry, String path,
                                                                  ObjectMapper objectMapper,String token) {
        List<Interaction> interactionList = new LinkedList<>();
        if(entry.getValue().getDeprecated()== null){
            //log.info("non deprecated path "+entry.getValue().getOperationId());

            InteractionRequest interactionRequest = prepareInteractionRequest(entry, path, objectMapper,token);
            for (Entry<String, ApiResponse> stringApiResponseEntry : entry.getValue().getResponses().entrySet()) {
                InteractionResponse interactionResponse = null;
                Interaction interaction = null;
                if (isOnlyHappyCase) {
                    interactionResponse = prepareHappyInteractionResponse(objectMapper, stringApiResponseEntry);
                    if (Objects.nonNull(interactionResponse)) {
                        interaction = prepareInteraction(entry, interactionRequest, interactionResponse);
                        interactionList.add(interaction);
                    }
                } else {
                    ReqResPair pair = prepareInteractionResponse(objectMapper, stringApiResponseEntry,interactionRequest);
                    interaction = prepareInteraction(entry, pair.getReq(), pair.getRes());
                    interactionList.add(interaction);
                }
            }
        }

        return interactionList;
    }

   /* private InteractionRequest getRequestBodyForErrors(InteractionRequest interactionRequest) {
        if(interactionRequest.){

        }
    }*/

    private Interaction prepareInteraction(Entry<String, Operation> entry, InteractionRequest interactionRequest,
                                           InteractionResponse interactionResponse) {
        return Interaction.builder()
                .description(entry.getValue().getDescription() != null ? entry.getValue().getDescription() : entry.getValue().getOperationId())
                .providerState(entry.getValue().getOperationId())
                .request(interactionRequest)
                .response(interactionResponse).build();
    }

    private InteractionRequest prepareInteractionRequest(Entry<String, Operation> entry, String path,
                                                         ObjectMapper objectMapper,String token) {
        return InteractionRequest.builder().method(entry.getKey())
                .path(requestPathParamGenerator(entry.getValue(), path))
                .headers(requestHeaderParamGenerator(entry.getValue(),token))
                .query(requestQueryParamGenerator(entry.getValue()))
                .body(requestBodyGenerator(entry.getValue(), objectMapper)).build();
    }

    private InteractionResponse prepareHappyInteractionResponse(ObjectMapper objectMapper, Entry<String, ApiResponse> apiResponseEntry) {
        InteractionResponse interactionResponse = null;
        JsonNode matchingRule = null;
        String jsonString = "{\n" +
                "\t\t\t\"$.body\": {\n" +
                "\t\t\t\t\"match\": \"type\"\n" +
                "\t\t\t}\n" +
                "\t\t}";

        ObjectMapper mapper = new ObjectMapper();
        try {
            matchingRule = mapper.readTree(jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if (!"default".equalsIgnoreCase(apiResponseEntry.getKey()) && HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is2xxSuccessful()) {
            interactionResponse = InteractionResponse.builder().status(apiResponseEntry.getKey())
                    .headers(responseHeaderParamGenerator(apiResponseEntry.getValue().getHeaders()))
                    .body(responseBodyGenerator(apiResponseEntry, objectMapper))
                    .matchingRules(matchingRule)
                    .build();
        }
        return interactionResponse;
    }

    private ReqResPair prepareInteractionResponse(ObjectMapper objectMapper, Entry<String, ApiResponse> apiResponseEntry,InteractionRequest interactionRequest) {
        JsonNode matchingRule = null;
        JsonNode emptyBodyJson = null;
        String jsonString = "{\n" +
                "\t\t\t\"$.body\": {\n" +
                "\t\t\t\t\"match\": \"type\"\n" +
                "\t\t\t}\n" +
                "\t\t}";
        String emptyBody =   "{\n" +
                "        \"invalid\" : \"request\"\n" +
                "      }";
        ObjectMapper mapper = new ObjectMapper();
        try {
            matchingRule = mapper.readTree(jsonString);
            emptyBodyJson = mapper.readTree(emptyBody);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        if(!HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is2xxSuccessful() &&
                HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is4xxClientError()){
            interactionRequest = get4xxInteractionRequest(apiResponseEntry, interactionRequest, matchingRule, emptyBodyJson);
        }
        InteractionResponse  interactionResponse =  InteractionResponse.builder().status(apiResponseEntry.getKey())
                .headers(responseHeaderParamGenerator(apiResponseEntry.getValue().getHeaders()))
                .body(responseBodyGenerator(apiResponseEntry, objectMapper))
                .matchingRules(matchingRule)
                .build();

        return ReqResPair.builder()
                .req(interactionRequest)
                .res(interactionResponse)
                .build();
    }

    private InteractionRequest get4xxInteractionRequest(Entry<String, ApiResponse> apiResponseEntry, InteractionRequest interactionRequest, JsonNode matchingRule, JsonNode emptyBodyJson) {
        if("404".equalsIgnoreCase(apiResponseEntry.getKey())) {
            interactionRequest =  InteractionRequest.builder().method(interactionRequest.getMethod())
                    .path(interactionRequest.getPath().replace("v1","v3"))
                    .headers(interactionRequest.getHeaders())
                    .query(interactionRequest.getQuery())
                    .body(interactionRequest.getBody()).build();
        }else if("401".equalsIgnoreCase(apiResponseEntry.getKey())){
            if(interactionRequest.getHeaders()!=null){
                interactionRequest.getHeaders().put("Authorization", interactionRequest.getHeaders().get("Authorization")+"invalid");
            }
            interactionRequest =  InteractionRequest.builder().method(interactionRequest.getMethod())
                    .path(interactionRequest.getPath())
                    .headers(interactionRequest.getHeaders())
                    .query(interactionRequest.getQuery())
                    .body(interactionRequest.getBody()).build();
        }else if("400".equalsIgnoreCase(apiResponseEntry.getKey())){
            interactionRequest =  InteractionRequest.builder().method(interactionRequest.getMethod())
                    .path(interactionRequest.getPath())
                    .headers(interactionRequest.getHeaders())
                    .query(interactionRequest.getQuery())
                    .body(emptyBodyJson).build();
        }else if("415".equalsIgnoreCase(apiResponseEntry.getKey())){
            interactionRequest =  InteractionRequest.builder().method(interactionRequest.getMethod())
                    .path(interactionRequest.getPath())
                    .headers(interactionRequest.getHeaders())
                    .query(interactionRequest.getQuery())
                    .body(matchingRule).build();
        }
        return interactionRequest;
    }

    @SuppressWarnings("unchecked")
    private JsonNode responseBodyGenerator(Entry<String, ApiResponse> apiResponseEntry, ObjectMapper objectMapper) {
        List<Param> resultList = new ArrayList<>();
        if (apiResponseEntry.getValue().getContent() == null) {
            return null;
        }
        Iterator<Entry<String, MediaType>> contentEntryIterator = apiResponseEntry.getValue().getContent().entrySet().iterator();
        try {
            String jsonString = null;
            ErrorResources errors = new ErrorResources();
            while (contentEntryIterator.hasNext()) {
                Entry<String, MediaType> entryMediaType = contentEntryIterator.next();
                Schema schema = entryMediaType.getValue().getSchema();
                if (Objects.nonNull(schema.getProperties())) {
                    log.info("::::::::in responseBodyGenerator default:::");
                    Iterator<Map.Entry<String, Schema>> itr = schema.getProperties().entrySet().iterator();
                    while (itr.hasNext()) {
                        resultList = extractedParamList(resultList, itr);
                    }
                    if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is2xxSuccessful()) {//2xx
                        Map<String, Object> map = parseParametersToBody(resultList);
                        jsonString = objectMapper.writeValueAsString(map);
                    } else if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).isError()) {//only 4xx and 5xx
                        errors = errors.getCompleteDescription(Integer.parseInt(apiResponseEntry.getKey()));
                        jsonString = objectMapper.writeValueAsString(errors);

                    }
                }else if (entryMediaType.getValue().getSchema() instanceof ComposedSchema) {
                    log.info("::::::::in responseBodyGenerator ComposedSchema:::");
                        ComposedSchema composedModel = (ComposedSchema) entryMediaType.getValue().getSchema();
                        List<Schema> schema1 = composedModel.getAllOf();
                        if(schema1==null){
                            schema1 = composedModel.getAnyOf();
                        }
                        if(schema1==null){
                            schema1 = composedModel.getOneOf();
                        }
                        if(Objects.nonNull(schema1)) {
                            Schema<?> schema2 = schema1.get(0);
                            Iterator<Map.Entry<String, Schema>> itr = schema2.getProperties().entrySet().iterator();
                            while (itr.hasNext()) {
                                resultList = extractedParamList(resultList, itr);
                            }
                        }
                    if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is2xxSuccessful()) {//2xx
                        Map<String, Object> map = parseParametersToBody(resultList);
                        jsonString = objectMapper.writeValueAsString(map);
                    } else if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).isError()) {//only 4xx and 5xx
                        errors = errors.getCompleteDescription(Integer.parseInt(apiResponseEntry.getKey()));
                        jsonString = objectMapper.writeValueAsString(errors);

                    }
                }else if (schema instanceof ArraySchema) {
                    log.info("::::::::in responseBodyGenerator ArraySchema:::");
                    List<List<Param>> resultList1 = new ArrayList<>();
                    ArraySchema arrayModel = (ArraySchema) schema;
                    resultList = getParams(resultList, arrayModel);
                    resultList1.add(resultList);
                    if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).is2xxSuccessful()) {//2xx
                        List<Map<String, Object>> mapList = parseParametersToBodyAsList(resultList1);
                        jsonString = objectMapper.writeValueAsString(mapList);
                    } else if (HttpStatus.valueOf(Integer.parseInt(apiResponseEntry.getKey())).isError()) {//only 4xx and 5xx
                        errors = errors.getCompleteDescription(Integer.parseInt(apiResponseEntry.getKey()));
                        jsonString = objectMapper.writeValueAsString(errors);

                    }

                }else if (schema instanceof MapSchema) {
                    log.error(":::: No Rules defined for MapSchema  :::: ");
                    //Schema additionalProperty = (Schema) schema.getAdditionalProperties();
                }
            }
            //	log.info("::::::::Request Body String :::"+jsonString);
            //	log.info("::::::::Response Body JSON :::"+jsonNode);
            return objectMapper.readTree(jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return objectMapper.nullNode();

    }

    private List<Map<String, Object>> parseParametersToBodyAsList(List<List<Param>> resultList1) {
        List<Map<String, Object>> mapList = new ArrayList<>();
        for (List<Param> arrayBody : resultList1) {
            Map<String, Object> map = parseParametersToBody(arrayBody);
            mapList.add(map);
        }
        return mapList;
    }

}
