package com.musicmeleaderboardfunctions;

import java.util.List;


public class Leaderboard {
    private String id;
    private List<UserScore> userScores;

    Leaderboard(String id, List<UserScore> userScores){
        this.id = id;
        this.userScores = userScores;
    }

    public String getId(){
        return this.id;
    }

    public List<UserScore> getUserScores(){
        return this.userScores;
    }

    public void setId(String id){
        this.id = id;
    }

    public void setUserScore(List<UserScore> userScores){
        this.userScores = userScores;
    }
}
