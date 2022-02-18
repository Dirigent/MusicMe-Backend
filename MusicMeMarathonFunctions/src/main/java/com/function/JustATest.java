package com.function;

import com.azure.cosmos.ConsistencyLevel;
import com.azure.cosmos.CosmosAsyncClient;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
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
import com.microsoft.azure.functions.annotation.BindingName;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import javax.swing.text.html.HTMLDocument.HTMLReader.ParagraphAction;


public class JustATest {
        CosmosClient client = ClientSingleton.getInstance().getClient();
        CosmosDatabase cosmosDatabase = client.getDatabase("Songs");
        CosmosContainer container = cosmosDatabase.getContainer("songs");
        Random rand = new Random();


    @FunctionName("test")
    public HttpResponseMessage run(@HttpTrigger(name = "test",
     methods = {HttpMethod.GET },
         authLevel = AuthorizationLevel.ANONYMOUS)
         HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
            ObjectMapper objectMapper = Utils.getSimpleObjectMapper();

            int limit = 20;

            int stage = 0;
            int offset = 0;
            

            CosmosQueryRequestOptions queryRequestOptions = new CosmosQueryRequestOptions();
    

            Map<String, String> params = (Map<String, String>) request.getQueryParameters();
            if(params.containsKey("genre")){
                queryRequestOptions.setPartitionKey(new PartitionKey(params.get("genre")));
                params = removeFirst(params);
            }

            if(params.containsKey("limit")){
                limit = Integer.parseInt(params.get("limit"));
            }

            if(params.containsKey("stage")){
                stage = Integer.parseInt(params.get("stage"));
            }

            params.remove("limit");
            params.remove("stage");
            SqlQuerySpec specs = this.buildQuery(params);
            int count = QueryUtilities.getCount2(queryRequestOptions, container, specs);


            int maxStages = (count / limit);
            

            if (stage >= maxStages) {
                stage = rand.nextInt(maxStages+1);

            }



            if ((stage * limit) > (count / 2)) {
                offset = Utilities.calculateOppositeOffset(stage, count, limit);
                
                specs.getParameters().add(new SqlParameter("@offset", offset));
                specs.getParameters().add(new SqlParameter("@limit", limit));

    
                specs.setQueryText(specs.getQueryText() + QueryUtilities.ASC_ENDING);
            } else {
                offset = Utilities.calculateOffset(stage, count, limit);

                specs.getParameters().add(new SqlParameter("@offset", offset));
                specs.getParameters().add(new SqlParameter("@limit", limit));

            
                specs.setQueryText("SELECT DISTINCT {\"name\": c.name, \"artist\": c.artist} AS song FROM c" + specs.getQueryText() + QueryUtilities.DESC_ENDING);
    
            }
            
            List<JsonNode> songs = QueryUtilities.queryCollection(specs, container, queryRequestOptions);

            if(songs == null) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("BAD REQUEST").build();
            }
    
            try {
                return request.createResponseBuilder(HttpStatus.OK).body(objectMapper.writeValueAsString(songs)).build();
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal server error").build();
            }   
    }


    private SqlQuerySpec buildQuery(Map<String, String> params){
        List<SqlParameter> parameters = new ArrayList<>();
        String query = "";
        Object[] keys = params.keySet().toArray();
        for(int i = 0; i < params.size(); i++){
            String parameter = (String) keys[i];
            if(i != 0){
                query += " AND";
            } else {
                query += " WHERE";
            }
            if(parameter.equals("artist")){
                query += " c.artist = @artist";
                parameters.add(new SqlParameter("@artist", params.get(parameter)));
            } else if (parameter.equals("decade") || parameter.equals("century")){
                query += " c.time."+parameter+" = @"+parameter;
                parameters.add(new SqlParameter("@"+parameter, params.get(parameter)));
            } else if (parameter.equals("year")){
                query += " c.time."+parameter+" = @year";
                parameters.add(new SqlParameter("@"+parameter, Integer.parseInt(params.get(parameter))));
            
            } else if (parameter.equals("gender")){
                query += " c."+parameter+" = @gender";
                parameters.add(new SqlParameter("@"+parameter, params.get(parameter)));

            } else if (parameter.equals("audience")){
                query += " c."+parameter+" = @audience";
                parameters.add(new SqlParameter("@"+parameter, params.get(parameter)));
            }
            
        }
        SqlQuerySpec querySpec = new SqlQuerySpec(query, parameters);
        
        return querySpec;
    }

    private Map<String, String> removeFirst(Map<String, String> params) {
        Map<String, String> newMap = new HashMap<>();
        Object[] keys = params.keySet().toArray();
        for(int i = 1; i < params.size(); i++){
            newMap.put((String) keys[i], params.get(keys[i]));
        }
        return newMap;
    }
}