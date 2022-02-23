package com.musicmeuserfunctions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.Random;

// TODO: formatovani

public class UserCreator {
    Random random = new Random();

    //Parts of the code are used under Creative Commons License from https://github.com/MicrosoftDocs/azure-docs
    @FunctionName("UserCreator")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
            HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS, route =
            "users") HttpRequestMessage<Optional<String>> request,
                                   @CosmosDBOutput(name = "database",
                                           databaseName = "Songs",
                                           collectionName = "Users",
                                           connectionStringSetting = "")
                                           OutputBinding<String> outputItem,
                                   final ExecutionContext context) {
        ObjectMapper mapper = new ObjectMapper();
        // TODO: deklarovat az je potreba
        //TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};

        // TODO: toto je zbytecna podminka, nemuze byt nikdy null
        if (request.getBody().get() == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Please pass a name on the query string or in the request body")
                    .build();
        } else {
            try {
                TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
                HashMap<String, Object> map = mapper.readValue(request.getBody().get(), typeRef);
                map.put("groupId", random.nextInt(20_000));
                map.put("id", UUID.randomUUID().toString());
                String document = mapper.writeValueAsString(map);
                outputItem.setValue(document);
                return request.createResponseBuilder(HttpStatus.OK).body(document).build();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("Please pass a name on the query string or in the request body")
                        .build();
            }
        }
    }
}
