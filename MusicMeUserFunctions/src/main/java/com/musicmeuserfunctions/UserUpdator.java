package com.musicmeuserfunctions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import java.util.Optional;

public class UserUpdator {
    private CosmosClient client = ClientSingleton.getInstance().getClient();
    private CosmosDatabase cosmosDatabase = client.getDatabase("Songs");
    private CosmosContainer container = cosmosDatabase.getContainer("Users");
    ObjectMapper mapper = new ObjectMapper();
    
    //Parts of the code are used under Creative Commons License from https://github.com/MicrosoftDocs/azure-docs
    @FunctionName("UserUpdator")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = {
            HttpMethod.PUT }, authLevel = AuthorizationLevel.ANONYMOUS, route = "users/group-id/{groupId}/user-id/{userId}") HttpRequestMessage<Optional<String>> request,
            @BindingName("groupId") int groupId, @BindingName("userId") String userId,
            final ExecutionContext context) {

        TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {};
        String node = container.readItem(userId, new PartitionKey(groupId), JsonNode.class).getItem().toString();
    
        if (!request.getBody().isPresent()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .body("Post").build();
        } else {
            try {
                HashMap<String, Object> currentMap = mapper.readValue(node, typeRef);
                HashMap<String, Object> newAtts = mapper.readValue(request.getBody().orElse(null), typeRef);
                HashMap<String, Object> newMap = populateMap(newAtts, currentMap);
                container.replaceItem(newMap, userId, new PartitionKey(groupId), new CosmosItemRequestOptions());
                return request.createResponseBuilder(HttpStatus.OK).body(mapper.writeValueAsString(newMap)).build();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("Failed to Process").build();
            } 
        }

    }
    /*Checks if new values are already present and if they are ann array or singular, 
    if they are an array, the array is extended otherwise the value is replaced */
    @SuppressWarnings("unchecked")
    private HashMap<String, Object> populateMap(HashMap<String, Object> newAtts, HashMap<String, Object> map) {
        Object[] keySet = newAtts.keySet().toArray();
        for(int i = 0; i < keySet.length; i++){
            if(map.containsKey(keySet[i]) && map.get(keySet[i]) instanceof List<?>){
                List<Object> newList = new ArrayList<>();
                newList = (List<Object>) newAtts.get(keySet[i]);
                newList.addAll((List<Object>) map.get(keySet[i]));
                map.replace(keySet[i].toString(), newList);
            } else {
                map.replace(keySet[i].toString(), newAtts.get(keySet[i]));
            }
            newAtts.remove(keySet[i].toString());
        } 
        map.putAll(newAtts);
        return map;
    }
}
