package com.function.musicme.songfunctions;

import com.azure.cosmos.CosmosContainer;

import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.FeedResponse;
import com.azure.cosmos.models.SqlParameter;
import com.azure.cosmos.models.SqlQuerySpec;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class QueryUtilities {
    //Prepared statements
    public static final String ASC_QUERY = "SELECT DISTINCT {\"name\": c.name, \"artist\": c.artist} AS song FROM c ORDER BY c.pi ASC OFFSET @offset LIMIT @limit";
    public static final String DESC_QUERY = "SELECT DISTINCT {\"name\": c.name , \"artist\": c.artist} AS song FROM c ORDER BY c.pi DESC OFFSET @offset LIMIT @limit";
    public static final String BASE_READ_QUERY = "SELECT DISTINCT {\"name\": c.name , \"artist\": c.artist} AS song FROM c";
    public static final String DESC_ENDING = " ORDER BY c.pi DESC OFFSET @offset LIMIT @limit";
    public static final String ASC_ENDING = " ORDER BY c.pi ASC OFFSET @offset LIMIT @limit";
    public static final String QUERY_START = " c.";
    public static final String QUERY_START_TIME = " c.time.";
    public static final String QUERY_PARAM_ARTIST = "artist";
    public static final String QUERY_PARAM_DECADE = "decade";
    public static final String QUERY_PARAM_CENTURY = "century";
    public static final String QUERY_PARAM_YEAR = "year";
    public static final String QUERY_PARAM_GENDER = "gender";
    public static final String QUERY_PARAM_AUDIENCE = "audience";
    public static final String QUERY_PARAM_STAGE = "stage";
    public static final String QUERY_PARAM_LIMIT = "limit";

    //Parts of the code are used under the MIT license from https://github.com/Azure-Samples/azure-cosmos-java-sql-api-samples

    //Queries a collectionn and processes the response to a List
    public static List<JsonNode> queryCollection(SqlQuerySpec querySpec, CosmosContainer container, CosmosQueryRequestOptions requestOptions){
        List<JsonNode> songs = new ArrayList<JsonNode>();
        requestOptions.setMaxBufferedItemCount(8);

        CosmosPagedIterable<JsonNode> pagedIterable = container.queryItems(querySpec, requestOptions,
            JsonNode.class);
        for (FeedResponse<JsonNode> pageResponse : pagedIterable.iterableByPage()) {
            for (JsonNode item : pageResponse.getElements()) {
                songs.add(item);
            }
        }

        return songs;
    }

    //Creates Query specs (Ads query string and params to SqlQuerySpec class)
    public static SqlQuerySpec buildQuerySpecs(int offset, int limit, String query) {
        SqlParameter[] parameters = { new SqlParameter("@offset", offset), new SqlParameter("@limit", limit) };

        SqlQuerySpec querySpec = new SqlQuerySpec(query, parameters);
        return querySpec;

    }

    //Finds count of query so that level calculation can be performed later
    public static int getCount(CosmosQueryRequestOptions requestOptions, CosmosContainer container, SqlQuerySpec specs){
        int count = 0;
        SqlQuerySpec newSpec = new SqlQuerySpec(specs.getQueryText(), specs.getParameters());
        newSpec.setQueryText("SELECT VALUE COUNT(1) FROM c" + specs.getQueryText());

        CosmosPagedIterable<JsonNode> pagedIterable = container.queryItems(newSpec, requestOptions,JsonNode.class);
            for (FeedResponse<JsonNode> pageResponse : pagedIterable.iterableByPage()) {
                for (JsonNode item : pageResponse.getElements()) {
                    count = item.asInt();
                }
            }

        return count;

    }
    //Retrieves Songs from database. To determine which songs too select proportion of Easy songs (high popularity index and hard songs (low popularity index) are calculated) 
    public static List<JsonNode> getSongs(int numOfLevels, int limit, int level, int count, CosmosQueryRequestOptions requestOptions, CosmosContainer container, String query1, String query2){
        Double proportionHard = ((double)level/(double)numOfLevels);
        Double proportionEasy = 1-proportionHard;  
        int[] limits = calculateLimit(proportionHard, proportionEasy, limit);

        int hardLimit = limits[0];
        int easyLimit = limits[1];
        List<JsonNode> songs = new ArrayList<JsonNode>();

        if (level > numOfLevels / 5) {
            if (proportionEasy > 0.0) {
                songs.addAll(QueryUtilities.queryCollection(
                        QueryUtilities.buildQuerySpecs(Utilities.calculateOffset(level, count / 2, limit), easyLimit,
                                query1),
                        container, requestOptions));
            }
            if (proportionHard > 0.0) {
                songs.addAll(QueryUtilities.queryCollection(
                        QueryUtilities.buildQuerySpecs(Utilities.calculateOppositeOffset(level, count / 2, limit),
                                hardLimit, query2),
                        container, requestOptions));
            }
        } else {
            songs.addAll(QueryUtilities
                    .queryCollection(QueryUtilities.buildQuerySpecs(Utilities.calculateOffset(level, count / 2, limit),
                            limit, QueryUtilities.DESC_QUERY), container, requestOptions));
        }

        return songs;

    }
    //caluclates how many easy and hard songs are to be retrieved
    public static int[] calculateLimit(Double proportionHard, Double proportionEasy, int limit) {
        int[] limits = new int[2];
        
        if(proportionHard > 1) {
            proportionHard = 1.0;
            proportionEasy = 0.0;
        }
        
        int hardLimit = (int)(limit*proportionHard);
        int easyLimit = limit - hardLimit; 

        limits[0] = hardLimit;
        limits[1] = easyLimit;
        return limits;
    }
}