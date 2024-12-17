package task.implementation;

import java.util.*;
import task.definitions.Constants;
import task.definitions.ErrorCode;
import task.definitions.MatchType;
import task.definitions.Types;
import task.definitions.TaskInterface;


public class TaskImplementation implements TaskInterface {


    @Override
    public ErrorCode initializeIndex() {
        // Initialization logic here
        System.out.println("initializeIndex method is running!");
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode destroyIndex() {
        // Cleanup logic here
        System.out.println("destroyIndex method is running!");
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode startQuery(Types.QueryID queryId, String queryStr, MatchType matchType, int matchDist) {
        // Add query logic here
        System.out.println("startQuery method is running!");
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode endQuery(Types.QueryID queryId) {
        // Remove query logic here
        System.out.println("This method is running!");
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode matchDocument(Types.DocID docId, String docStr) {
        // Document matching logic here
        System.out.println("endQuery method is running!");
        return ErrorCode.EC_SUCCESS;
    }

    @Override
    public ErrorCode getNextAvailRes(Types.DocID[] pDocId, int[] pNumRes, Types.QueryID[][] pQueryIds) {
        // Retrieve results logic here
        System.out.println("getNextAvailRes method is running!");
        return ErrorCode.EC_NO_AVAIL_RES; // or EC_SUCCESS if results are available.
    }
}

