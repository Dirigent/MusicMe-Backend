package com.musicmeleaderboardfunctions;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
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

public class LeaderboardUpdator {
    private CosmosClient client = ClientSingleton.getInstance().getClient();
    private CosmosDatabase cosmosDatabase = client.getDatabase("Songs");
    private CosmosContainer container = cosmosDatabase.getContainer("HighScores");
    private ObjectMapper mapper = new ObjectMapper();


//Parts of the code are used under Creative Commons License from MicrosoftDocs
@FunctionName("leaderboardUpdator")
public HttpResponseMessage run(@HttpTrigger(name = "levelReq",
 methods = {
    HttpMethod.PUT },
    authLevel = AuthorizationLevel.ANONYMOUS,
    route = "leaderboards/{id}")
    HttpRequestMessage<Optional<UserScore>> request,
    @BindingName("id") String type,     
        final ExecutionContext context) {
            String response = "";
            UserScore userScore = request.getBody().orElse(null);
            if(userScore == null){
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Bad Request " + HttpStatus.BAD_REQUEST.value()).build();
            }
            
            CosmosItemRequestOptions options = new CosmosItemRequestOptions();
            
            JsonNode node = container.readItem(type, new PartitionKey(type), JsonNode.class).getItem();

            
            
            List<UserScore> currentScores = Utilities.jsonNodeToList(node);

            UserScoreChangeBundle bundle = sortScore(userScore, currentScores);
            Leaderboard leaderboard = new Leaderboard(type, bundle.getScores());
            if(bundle.getChanged()){
                CosmosItemResponse something = container.replaceItem(leaderboard, type, new PartitionKey(type), options);
                
                //Cosmos DB generates garbage this is done to generate proper response with relevant information only
                Leaderboard leaderboard2 = new Leaderboard(type, bundle.getScores());
                try {
                    response = mapper.writeValueAsString(leaderboard2);
                    return request.createResponseBuilder(HttpStatus.OK).body(response).build();
                } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Bad Request " + HttpStatus.BAD_REQUEST.value()).build();

                }
            } else {

                try {
                    return request.createResponseBuilder(HttpStatus.OK).body(mapper.writeValueAsString(leaderboard))
                            .build();
                } catch (JsonProcessingException e) {
                    return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to Process").build();
                }
            }
            
   

    }

    //Sorts the list of high scores
    private UserScoreChangeBundle userSorter(UserScore userScore, List<UserScore> list, int index){
            UserScore store;
            boolean changed = false;
            if(userScore.getScore() > list.get(index).getScore()) {
                list.set(index, userScore);
                for(int e = index; e > 0; e--) {
                    if(list.get(e).getScore()> list.get(e-1).getScore()) {
                        store = list.get(e-1);
                        list.set(e-1, list.get(e));
                        list.set(e, store);
                    }
                            
                }
                changed = true;
            }
        return new UserScoreChangeBundle(changed, list);
    }

    //determines if the user already has an entry in the list
    private UserScoreChangeBundle sortScore(UserScore userScore, List<UserScore> list){

            for(int i = 0; i < list.size(); i++){
                    if(list.get(i).getId().equals(userScore.getId())){
                        return userSorter(userScore, list, i);
                    }
            }

            return userSorter(userScore, list, list.size()-1);
            
    }
}
