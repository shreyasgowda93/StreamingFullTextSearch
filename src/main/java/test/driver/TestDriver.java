package test.driver;

import task.implementation.TaskImplementation;
import task.definitions.*;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.*;
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

                    case 'm': // MatchDocument
                        String docStr = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                        ErrorCode matchResult = task.matchDocument(new Types.DocID(id), docStr);
                        if (matchResult != ErrorCode.EC_SUCCESS) {
                            System.err.println("MatchDocument failed for Document ID: " + id);
                            return;
                        }
                        break;

                    case 'r': // Result Retrieval
                        int numRes = Integer.parseInt(parts[2]);
                        List<Integer> queryIds = new ArrayList<>();
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
        for (int i = 0; i < numCurResults; i++) {
            Types.DocID docIdOut = new Types.DocID(0);
            int[] numResOut = new int[1];
            Types.QueryID[][] queryIdsOut = new Types.QueryID[1][];

            ErrorCode resultCode = task.getNextAvailRes(new Types.DocID[]{docIdOut}, numResOut, queryIdsOut);

            if (resultCode == ErrorCode.EC_NO_AVAIL_RES) {
                System.err.println("GetNextAvailRes returned EC_NO_AVAIL_RES but there are undelivered results.");
                return;
            } else if (resultCode != ErrorCode.EC_SUCCESS) {
                System.err.println("GetNextAvailRes returned an unknown error code.");
                return;
            }

            int docIdInt = docIdOut.intValue();
            if (docIdInt < firstResult || docIdInt >= firstResult + numCurResults || curResultsRet[docIdInt - firstResult]) {
                System.err.println("GetNextAvailRes returned an invalid or duplicate document ID: " + docIdInt);
                return;
            }

            List<Integer> expectedQueryIds = expectedResults.get(docIdInt);
            if (expectedQueryIds == null || expectedQueryIds.size() != numResOut[0]) {
                System.err.println("GetNextAvailRes returned incorrect results for Document ID: " + docIdInt);
                return;
            }

            curResultsRet[docIdInt - firstResult] = true;
        }
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



