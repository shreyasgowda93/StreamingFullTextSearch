package task.definitions;

public class Types {

    public static class QueryID extends Number {
        private final int id;

        public QueryID(int id) {
            this.id = id;
        }

        @Override
        public int intValue() {
            return id;
        }

        @Override
        public long longValue() {
            return id;
        }

        @Override
        public float floatValue() {
            return id;
        }

        @Override
        public double doubleValue() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            QueryID queryID = (QueryID) obj;
            return id == queryID.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "QueryID{" + "id=" + id + '}';
        }
    }

    public static class DocID extends Number {
        private final int id;

        public DocID(int id) {
            this.id = id;
        }

        @Override
        public int intValue() {
            return id;
        }

        @Override
        public long longValue() {
            return id;
        }

        @Override
        public float floatValue() {
            return id;
        }

        @Override
        public double doubleValue() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            DocID docID = (DocID) obj;
            return id == docID.id;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(id);
        }

        @Override
        public String toString() {
            return "DocID{" + "id=" + id + '}';
        }
    }
}
