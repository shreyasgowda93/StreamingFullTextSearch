package task.implementation;

import java.util.*;
import task.definitions.Constants;
import task.definitions.ErrorCode;
import task.definitions.MatchType;
import task.definitions.Types;
import task.definitions.TaskInterface;


public class TaskImplementation implements TaskInterface {

    // List to store active queries
    List<Query> activeQueries;

    // List to store matching results
    List<MatchResult> matchingResults;

    // Inner class to represent a query
    private static class Query {
        Types.QueryID queryId;
        String queryStr;
        MatchType matchType;
        int matchDist;


        // Constructor
        public Query(Types.QueryID queryId, String queryStr, MatchType matchType, int matchDist) {
            this.queryId = queryId;
            this.queryStr = queryStr;
            this.matchType = matchType;
            this.matchDist = matchDist;
        }
    }

    // Inner class to store match results for documents
    private static class MatchResult {
        Types.DocID docId;
        List<Types.QueryID> matchedQueries;

        MatchResult(Types.DocID docId, List<Types.QueryID> matchedQueries) {
            this.docId = docId;
            this.matchedQueries = matchedQueries;
        }
    }

    @Override
    public ErrorCode initializeIndex() {
        if (activeQueries == null) {
            activeQueries = new ArrayList<>();
        }
        if (matchingResults == null) {
            matchingResults = new ArrayList<>();
        }
        System.out.println("Index initialized.");
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode destroyIndex() {
        activeQueries.clear();
        matchingResults.clear();
        System.out.println("Index destroyed.");
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode startQuery(Types.QueryID queryId, String queryStr, MatchType matchType, int matchDist) {
        // Check if the query already exists in the list
        for (Query query : activeQueries) {
            if (query.queryId.equals(queryId)) {
                System.out.println("Query already exists: " + queryId);
                return ErrorCode.EC_FAIL;
            }
        }
        // Add the new query to the list
        activeQueries.add(new Query(queryId, queryStr, matchType, matchDist));
        System.out.println("Query started: " + queryId);
        return ErrorCode.EC_SUCCESS;
    }


    @Override
    public ErrorCode endQuery(Types.QueryID queryId) {
        // Find and remove the query from the list
        activeQueries.removeIf(query -> query.queryId.equals(queryId));
        System.out.println("Query ended: " + queryId);
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode matchDocument(Types.DocID docId, String docStr) {
        List<Types.QueryID> matchedQueries = new ArrayList<>();

        // Iterate through the active queries and check for matches
        for (Query query : activeQueries) {
            if (matches(docStr, query)) {
                matchedQueries.add(query.queryId);
            }
        }

        if (!matchedQueries.isEmpty()) {
            // Store the result in matchingResults
            matchingResults.add(new MatchResult(docId, matchedQueries));
            System.out.println("Document matched: " + docId);
            return ErrorCode.EC_SUCCESS;
        } else {
            System.out.println("No matches for document: " + docId);
            return ErrorCode.EC_NO_AVAIL_RES;
        }
    }

    @Override
    public ErrorCode getNextAvailRes(Types.DocID[] pDocId, int[] pNumRes, Types.QueryID[][] pQueryIds) {
        if (!matchingResults.isEmpty()) {
            // Get the first matching result and return it
            MatchResult result = matchingResults.remove(0);
            pDocId[0] = result.docId;
            pNumRes[0] = result.matchedQueries.size();
            pQueryIds[0] = result.matchedQueries.toArray(new Types.QueryID[0]);

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