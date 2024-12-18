package task.implementation;

import java.util.*;
import task.definitions.Constants;
import task.definitions.ErrorCode;
import task.definitions.MatchType;
import task.definitions.Types;
import task.definitions.TaskInterface;

public class TaskImplementation implements TaskInterface {

    // Data structures to hold queries and results
    private Map<Types.QueryID, Query> activeQueries;
    private Map<Types.DocID, List<Types.QueryID>> matchingResults;

    public TaskImplementation() {
        activeQueries = new HashMap<>();
        matchingResults = new HashMap<>();
    }

    // Inner class to represent a query
    private static class Query {
        String queryStr;
        MatchType matchType;
        int matchDist;

        Query(String queryStr, MatchType matchType, int matchDist) {
            this.queryStr = queryStr;
            this.matchType = matchType;
            this.matchDist = matchDist;
        }
    }

    @Override
    public ErrorCode initializeIndex() {
        // Initialize data structures
        activeQueries.clear();
        matchingResults.clear();
        System.out.println("Index initialized.");
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode destroyIndex() {
        // Clear data structures
        activeQueries.clear();
        matchingResults.clear();
        System.out.println("Index destroyed.");
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode startQuery(Types.QueryID queryId, String queryStr, MatchType matchType, int matchDist) {
        if (activeQueries.containsKey(queryId)) {
            System.out.println("Query already exists: " + queryId);
            return ErrorCode.EC_FAIL;
        }

        // Add the query to active queries
        activeQueries.put(queryId, new Query(queryStr, matchType, matchDist));
        System.out.println("Query started: " + queryId);
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode endQuery(Types.QueryID queryId) {
        if (!activeQueries.containsKey(queryId)) {
            System.out.println("Query does not exist: " + queryId);
            return ErrorCode.EC_FAIL;
        }

        // Remove the query from active queries
        activeQueries.remove(queryId);
        System.out.println("Query ended: " + queryId);
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode matchDocument(Types.DocID docId, String docStr) {
        List<Types.QueryID> matchedQueries = new ArrayList<>();

        for (Map.Entry<Types.QueryID, Query> entry : activeQueries.entrySet()) {
            Types.QueryID queryId = entry.getKey();
            Query query = entry.getValue();

            if (matches(docStr, query)) {
                matchedQueries.add(queryId);
            }
        }

        if (!matchedQueries.isEmpty()) {
            matchingResults.put(docId, matchedQueries);
            System.out.println("Document matched: " + docId);
            return ErrorCode.EC_SUCCESS;
        } else {
            System.out.println("No matches for document: " + docId);
            return ErrorCode.EC_NO_AVAIL_RES;
        }
    }

    @Override
    public ErrorCode getNextAvailRes(Types.DocID[] pDocId, int[] pNumRes, Types.QueryID[][] pQueryIds) {
        for (Map.Entry<Types.DocID, List<Types.QueryID>> entry : matchingResults.entrySet()) {
            pDocId[0] = entry.getKey();
            List<Types.QueryID> queries = entry.getValue();

            pNumRes[0] = queries.size();
            pQueryIds[0] = queries.toArray(new Types.QueryID[0]);

            // Remove the result after returning
            matchingResults.remove(entry.getKey());
            System.out.println("Returning results for document: " + pDocId[0]);
            return ErrorCode.EC_SUCCESS;
        }

        System.out.println("No available results.");
        return ErrorCode.EC_NO_AVAIL_RES;
    }

    // Helper method to check if a document matches a query
    private boolean matches(String docStr, Query query) {
        switch (query.matchType) {
            case MT_EXACT_MATCH:
                return docStr.contains(query.queryStr);

            case MT_HAMMING_DIST:
                return hammingDistance(docStr, query.queryStr) <= query.matchDist;

            case MT_EDIT_DIST:
                return editDistance(docStr, query.queryStr) <= query.matchDist;

            default:
                return false;
        }
    }

    // Helper method to compute Hamming distance
    private int hammingDistance(String str1, String str2) {
        if (str1.length() != str2.length()) return Integer.MAX_VALUE;
        int distance = 0;
        for (int i = 0; i < str1.length(); i++) {
            if (str1.charAt(i) != str2.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

    // Helper method to compute Edit distance (Levenshtein distance)
    private int editDistance(String str1, String str2) {
        int[][] dp = new int[str1.length() + 1][str2.length() + 1];

        for (int i = 0; i <= str1.length(); i++) {
            for (int j = 0; j <= str2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else if (str1.charAt(i - 1) == str2.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
                }
            }
        }

        return dp[str1.length()][str2.length()];
    }
}
