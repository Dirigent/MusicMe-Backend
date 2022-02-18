package com.function.musicme.songfunctions;


import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.OutputBinding;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;


import java.util.Optional;

public class SongCreator {

    //Creates a new document if 
    @FunctionName("SongCreator")
    public HttpResponseMessage run(@HttpTrigger(name = "req", methods = { HttpMethod.POST}, authLevel = AuthorizationLevel.ADMIN, route = "songs") HttpRequestMessage<Optional<String>> request,
            @CosmosDBOutput(name = "database",
            databaseName = "Songs",
            collectionName = "songs2",
            connectionStringSetting = "connectionString")
            OutputBinding<String> outputItem,
            final ExecutionContext context) {


            final String body = request.getBody().orElse(null);

            if (body != null) {
                outputItem.setValue(body);
                return request.createResponseBuilder(HttpStatus.OK).body(body).build();
            } else {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).build();
            }
        }
            
}
