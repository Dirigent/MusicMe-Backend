package com.musicmeleaderboardfunctions;

public class UserScore {
    private String id;
    private String username;
    private int score;
    public UserScore(){}

    public UserScore(String username, String id, int score){
        this.username = username;
        this.id = id;
        this.score = score;

    }

    public void setScore(int score){
        this.score = score;
    }

    public String getUsername(){
        return this.username;
    }
    
    public int getScore(){
        return this.score;
        
    }

    public String getId(){
        return this.id;
    }



    @Override
    public String toString() {
        return "UserScore={id=" + id + ",username=" + username + ",score= "+score+"}";
    }
}
