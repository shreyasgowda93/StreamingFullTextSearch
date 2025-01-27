package test.driver;

public class Main {

    public static void main(String[] args)  {
        // Create an instance of TestDriver and run tests
        String testFile = (args.length > 0) ? args[0] : "src/main/resources/small_test.txt";
        TestDriver driver = new TestDriver();
        driver.runTest(testFile);
    }
}