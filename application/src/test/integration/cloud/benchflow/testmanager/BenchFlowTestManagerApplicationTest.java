package cloud.benchflow.testmanager;

import cloud.benchflow.testmanager.api.request.ChangeBenchFlowTestStateRequest;
import cloud.benchflow.testmanager.api.response.RunBenchFlowTestResponse;
import cloud.benchflow.testmanager.archive.TestArchives;
import cloud.benchflow.testmanager.configurations.BenchFlowTestManagerConfiguration;
import cloud.benchflow.testmanager.constants.BenchFlowConstants;
import cloud.benchflow.testmanager.helpers.TestConstants;
import cloud.benchflow.testmanager.models.BenchFlowTestModel;
import cloud.benchflow.testmanager.models.User;
import cloud.benchflow.testmanager.resources.BenchFlowTestResource;
import cloud.benchflow.testmanager.resources.BenchFlowUserResource;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.client.JerseyClientConfiguration;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit.DropwizardAppRule;
import io.dropwizard.util.Duration;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 18.02.17.
 */
public class BenchFlowTestManagerApplicationTest extends DockerComposeTest {


    @Rule
    public final DropwizardAppRule<BenchFlowTestManagerConfiguration> RULE = new DropwizardAppRule<>(
            BenchFlowTestManagerApplication.class,
            "../configuration.yml",
            ConfigOverride.config("mongoDB.hostname", MONGO_CONTAINER.getIp()),
            ConfigOverride.config("mongoDB.port", String.valueOf(MONGO_CONTAINER.getExternalPort())),
            ConfigOverride.config("minio.address", "http://" + MINIO_CONTAINER.getIp() + ":" + MINIO_CONTAINER.getExternalPort()),
            ConfigOverride.config("minio.accessKey", MINIO_ACCESS_KEY),
            ConfigOverride.config("minio.secretKey", MINIO_SECRET_KEY),
            ConfigOverride.config("benchFlowExperimentManager.address", "localhost")
    );

    @Test
    public void runBenchFlowTest() throws Exception {

        // needed for multipart client
        // https://github.com/dropwizard/dropwizard/issues/1013
        JerseyClientConfiguration configuration = new JerseyClientConfiguration();
        configuration.setChunkedEncodingEnabled(false);
        // needed because parsing testYaml takes more than default time
        configuration.setTimeout(Duration.milliseconds(1000));

        Client client = new JerseyClientBuilder(RULE.getEnvironment()).using(configuration).build("test client");

        String testName = "testNameExample";
        User user = BenchFlowConstants.BENCHFLOW_USER;

        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("benchFlowTestBundle",
                                                                 TestArchives.getValidTestArchiveFile(),
                                                                 MediaType.APPLICATION_OCTET_STREAM_TYPE);

        MultiPart multiPart = new MultiPart();
        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
        multiPart.bodyPart(fileDataBodyPart);

        Response response = client
                .target(String.format("http://localhost:%d/" + user.getUsername() + BenchFlowUserResource.RUN_PATH, RULE.getLocalPort()))
                .register(MultiPartFeature.class)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.entity(multiPart, multiPart.getMediaType()));

        Assert.assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        RunBenchFlowTestResponse testResponse = response.readEntity(RunBenchFlowTestResponse.class);

        Assert.assertNotNull(testResponse);
        Assert.assertTrue(testResponse.getTestID().contains(testName));

    }

    @Test
    public void changeTestStateInvalid() throws Exception {

        Client client = new JerseyClientBuilder(RULE.getEnvironment()).build("test client");

        BenchFlowTestModel.BenchFlowTestState state = BenchFlowTestModel.BenchFlowTestState.COMPLETED;
        String testID = TestConstants.VALID_TEST_ID;

        ChangeBenchFlowTestStateRequest stateRequest = new ChangeBenchFlowTestStateRequest(state);

        String target = "http://localhost:" + RULE.getLocalPort() + "/" + BenchFlowConstants.getPathFromBenchFlowID(testID) + BenchFlowTestResource.STATE_PATH;

        Response response = client
                .target(target)
                .request(MediaType.APPLICATION_JSON)
                .put(Entity.entity(stateRequest, MediaType.APPLICATION_JSON));

        Assert.assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());

        // TODO - check how to include message
//        Assert.assertEquals(InvalidBenchFlowTestIDWebException.message, response.getStatusInfo().getReasonPhrase());


    }

}