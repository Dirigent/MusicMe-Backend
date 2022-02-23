package com.musicmeleaderboardfunctions;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
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

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class LeaderboardReader {

    // TODO: pravdepodobne private a final budou ok
    private final CosmosClient client = ClientSingleton.getInstance().getClient();
    private final CosmosDatabase cosmosDatabase = client.getDatabase("Songs");
    private final CosmosContainer container = cosmosDatabase.getContainer("HighScores");

    @FunctionName("LeaderboardReader")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {
                    HttpMethod.GET}, authLevel = AuthorizationLevel.ANONYMOUS, route = "leaderboards/{id}") HttpRequestMessage<Optional<String>> request,
            @BindingName("id") String id, final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");
        ObjectMapper mapper = new ObjectMapper();

        JsonNode node = container.readItem(id, new PartitionKey(id), JsonNode.class).getItem();
        if (node != null) {
            Leaderboard leaderboard = new Leaderboard(node.get("id").asText(), Utilities.jsonNodeToList(node));
            try {
                return request.createResponseBuilder(HttpStatus.OK)
                        .body(mapper.writeValueAsString(leaderboard))
                        .build();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                        .body("Bad Request")
                        .build();
            }
        } else {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Bad Request").build();
        }
    }
}
