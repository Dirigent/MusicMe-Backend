package com.function.musicme.songfunctions;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;

// TODO: duplikat
public final class ClientSingleton {
    //Follows singleton pattern to reuse connection to CosmosDB when needed.
    private static volatile ClientSingleton INSTANCE = null;
    private ClientSingleton(){}
    private CosmosClient client;
    private int count = 0;

    public static ClientSingleton getInstance(){ 
        if (INSTANCE  == null){
            synchronized (ClientSingleton.class){
                if (INSTANCE == null){
                    INSTANCE = new ClientSingleton();
                    INSTANCE.setDocumentClient();
                }
            }
        }
        return INSTANCE;  
    }

    public int getCount(){
        return ++this.count;
    }

    public CosmosClient getClient() {
        return this.client;
    }

    private void setDocumentClient() {
         this.client = new CosmosClientBuilder()
                .endpoint(AccountSettings.HOST)
                .key(AccountSettings.MASTER_KEY)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildClient();
    }
    
}