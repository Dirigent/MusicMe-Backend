package com.musicmeleaderboardfunctions;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public final class ClientSingleton {

    // TODO: je nejaky duvod vytvaret LAZY instanci? Neda se udelat rovnou - viz variant class ClientSingleton2?
    private static volatile ClientSingleton INSTANCE = null;

    private static final Lock instanceLock = new ReentrantLock();

    public static ClientSingleton getInstance() {
        // TODO: to, co chceme synchronizavci ochranit, je promenna INSTANCE. V tomto pripade se ale testuje na null
        // jeste pred sycnrhonizaci. Da se udelat lepe

        try {
            instanceLock.lock();
            if (INSTANCE == null) {
                INSTANCE = new ClientSingleton();
                INSTANCE.setDocumentClient();
            }
            return INSTANCE;
        } finally {
            instanceLock.unlock();
        }

        // Original
//        if (INSTANCE  == null){
//            synchronized (ClientSingleton.class){
//                if (INSTANCE == null){
//                    INSTANCE = new ClientSingleton();
//                    INSTANCE.setDocumentClient();
//                }
//            }
//        }
//        return INSTANCE;
    }

    // TODO: je prehlednejsi usporadat kod do bloku, kde budou pohromade staticke promennr/funkce, atributy a pod
    private CosmosClient client;
    private int count = 0;

    private ClientSingleton(){}

    // TODO: tato metoda sice neni pouzita, ale getter (jmeno getCount) je matouci. Vhodnejsi by bylo: getCountAndIncrease()
    public int getCount(){
        return ++this.count;
    }

    public CosmosClient getClient() {
        return this.client;
    }

    // TODO: prehlednejsi by bylo presunout ten kod do konstructoru
    private void setDocumentClient() {
         this.client = new CosmosClientBuilder()
                .endpoint(AccountSettings.HOST)
                .key(AccountSettings.MASTER_KEY)
                .consistencyLevel(ConsistencyLevel.EVENTUAL)
                .buildClient();
    }
}