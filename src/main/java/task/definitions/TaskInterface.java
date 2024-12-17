package task.definitions;

public interface TaskInterface {
    ErrorCode initializeIndex();
    ErrorCode destroyIndex();
    ErrorCode startQuery(Types.QueryID queryId, String queryStr, MatchType matchType, int matchDist);
    ErrorCode endQuery(Types.QueryID queryId);
    ErrorCode matchDocument(Types.DocID docId, String docStr);
    ErrorCode getNextAvailRes(Types.DocID[] pDocId, int[] pNumRes, Types.QueryID[][] pQueryIds);
}

