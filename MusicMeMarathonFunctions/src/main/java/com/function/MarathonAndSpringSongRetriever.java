package com.function;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
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
import com.azure.cosmos.models.PartitionKey;


import java.util.List;
import java.util.Optional;
import java.util.Random;


public class MarathonAndSpringSongRetriever {
    CosmosClient client = ClientSingleton.getInstance().getClient();
    CosmosDatabase database = ClientSingleton.getInstance().getClient().getDatabase("Songs");
    CosmosContainer container = database.getContainer("songs");

    Random rand = new Random();
    int offset;


    @FunctionName("songs")
    public HttpResponseMessage run(@HttpTrigger(name = "songReq", methods = {
            HttpMethod.GET }, authLevel = AuthorizationLevel.ANONYMOUS, route = "songs/genre/{genre}/{limit}/{stage}") HttpRequestMessage<Optional<String>> request,
            @BindingName("limit") int limit,
            @BindingName("stage") int stage,
            @BindingName("genre") String genre,
             final ExecutionContext context) {
        ObjectMapper objectMapper = Utils.getSimpleObjectMapper();
        String query;
        
        
        CosmosQueryRequestOptions queryRequestOptions = new CosmosQueryRequestOptions();
        queryRequestOptions.setPartitionKey(new PartitionKey(genre));
        int count = QueryUtilities.getCount(queryRequestOptions, container);


        int maxStages = (count / limit);
        

        if (stage >= maxStages) {
            stage = rand.nextInt(maxStages);
        }
        if ((stage * limit) > (count / 2)) {
            offset = Utilities.calculateOppositeOffset(stage, count, limit);

            query = QueryUtilities.ASC_QUERY;
        } else {
            offset = Utilities.calculateOffset(stage, count, limit);
        
            query = QueryUtilities.DESC_QUERY;

        }

        List<JsonNode> songs = QueryUtilities.queryCollection(QueryUtilities.buildQuerySpecs(offset, limit, query), container, queryRequestOptions);

        if(songs == null) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("BAD REQUEST").build();
        }

        try {
            return request.createResponseBuilder(HttpStatus.OK).body(objectMapper.writeValueAsString(songs)).build();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error").build();
        }
                    

                
    }

   
}


        

