package com.musicmeleaderboardfunctions;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;


// TODO: Varianta vytvoreni primo
public final class ClientSingleton2 {

    private static final ClientSingleton2 INSTANCE = new ClientSingleton2();
    public static ClientSingleton2 getInstance() {
        return INSTANCE;
    }

    private final CosmosClient client;
    private int count = 0;

    private ClientSingleton2(){
        this.client = new CosmosClientBuilder()
                .endpoint(AccountSettings.HOST)
                .key(AccountSettings.MASTER_KEY)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildClient();
    }

    public int getCount(){
        return ++this.count;
    }

    public CosmosClient getClient() {
        return this.client;
    }
}