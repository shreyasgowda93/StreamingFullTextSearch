package task.definitions;

public class Types {
    public static class QueryID extends Number {
        private final int id;

        public QueryID(int id) {
            this.id = id;
        }

        @Override
        public int intValue() { return id; }
        @Override
        public long longValue() { return id; }
        @Override
        public float floatValue() { return id; }
        @Override
        public double doubleValue() { return id; }
    }

    public static class DocID extends Number {
        private final int id;

        public DocID(int id) {
            this.id = id;
        }

        @Override
        public int intValue() { return id; }
        @Override
        public long longValue() { return id; }
        @Override
        public float floatValue() { return id; }
        @Override
        public double doubleValue() { return id; }
    }
}

