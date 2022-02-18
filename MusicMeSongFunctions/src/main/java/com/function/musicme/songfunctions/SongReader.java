package com.function.musicme.songfunctions;


import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;


/**
 * Azure Functions with HTTP Trigger.
 */
public class SongReader {
    private CosmosClient client = ClientSingleton.getInstance().getClient();
    private CosmosDatabase cosmosDatabase = client.getDatabase("Songs");
    private CosmosContainer container = cosmosDatabase.getContainer("songs");
    private Random rand = new Random();

    @FunctionName("SongReader")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS,
                route = "songs")
            
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
                ObjectMapper objectMapper = Utils.getSimpleObjectMapper();
                int limit = 20;
                int stage = 0;
            
                CosmosQueryRequestOptions queryRequestOptions = new CosmosQueryRequestOptions();
    
                Map<String, String> params = new HashMap<>();
                if(request.getQueryParameters().isEmpty()) {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("BAD REQUEST").build();
                }
                params.putAll((Map<String, String>) request.getQueryParameters());

                if(params.containsKey("genre")){
                    queryRequestOptions.setPartitionKey(new PartitionKey(params.get("genre")));
                    params.remove("genre");
                }

                stage = this.removeParam(params, QueryUtilities.QUERY_PARRAM_STAGE, stage);
                limit = this.removeParam(params, QueryUtilities.QUERY_PARRAM_LIMIT, limit);
    
                SqlQuerySpec specs = this.buildQuery(params);
                int count = QueryUtilities.getCount(queryRequestOptions, container, specs);
    
                stage = this.findStage(stage, count, limit);
    
                specs = this.setSqlQuerySpecText(specs, stage, limit, count);

               List<JsonNode> songs = QueryUtilities.queryCollection(specs, container, queryRequestOptions);
        
                try {
                    return request.createResponseBuilder(HttpStatus.OK).header("Access-Control-Allow-Origin", "*").header("Access-Control-Allow-Credentials", "true").body(objectMapper.writeValueAsString(songs)).build();
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                    return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error").build();
                } 
        
    }
    //Builds query string and ads sql parameters to prepared statement
    private SqlQuerySpec setSqlQuerySpecText(SqlQuerySpec specs,int stage,int limit, int count){
        int offset = 0;
        if ((stage * limit) > (count / 2)) {
            offset = Utilities.calculateOppositeOffset(stage, count, limit);
            specs.getParameters().add(new SqlParameter("@offset", offset));
            specs.getParameters().add(new SqlParameter("@limit", limit));
            specs.setQueryText(QueryUtilities.BASE_READ_QUERY + specs.getQueryText() + QueryUtilities.ASC_ENDING);
        } else {
            offset = Utilities.calculateOffset(stage, count, limit);
            specs.getParameters().add(new SqlParameter("@offset", offset));
            specs.getParameters().add(new SqlParameter("@limit", limit));
            specs.setQueryText(QueryUtilities.BASE_READ_QUERY + specs.getQueryText() + QueryUtilities.DESC_ENDING);
        }
        return specs;
    }
    //Calculates maximum stages if a user has a higher stage a random stage is selected
    private int findStage(int stage, int count, int limit){
        int maxStages = (count / limit);
        if (stage >= maxStages) {
            return rand.nextInt(maxStages+1);
        } else {
            return stage;
        }
    }
  
    //builds query by cheching if query parameters are appropriate
    private SqlQuerySpec buildQuery(Map<String, String> params){
        List<SqlParameter> parameters = new ArrayList<>();
        String query = "";
        Object[] keys = params.keySet().toArray();
        for(int i = 0; i < params.size(); i++){
            String parameter = (String) keys[i];
            if(parameter.equals(QueryUtilities.QUERY_PARRAM_ARTIST)){
                query += queryOperators(i)+ QueryUtilities.QUERY_START +  parameter + " = @" + parameter;
                parameters.add(new SqlParameter("@"+parameter, params.get(parameter)));
            } else if (parameter.equals(QueryUtilities.QUERY_PARRAM_DECADE) || parameter.equals(QueryUtilities.QUERY_PARRAM_CENTURY)){
                query += queryOperators(i)+ QueryUtilities.QUERY_START_TIME +  parameter + " = @" + parameter;
                parameters.add(new SqlParameter("@"+parameter, params.get(parameter)));
            } else if (parameter.equals(QueryUtilities.QUERY_PARRAM_YEAR)){
                query += queryOperators(i)+QueryUtilities.QUERY_START_TIME +  parameter + " = @" + parameter;
                parameters.add(new SqlParameter("@"+parameter, Integer.parseInt(params.get(parameter))));
            } else if (parameter.equals(QueryUtilities.QUERY_PARRAM_GENDER)){
                query += queryOperators(i)+QueryUtilities.QUERY_START +  parameter + " = @" + parameter;
                parameters.add(new SqlParameter("@"+parameter, params.get(parameter)));
            } else if (parameter.equals(QueryUtilities.QUERY_PARRAM_AUDIENCE)){
                query += queryOperators(i)+QueryUtilities.QUERY_START +  parameter + " = @" + parameter;
                parameters.add(new SqlParameter("@"+parameter, params.get(parameter)));
            } 
            
        }
        SqlQuerySpec querySpec = new SqlQuerySpec(query, parameters);
        
        return querySpec;
    }
    //returns query operators based on the currrent loop iteration 
    private String queryOperators(int i){
        if(i == 0){
            return " WHERE";
        } else {
            return " AND";
        }
    }
    // removes parameter and returns the value of the parameter if it is present
    private int removeParam(Map<String, String> params, String key, int num){
        if(params.containsKey(key)){
            num = Integer.parseInt(params.get(key));
            params.remove(key);
            return num;
        } else {
            return num;
        }
    }
}
