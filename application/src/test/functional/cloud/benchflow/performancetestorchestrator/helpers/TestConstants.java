package cloud.benchflow.performancetestorchestrator.helpers;

import cloud.benchflow.performancetestorchestrator.models.User;

import static cloud.benchflow.performancetestorchestrator.constants.BenchFlowConstants.MODEL_ID_DELIMITER;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 22.02.17.
 */
public class TestConstants {

    public static final String INVALID_PERFORMANCE_TEST_NAME = "invalid";
    public static final String VALID_PERFORMANCE_TEST_NAME = "testNameExample";

    public static final String PERFORMANCE_EXPERIMENT_ID = "benchflow.testNameExample.1.1";

    public static String TEST_USER_NAME = "testUser";
    public static User TEST_USER = new User(TEST_USER_NAME);


    public static String VALID_TEST_ID = TEST_USER_NAME + MODEL_ID_DELIMITER + VALID_PERFORMANCE_TEST_NAME + MODEL_ID_DELIMITER + 1;
    public static String VALID_EXPERIMENT_ID = VALID_TEST_ID + MODEL_ID_DELIMITER + 1;
}