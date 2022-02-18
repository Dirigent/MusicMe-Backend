package com.function.musicme.songfunctions;

import java.util.Random;

public class Utilities {


    //Calculates offset of songs in descending order
    public static int calculateOffset(int stage, int count, int limit) {
        Random rand = new Random();
        int offset = stage * 10;

        offset = randomInt(rand.nextInt(3), rand.nextInt(1+(count/15)), offset);
 

        return Math.abs(offset);
    }
    //Calculates offset of songs in ascending order
    public static  int calculateOppositeOffset(int stage, int count, int limit) {
        Random rand = new Random();

        int offset = count - limit - (stage * 10);
        if(0 > offset){
        	offset = 0;
        }

        offset = randomInt(rand.nextInt(3), rand.nextInt(1+(count/15)), offset);


        return Math.abs(offset);
    } 

    //generates random int based on a random factor
    private static int randomInt(int ranFactor, int ranFactor2, int offset) {
        if(ranFactor == 1){
            offset -= ranFactor2;
        } else {
            offset += ranFactor2;
        }

        return offset;
    }
}
