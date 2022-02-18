package com.musicmeleaderboardfunctions;

import java.util.ArrayList;
import java.util.List;


import com.fasterxml.jackson.databind.JsonNode;

public class Utilities {

    //Retrieves list of userscore objects form leaderboard
    public static List<UserScore> jsonNodeToList(JsonNode leaderboard){
        List<UserScore> currentScores = new ArrayList<>();
        for(JsonNode node : leaderboard.get("userScores")){
            currentScores.add(new UserScore(node.get("username").asText(), node.get("id").asText(), node.get("score").asInt()));
        }
       return currentScores;
    }
    

  
}
