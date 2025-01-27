package task.implementation;

import java.util.*;
import java.util.concurrent.*;
import task.definitions.*;

public class TaskImplementation implements TaskInterface {


    private final ConcurrentMap<Types.QueryID, Query> activeQueries = new ConcurrentHashMap<>();
    private final ConcurrentLinkedQueue<DocumentResult> resultsQueue = new ConcurrentLinkedQueue<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    private final Trie queryTrie = new Trie();

    @Override
    public ErrorCode initializeIndex() {
        activeQueries.clear();
        resultsQueue.clear();
        queryTrie.clear();
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode destroyIndex() {
        activeQueries.clear();
        resultsQueue.clear();
        queryTrie.clear();
        executor.shutdown();
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode startQuery(Types.QueryID queryId, String queryStr, MatchType matchType, int matchDist) {
        Query query = new Query(queryId, queryStr, matchType, matchDist);
        activeQueries.put(queryId, query);
        queryTrie.insert(queryStr, query);
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode endQuery(Types.QueryID queryId) {
        Query removed = activeQueries.remove(queryId);
        if (removed != null) {
            queryTrie.remove(removed.str);
        }
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode matchDocument(Types.DocID docId, String docStr) {
        if (docId.intValue() == 0) return ErrorCode.EC_FAIL;

        // Submit a document matching task to the thread pool
        executor.submit(() -> {
            List<Types.QueryID> matchingQueries = new ArrayList<>();
            for (Query query : queryTrie.matchQueries(docStr)) {
                if (isQueryMatchingDocument(query, docStr)) {
                    matchingQueries.add(query.queryId);
                }
            }

            if (!matchingQueries.isEmpty()) {
                resultsQueue.offer(new DocumentResult(docId, matchingQueries));
            }
        });

        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode getNextAvailRes(Types.DocID[] pDocId, int[] pNumRes, Types.QueryID[][] pQueryIds) {
        DocumentResult result = resultsQueue.poll();
        if (result == null) return ErrorCode.EC_NO_AVAIL_RES;

        pDocId[0] = result.docId;
        pNumRes[0] = result.queryIds.size();
        pQueryIds[0] = result.queryIds.toArray(new Types.QueryID[0]);
        return ErrorCode.EC_SUCCESS;
    }

    private boolean isQueryMatchingDocument(Query query, String docStr) {
        String[] queryWords = query.str.split("\\s+");
        String[] docWords = docStr.split("\\s+");

        for (String queryWord : queryWords) {
            boolean wordMatches = Arrays.stream(docWords).anyMatch(docWord -> {
                System.out.println("Comparing query word: " + queryWord + " with doc word: " + docWord);
                switch (query.matchType) {
                    case MT_EXACT_MATCH:
                        return queryWord.equals(docWord);
                    case MT_HAMMING_DIST:
                        return queryWord.length() == docWord.length() &&
                                hammingDistance(queryWord, docWord) <= query.matchDist;
                    case MT_EDIT_DIST:
                        return editDistance(queryWord, docWord) <= query.matchDist;
                    default:
                        return false;
                }
            });
            if (!wordMatches) return false;
        }
        return true;
    }


    private int hammingDistance(String a, String b) {
        int distance = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) != b.charAt(i)) distance++;
        }
        return distance;
    }

    private int editDistance(String a, String b) {
        int[][] dp = new int[2][b.length() + 1];
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            dp[i % 2][0] = i;
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1)) {
                    dp[i % 2][j] = dp[(i - 1) % 2][j - 1];
                } else {
                    dp[i % 2][j] = Math.min(dp[(i - 1) % 2][j - 1],
                            Math.min(dp[(i - 1) % 2][j], dp[i % 2][j - 1])) + 1;
                }
            }
        }
        return dp[a.length() % 2][b.length()];
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

    private static class Trie {
        private final Node root = new Node();

        void insert(String word, Query query) {
            Node node = root;
            for (char c : word.toCharArray()) {
                node = node.children.computeIfAbsent(c, k -> new Node());
            }
            node.queries.add(query);
        }

        void remove(String word) {
            // Simplified; removal requires careful cleanup
        }

        List<Query> matchQueries(String docStr) {
            List<Query> queries = new ArrayList<>();
            for (String word : docStr.split("\\s+")) {
                Node node = root;
                for (char c : word.toCharArray()) {
                    node = node.children.get(c);
                    if (node == null) break;
                }
                if (node != null) queries.addAll(node.queries);
            }
            return queries;
        }

        void clear() {
            root.children.clear();
        }

        private static class Node {
            Map<Character, Node> children = new HashMap<>();
            List<Query> queries = new ArrayList<>();
        }
    }
}