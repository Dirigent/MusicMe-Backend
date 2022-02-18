package com.function;

import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosDatabase;
import com.azure.cosmos.implementation.Utils;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
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
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class TimeSongRetriever {
        Random rand = new Random();
        private CosmosClient client = ClientSingleton.getInstance().getClient();
        private   CosmosDatabase cosmosDatabase = client.getDatabase("Songs");
        private CosmosContainer container = cosmosDatabase.getContainer("songs");
        private final String queryDecadeDesc = "SELECT DISTINCT {\"name\" : c.name, \"artist\" : c.artist} AS song FROM c WHERE  c.time.decade = @decade ORDER BY c.pi DESC OFFSET @offset LIMIT @limit";
        private final String queryYearDesc = "SELECT DISTINCT {\"name\" : c.name, \"artist\" : c.artist} AS song FROM c WHERE  c.time.year = @year ORDER BY c.pi DESC OFFSET @offset LIMIT @limit";
        private final String queryDecadeAsc = "SELECT DISTINCT {\"name\" : c.name, \"artist\" : c.artist} AS song FROM c WHERE  c.time.decade = @decade ORDER BY c.pi ASC OFFSET @offset LIMIT @limit";
        private final String queryYearAsc = "SELECT DISTINCT {\"name\" : c.name, \"artist\" : c.artist} AS song FROM c WHERE  c.time.year = @year ORDER BY c.pi ASC OFFSET @offset LIMIT @limit";

    @FunctionName("time")
    public HttpResponseMessage run(@HttpTrigger(name = "timeReq",
     methods = {
        HttpMethod.GET },
         authLevel = AuthorizationLevel.ANONYMOUS,
          route = "time/type/{type}/time/{time}/limit/{limit}/level/{level}")
         HttpRequestMessage<Optional<String>> request,
            @BindingName("time") String time,
            @BindingName("limit") int limit,
            @BindingName("level") int level,
            @BindingName("type") String type,
            final ExecutionContext context) {
                ObjectMapper objectMapper = Utils.getSimpleObjectMapper();
                SqlQuerySpec countQuerySpec;
                int count;
                Object value;
                String param;
                String query1;
                String query2;
                
                CosmosQueryRequestOptions queryRequestOptions = new CosmosQueryRequestOptions();
            
                if(type.equals("year")){
                    SqlParameter[] parameters = {new SqlParameter("@year", Integer.parseInt(time))};
                    countQuerySpec = new SqlQuerySpec("SELECT VALUE COUNT(1) FROM c WHERE c.time.year = @year", parameters);
                    param = "@year";
                    value = Integer.parseInt(time);
                    query1 = queryYearDesc;
                    query2 = queryYearAsc;
                } else {
                    SqlParameter[] parameters = {new SqlParameter("@decade", time)};
                    countQuerySpec = new SqlQuerySpec("SELECT VALUE COUNT(1) FROM c WHERE c.time.decade = @decade", parameters);
                    param = "@decade";
                    value = time;
                    query1 = queryDecadeDesc;
                    query2 = queryDecadeAsc;
                }

                count = this.getCount(new CosmosQueryRequestOptions(), container, countQuerySpec);
                int maxLevel = count/27;
                if(level > maxLevel) {
                    level = rand.nextInt(maxLevel+1);
                }

                List<JsonNode> songs= this.getSongs(maxLevel, limit, level, count, queryRequestOptions, param, container, query1, query2, value);

                

                if(songs.size() > 0) {
                    try {
                        return request.createResponseBuilder(HttpStatus.OK).body(objectMapper.writeValueAsString(songs)).build();
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                        return request.createResponseBuilder(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to parse json").build();
            
                    }  
                } else {
                    return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Something Went Wrong").build();
                }
        }

        private SqlQuerySpec buildQuerySpecs(int offset, int limit, String query, String param, Object value) {
            SqlParameter[] parameters = {new SqlParameter("@offset", offset), new SqlParameter("@limit", limit), new SqlParameter(param, value)};
            
    
            SqlQuerySpec querySpec = new SqlQuerySpec(query, parameters);
            return querySpec;
    
        }

        private int getCount(CosmosQueryRequestOptions requestOptions, CosmosContainer container, SqlQuerySpec querySpec) {
            int count = 0;
            CosmosPagedIterable<JsonNode> pagedIterable = container.queryItems(querySpec, requestOptions,JsonNode.class);
    
    
                for (FeedResponse<JsonNode> pageResponse : pagedIterable.iterableByPage()) {
                    for (JsonNode item : pageResponse.getElements()) {
                        count = item.asInt();
                    }
                }
    
            return count;
    
        }

        private List<JsonNode> getSongs(int numOfLevels, int limit, int level, int count, CosmosQueryRequestOptions requestOptions, String param, CosmosContainer container, String query1, String query2, Object value){
            Double proportionHard = ((double)level/(double)numOfLevels);
            Double proportionEasy = 1-proportionHard;  
            int[] limits = QueryUtilities.calculateLimit(proportionHard, proportionEasy, limit);
    
            int hardLimit = limits[0];
            int easyLimit = limits[1];
            List<JsonNode> songs = new ArrayList<JsonNode>();
    
            if (level > numOfLevels / 5) {
                if (proportionEasy > 0.0) {
                    songs.addAll(QueryUtilities.queryCollection(
                            this.buildQuerySpecs(Utilities.calculateOffset(level, count / 2, limit), easyLimit,
                                    query1,param, value),
                            container, requestOptions));
                }
                if (proportionHard > 0.0) {
                    songs.addAll(QueryUtilities.queryCollection(
                            this.buildQuerySpecs(Utilities.calculateOppositeOffset(level, count / 2, limit), hardLimit,
                                    query2, param, value),
                            container, requestOptions));
                }
            } else {

                songs.addAll(QueryUtilities
                        .queryCollection(this.buildQuerySpecs(Utilities.calculateOffset(level, count / 2, limit), easyLimit,
                        query1,param, value),
                        container, requestOptions));
            }
    
            return songs;
    
        }
    
}
    