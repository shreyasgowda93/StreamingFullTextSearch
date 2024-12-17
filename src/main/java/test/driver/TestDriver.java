package test.driver;

import task.implementation.TaskImplementation;
import task.definitions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

class TestDriver {

    public void readFile() throws IOException {
        // Obtain an InputStream for the resource file
        final InputStream stream = this.getClass().getResourceAsStream("/small_test.txt");
        assert stream != null;
        final InputStreamReader reader = new InputStreamReader(stream);
        final BufferedReader buffered = new BufferedReader(reader);
        String line = buffered.readLine();
        System.out.println("The txt file contains the following data:");
        System.out.println(line);
        for (int i = 0; i < 5; i++) {
            System.out.println(buffered.readLine());
        }
    }


    // A method to test TaskImplementation methods
    public void runTests() {
        // Create an instance of TaskImplementation
        TaskImplementation taskImplementation = new TaskImplementation();

        // Call initializeIndex
        System.out.println("Calling initializeIndex...");
        taskImplementation.initializeIndex();

        // Call startQuery
        System.out.println("Calling startQuery...");
        Types.QueryID queryId1 = new Types.QueryID(1);
        String queryStr1 = "hello";
        MatchType matchType1 = MatchType.MT_EXACT_MATCH;
        int matchDist1 = 0;
        taskImplementation.startQuery(queryId1, queryStr1, matchType1, matchDist1);

        // Call endQuery
        System.out.println("Calling endQuery...");
        taskImplementation.endQuery(queryId1);

        // Call matchDocument
        System.out.println("Calling matchDocument...");
        Types.DocID docId1 = new Types.DocID(101);
        String docStr1 = "hello world";
        taskImplementation.matchDocument(docId1, docStr1);

        // Call getNextAvailRes
        System.out.println("Calling getNextAvailRes...");
        Types.DocID[] pDocId = new Types.DocID[1];
        int[] pNumRes = new int[1];
        Types.QueryID[][] pQueryIds = new Types.QueryID[1][];
        taskImplementation.getNextAvailRes(pDocId, pNumRes, pQueryIds);

        // Call destroyIndex
        System.out.println("Calling destroyIndex...");
        taskImplementation.destroyIndex();
    }
}


