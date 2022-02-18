package com.function;


import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.fasterxml.jackson.core.JsonProcessingException;
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


import java.util.List;
import java.util.Optional;
import java.util.Random;


public class LevelRetriever {
        CosmosClient client = ClientSingleton.getInstance().getClient();
        CosmosDatabase cosmosDatabase = client.getDatabase("Songs");
        CosmosContainer container = cosmosDatabase.getContainer("songs");
        CosmosAsyncClient cosmosAsynClient = new CosmosClientBuilder()
            .endpoint(AccountSettings.HOST)
            .key(AccountSettings.MASTER_KEY)
            .consistencyLevel(ConsistencyLevel.EVENTUAL)
            .buildAsyncClient();


    @FunctionName("levels")
    public HttpResponseMessage run(@HttpTrigger(name = "levelReq",
     methods = {
        HttpMethod.GET },
         authLevel = AuthorizationLevel.ANONYMOUS,
          route = "levels/genre/{genre}/limit/{limit}/{level}")
         HttpRequestMessage<Optional<String>> request,
            @BindingName("genre") String genre,
            @BindingName("limit") int limit,
            @BindingName("level") int level,
            final ExecutionContext context) {
             ObjectMapper objectMapper = Utils.getSimpleObjectMapper();
                Random rand = new Random();

                CosmosQueryRequestOptions queryRequestOptions = new CosmosQueryRequestOptions();
                queryRequestOptions.setPartitionKey(new PartitionKey(genre));
                
                int count = QueryUtilities.getCount(queryRequestOptions, container);
                int maxLevel = count/27;
                if(level > maxLevel) {
                    level = rand.nextInt(maxLevel+1);
                }
            

            List<JsonNode> songs= QueryUtilities.getSongs(maxLevel, limit, level, count, queryRequestOptions, container, QueryUtilities.DESC_QUERY, QueryUtilities.ASC_QUERY);

            if(songs.size() > 0) {
                try {
                    return request.createResponseBuilder(HttpStatus.OK).body(objectMapper.writeValueAsString(songs)).build();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to parse json").build();
        
                }
            } else {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Something Went Wrong").build();
            } 

        }
    
     
}
 
    
