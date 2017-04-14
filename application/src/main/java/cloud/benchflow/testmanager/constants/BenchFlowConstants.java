package cloud.benchflow.testmanager.constants;

import cloud.benchflow.testmanager.models.User;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 16.02.17.
 */
public class BenchFlowConstants {

    // TODO - this is necessary for current version of Test Manager
    private static final String YAML_EXTENSION = ".yml";

    // Archive
    public static final String DEPLOYMENT_DESCRIPTOR_NAME = "docker-compose";
    public static final String TEST_EXPERIMENT_DEFINITION_NAME = "benchflow-test";
    public static final String BPMN_MODELS_FOLDER_NAME = "models";
    public static final String MINIO_ID_DELIMITER = "/";

    // Minio
    public static final String TESTS_BUCKET = "tests";
    public static final String TEST_EXPERIMENT_DEFINITION_FILE_NAME = TEST_EXPERIMENT_DEFINITION_NAME + YAML_EXTENSION;
    public static final String DEPLOYMENT_DESCRIPTOR_FILE_NAME = DEPLOYMENT_DESCRIPTOR_NAME + YAML_EXTENSION;

    public static final String GENERATED_BENCHMARK_FILE_NAME = "benchflow-benchmark.jar";

    // MongoDB
    public static final String DB_NAME = "benchflow-test-manager";
    public static final String MODEL_ID_DELIMITER = ".";
    public static final User BENCHFLOW_USER = new User("benchflow");
}
