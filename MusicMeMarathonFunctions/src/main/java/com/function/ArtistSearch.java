package com.function;

import java.util.List;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.implementation.guava25.base.Optional;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.HttpMethod;



public class ArtistSearch {
    CosmosClient client = ClientSingleton.getInstance().getClient();
    CosmosDatabase cosmosDatabase = client.getDatabase("Songs");
    

    //Parts of the code are used under Creative Commons License from https://github.com/MicrosoftDocs/azure-docs
    @FunctionName("ArtistSearch")
    public HttpResponseMessage run2(
        @HttpTrigger(name = "artistSearch",
            methods = {HttpMethod.GET, HttpMethod.POST},
            authLevel = AuthorizationLevel.ANONYMOUS,
            route = "container/{container}/{name}")
            HttpRequestMessage<Optional<String>> request,
            @BindingName("name") String name,
            @BindingName("container") String containerId,
            final ExecutionContext context) {
                ObjectMapper objectMapper = Utils.getSimpleObjectMapper();
                CosmosContainer container = cosmosDatabase.getContainer(containerId);


                String query = getQuery(containerId);
                final int mid = name.length() / 2;
                String halfName = name.substring(0, mid);
                SqlParameter[] parameters = {new SqlParameter("@name", name), new SqlParameter("@halfName", halfName)};

                SqlQuerySpec querySpec = new SqlQuerySpec(query, parameters);

                List<JsonNode> artists = QueryUtilities.queryCollection(querySpec, container, new CosmosQueryRequestOptions());
                
                
                if (artists.size() > 0) {
                    try {
                        return request.createResponseBuilder(HttpStatus.OK).body(objectMapper.writeValueAsString(artists)).build();
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Failed to parse json").build();
            
                    }
                } else {
                    return request.createResponseBuilder(HttpStatus.OK).body(artists).build();
                }
            }

            private String getQuery(String containerId){
                if(containerId.equals("artists")){
                    return "SELECT DISTINCT TOP 10 c.name FROM c WHERE CONTAINS(c.name, @name) OR STARTSWITH(c.name, @halfName)";
                } else {
                    return "SELECT DISTINCT {\"name\": c.name, \"artist\": c.artist} AS song FROM c WHERE c.artist = @name offset 0 LIMIT 20";
                }
            }
                

}