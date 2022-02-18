package com.musicmeleaderboardfunctions;

import java.util.List;


public class UserScoreChangeBundle {
    private boolean changed;
    private List<UserScore> scores;

    public UserScoreChangeBundle(boolean changed, List<UserScore> scores){
        this.changed = changed;
        this.scores = scores;
    
    }

    public boolean getChanged(){
        return this.changed;
    }

    public List<UserScore> getScores(){
        return this.scores;
    }
}
