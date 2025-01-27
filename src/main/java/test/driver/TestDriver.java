package test.driver;

import task.implementation.TaskImplementation;
import task.definitions.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TestDriver {

    public void runTest(String testFile) {
        System.out.println("Start Test...");
        TaskImplementation task = new TaskImplementation();

        try (BufferedReader reader = new BufferedReader(new FileReader(testFile))) {
            long startTime = System.currentTimeMillis();

            // Initialize index
            ErrorCode initResult = task.initializeIndex();
            if (initResult != ErrorCode.EC_SUCCESS) {
                System.err.println("Failed to initialize index.");
                return;
            }

            Map<Integer, List<Integer>> expectedResults = new HashMap<>();
            boolean[] curResultsRet = new boolean[100];
            int firstResult = 0;
            int numCurResults = 0;

            String line;
            List<Integer> queryIds = null; // Shared variable for query IDs

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" ");
                char command = parts[0].charAt(0);
                int id = Integer.parseInt(parts[1]);

                if (numCurResults > 0 && (command == 's' || command == 'e')) {
                    processResults(task, curResultsRet, firstResult, numCurResults, expectedResults);
                    numCurResults = 0;
                }

                switch (command) {
                    case 's': // StartQuery
                        int matchType = Integer.parseInt(parts[2]);
                        int matchDist = Integer.parseInt(parts[3]);
                        String queryStr = String.join(" ", Arrays.copyOfRange(parts, 4, parts.length));
                        ErrorCode startResult = task.startQuery(new Types.QueryID(id), queryStr,
                                MatchType.values()[matchType], matchDist);
                        if (startResult != ErrorCode.EC_SUCCESS) {
                            System.err.println("StartQuery failed for Query ID: " + id);
                            return;
                        }
                        break;

                    case 'e': // EndQuery
                        ErrorCode endResult = task.endQuery(new Types.QueryID(id));
                        if (endResult != ErrorCode.EC_SUCCESS) {
                            System.err.println("EndQuery failed for Query ID: " + id);
                            return;
                        }
                        break;

                    case 'm': // Match Document
                        String docStr = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                        Types.DocID docId = new Types.DocID(id);
                        ErrorCode matchResult = task.matchDocument(docId, docStr);

                        if (matchResult != ErrorCode.EC_SUCCESS) {
                            System.err.println("MatchDocument failed for Document ID: " + id);
                            return;
                        }

                        // Retrieve matching queries from the resultsQueue
                        queryIds = new ArrayList<>();
                        Types.DocID[] tempDocId = new Types.DocID[1];
                        int[] numRes = new int[1];
                        Types.QueryID[][] queryIdsArray = new Types.QueryID[1][];

                        while (task.getNextAvailRes(tempDocId, numRes, queryIdsArray) == ErrorCode.EC_SUCCESS) {
                            if (tempDocId[0].intValue() == id) {
                                for (Types.QueryID queryId : queryIdsArray[0]) {
                                    queryIds.add(queryId.intValue());
                                }
                                break;
                            }
                        }

                        expectedResults.put(id, queryIds);
                        System.out.println("MatchDocument processed for DocID: " + id + ", docStr: " + docStr + ", Matching Queries: " + queryIds);
                        break;

                    case 'r': // Result Retrieval
                        int numResR = Integer.parseInt(parts[2]);
                        queryIds = new ArrayList<>(); // Reuse shared variable
                        for (int i = 3; i < parts.length; i++) {
                            queryIds.add(Integer.parseInt(parts[i]));
                        }
                        expectedResults.put(id, queryIds);
                        if (numCurResults == 0) firstResult = id;
                        curResultsRet[numCurResults] = false;
                        numCurResults++;
                        break;

                    default:
                        System.err.println("Unknown command: " + command);
                        return;
                }
            }

            long elapsedTime = System.currentTimeMillis() - startTime;

            // Destroy index
            ErrorCode destroyResult = task.destroyIndex();
            if (destroyResult != ErrorCode.EC_SUCCESS) {
                System.err.println("Failed to destroy index.");
                return;
            }

            System.out.println("Your program has successfully passed all tests.");
            printElapsedTime(elapsedTime);

        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }

    private void processResults(TaskImplementation task, boolean[] curResultsRet, int firstResult,
                                int numCurResults, Map<Integer, List<Integer>> expectedResults) {
        System.out.println("Processing results...");

        // Determine the valid range of DocIDs
        int minDocId = expectedResults.keySet().stream().min(Integer::compareTo).orElse(0);
        int maxDocId = expectedResults.keySet().stream().max(Integer::compareTo).orElse(0);

        System.out.println("Processing DocIDs in range: [" + minDocId + ", " + maxDocId + "]");

        curResultsRet = new boolean[maxDocId - minDocId + 1];
        boolean isQueueEmpty = false; // Track if the queue is empty

        while (!isQueueEmpty) {
            Types.DocID[] docIdOut = new Types.DocID[1];
            int[] numResOut = new int[1];
            Types.QueryID[][] queryIdsOut = new Types.QueryID[1][];

            ErrorCode resultCode = task.getNextAvailRes(docIdOut, numResOut, queryIdsOut);

            if (resultCode == ErrorCode.EC_NO_AVAIL_RES) {
                System.out.println("GetNextAvailRes: No available results. Ending result processing.");
                isQueueEmpty = true; // Mark the queue as empty and exit the loop
                break;
            } else if (resultCode != ErrorCode.EC_SUCCESS) {
                System.err.println("GetNextAvailRes returned an unknown error code.");
                return;
            }

            Types.DocID currentDocId = docIdOut[0];
            System.out.println("Processing Document ID: " + currentDocId);

            if (currentDocId == null || currentDocId.intValue() == 0) {
                System.err.println("Detected invalid DocID: " + currentDocId);
                return;
            }

            int docIdInt = currentDocId.intValue();
            if (docIdInt < minDocId || docIdInt > maxDocId) {
                System.err.println("DocID out of range: " + docIdInt + ". Valid range: [" + minDocId + ", " + maxDocId + "]");
                return;
            }

            int index = docIdInt - minDocId;
            if (curResultsRet[index]) {
                System.err.println("Detected duplicate DocID: " + docIdInt);
                return;
            }
            curResultsRet[index] = true;

            List<Integer> expectedQueryIds = expectedResults.get(docIdInt);
            if (expectedQueryIds == null || expectedQueryIds.size() != numResOut[0]) {
                System.err.println("GetNextAvailRes returned incorrect results for Document ID: " + docIdInt);
                System.err.println("Expected: " + expectedQueryIds + ", Found: " + Arrays.toString(queryIdsOut[0]));
                return;
            }

            System.out.println("Successfully processed Document ID: " + docIdInt + " with Query IDs: " + expectedQueryIds);
        }

        System.out.println("All results successfully processed.");
    }




    private void printElapsedTime(long elapsedTimeMillis) {
        long hours = elapsedTimeMillis / (1000 * 60 * 60);
        elapsedTimeMillis %= (1000 * 60 * 60);
        long minutes = elapsedTimeMillis / (1000 * 60);
        elapsedTimeMillis %= (1000 * 60);
        long seconds = elapsedTimeMillis / 1000;
        long millis = elapsedTimeMillis % 1000;

        System.out.printf("Elapsed Time: %dh:%dm:%ds:%dms%n", hours, minutes, seconds, millis);
    }
}