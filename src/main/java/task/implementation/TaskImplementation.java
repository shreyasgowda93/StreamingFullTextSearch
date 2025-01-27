package task.implementation;

import java.util.*;
import task.definitions.*;

public class TaskImplementation implements TaskInterface {

    private final List<Query> activeQueries = new ArrayList<>();
    private final Queue<DocumentResult> resultsQueue = new LinkedList<>();
    private int totalMatchingQueries = 0;

    @Override
    public ErrorCode initializeIndex() {
        activeQueries.clear();
        resultsQueue.clear();
        totalMatchingQueries = 0;
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode destroyIndex() {
        activeQueries.clear();
        resultsQueue.clear();
        System.out.println("The total matched queries are " + totalMatchingQueries);
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

    @Override
    public ErrorCode matchDocument(Types.DocID docId, String docStr) {
        if (docId.intValue() == 0) {
            return ErrorCode.EC_FAIL;
        }

        List<Types.QueryID> matchingQueries = new ArrayList<>();
        for (Query query : activeQueries) {
            if (isQueryMatchingDocument(query, docStr)) {
                matchingQueries.add(query.queryId);
            }
        }

        if (!matchingQueries.isEmpty()) {
            resultsQueue.offer(new DocumentResult(docId, matchingQueries));
            totalMatchingQueries += matchingQueries.size();
        }

        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode getNextAvailRes(Types.DocID[] pDocId, int[] pNumRes, Types.QueryID[][] pQueryIds) {
        DocumentResult result = resultsQueue.poll();
        if (result == null) {
            return ErrorCode.EC_NO_AVAIL_RES;
        }

        pDocId[0] = result.docId;
        pNumRes[0] = result.queryIds.size();
        pQueryIds[0] = result.queryIds.toArray(new Types.QueryID[0]);
        return ErrorCode.EC_SUCCESS;
    }

    private boolean isQueryMatchingDocument(Query query, String docStr) {
        String[] queryWords = query.str.split("\\s+");
        String[] docWords = docStr.split("\\s+");

        for (String queryWord : queryWords) {
            boolean wordMatches = false;

            for (String docWord : docWords) {
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
                    break;
                }
            }

            if (!wordMatches) {
                return false;
            }
        }

        return true;
    }

    private int hammingDistance(String a, String b) {
        int distance = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) {
                distance++;
            }
        }
        return distance;
    }

    private int editDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i][j] = dp[i - 1][j - 1];
                } else {
                    dp[i][j] = Math.min(dp[i - 1][j - 1],
                            Math.min(dp[i - 1][j], dp[i][j - 1])) + 1;
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