package test.driver;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        // Create an instance of TestDriver and run tests
        TestDriver testDriver = new TestDriver();
        testDriver.readFile(); // Read the data file
        testDriver.runTests(); // Run the core functions
    }
}
