package task.implementation;

import java.util.*;
import task.definitions.*;

public class TaskImplementation implements TaskInterface {

    private final List<Query> activeQueries = new ArrayList<>();
    private final Queue<DocumentResult> resultsQueue = new LinkedList<>();

    @Override
    public ErrorCode initializeIndex() {
        activeQueries.clear();
        resultsQueue.clear();
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode destroyIndex() {
        activeQueries.clear();
        resultsQueue.clear();
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode startQuery(Types.QueryID queryId, String queryStr, MatchType matchType, int matchDist) {
        Query query = new Query(queryId, queryStr, matchType, matchDist);
        activeQueries.add(query);
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode endQuery(Types.QueryID queryId) {
        activeQueries.removeIf(query -> query.queryId.equals(queryId));
        return ErrorCode.EC_SUCCESS;
    }

    private void logResultsQueueState(String action) {
        System.out.println("Action: " + action);
        System.out.println("Current state of resultsQueue:");

        for (DocumentResult result : resultsQueue) {
            if (result.docId.intValue() == 0) {
                System.out.println("  Invalid DocID detected in queue: " + result.docId);
            } else {
                System.out.println("  DocID: " + result.docId + ", Queries: " + result.queryIds);
            }
        }
    }



    @Override
    public ErrorCode matchDocument(Types.DocID docId, String docStr) {
        System.out.println("matchDocument called with DocID: " + docId + ", docStr: " + docStr);

        // Validate DocID
        if (docId.intValue() == 0) {
            System.out.println("Error: Attempted to add invalid DocID: " + docId);
            return ErrorCode.EC_FAIL;
        }

        List<Types.QueryID> matchingQueries = new ArrayList<>();
        for (Query query : activeQueries) {
            boolean isMatch = isQueryMatchingDocument(query, docStr);

            if (isMatch) {
                System.out.println("Query matched: " + query.queryId);
                matchingQueries.add(query.queryId);
            } else {
                System.out.println("Query did not match: " + query.queryId);
            }
        }

        if (!matchingQueries.isEmpty()) {
            System.out.println("Adding DocID to resultsQueue: " + docId);
            resultsQueue.add(new DocumentResult(docId, matchingQueries));
        } else {
            System.out.println("No matches for document: " + docId);
        }

        logResultsQueueState("After adding to resultsQueue in matchDocument");
        return ErrorCode.EC_SUCCESS;
    }


    private boolean isQueryMatchingDocument(Query query, String docStr) {
        System.out.println("Matching query: " + query.str + " against document: " + docStr);

        String[] queryWords = query.str.split("\\s+");
        String[] docWords = docStr.split("\\s+");

        for (String queryWord : queryWords) {
            boolean wordMatches = false;

            for (String docWord : docWords) {
                System.out.println("Comparing query word: " + queryWord + " with doc word: " + docWord);

                switch (query.matchType) {
                    case MT_EXACT_MATCH:
                        if (queryWord.equals(docWord)) {
                            wordMatches = true;
                        }
                        break;

                    case MT_HAMMING_DIST:
                        if (queryWord.length() == docWord.length() &&
                                hammingDistance(queryWord, docWord) <= query.matchDist) {
                            wordMatches = true;
                        }
                        break;

                    case MT_EDIT_DIST:
                        if (editDistance(queryWord, docWord) <= query.matchDist) {
                            wordMatches = true;
                        }
                        break;
                }

                if (wordMatches) {
                    System.out.println("Match found: " + queryWord + " -> " + docWord);
                    break;
                }
            }

            if (!wordMatches) {
                System.out.println("No match found for query word: " + queryWord);
                return false;
            }
        }

        System.out.println("Query matches document.");
        return true;
    }

    @Override
    public ErrorCode getNextAvailRes(Types.DocID[] pDocId, int[] pNumRes, Types.QueryID[][] pQueryIds) {
        System.out.println("Polling from resultsQueue...");

        while (!resultsQueue.isEmpty()) {
            DocumentResult result = resultsQueue.poll();

            // Validate the polled result
            if (result == null || result.docId == null || result.docId.intValue() == 0) {
                System.err.println("Invalid DocID retrieved: " + (result != null ? result.docId : "null"));
                continue; // Skip invalid result
            }

            // Populate output parameters
            pDocId[0] = result.docId;
            pNumRes[0] = result.queryIds.size();
            pQueryIds[0] = result.queryIds.toArray(new Types.QueryID[0]);

            System.out.println("Returning result for document: " + result.docId + " with queries: " + result.queryIds);
            return ErrorCode.EC_SUCCESS;
        }

        System.out.println("GetNextAvailRes: No available results.");
        return ErrorCode.EC_NO_AVAIL_RES;
    }



    private int hammingDistance(String a, String b) {
        int distance = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) distance++;
        }
        return distance;
    }

    private int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1])) + 1;
                }
            }
        }

        return dp[a.length()][b.length()];
    }

    private static class Query {
        Types.QueryID queryId;
        String str;
        MatchType matchType;
        int matchDist;

        Query(Types.QueryID queryId, String str, MatchType matchType, int matchDist) {
            this.queryId = queryId;
            this.str = str;
            this.matchType = matchType;
            this.matchDist = matchDist;
        }
    }

    private static class DocumentResult {
        Types.DocID docId;
        List<Types.QueryID> queryIds;

        DocumentResult(Types.DocID docId, List<Types.QueryID> queryIds) {
            this.docId = docId;
            this.queryIds = queryIds;
        }
    }
}
