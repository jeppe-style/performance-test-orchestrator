package cloud.benchflow.testmanager.resources;

import cloud.benchflow.dsl.BenchFlowDSL;
import cloud.benchflow.dsl.definition.BenchFlowTest;
import cloud.benchflow.testmanager.api.request.ChangeBenchFlowTestStateRequest;
import cloud.benchflow.testmanager.api.response.ChangeBenchFlowTestStateResponse;
import cloud.benchflow.testmanager.api.response.RunBenchFlowTestResponse;
import cloud.benchflow.testmanager.archive.BenchFlowTestArchiveExtractor;
import cloud.benchflow.testmanager.constants.BenchFlowConstants;
import cloud.benchflow.testmanager.exceptions.BenchFlowTestIDDoesNotExistException;
import cloud.benchflow.testmanager.exceptions.InvalidTestArchiveException;
import cloud.benchflow.testmanager.exceptions.UserIDAlreadyExistsException;
import cloud.benchflow.testmanager.exceptions.web.InvalidBenchFlowTestIDWebException;
import cloud.benchflow.testmanager.exceptions.web.InvalidTestArchiveWebException;
import cloud.benchflow.testmanager.models.BenchFlowTestModel;
import cloud.benchflow.testmanager.models.User;
import cloud.benchflow.testmanager.services.external.BenchFlowExperimentManagerService;
import cloud.benchflow.testmanager.services.external.MinioService;
import cloud.benchflow.testmanager.services.internal.dao.BenchFlowExperimentModelDAO;
import cloud.benchflow.testmanager.services.internal.dao.BenchFlowTestModelDAO;
import cloud.benchflow.testmanager.services.internal.dao.UserDAO;
import cloud.benchflow.testmanager.tasks.RunBenchFlowTestTask;
import io.swagger.annotations.Api;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.zip.ZipInputStream;

/**
 * @author Jesper Findahl (jesper.findahl@usi.ch)
 *         created on 13.02.17.
 */
@Path("/v1/users/{username}/tests")
@Api(value = "benchflow-test")
public class BenchFlowTestResource {

    public static String RUN_PATH = "/run";
    public static String STATE_PATH = "/state";
    public static String STATUS_PATH = "/status";

    private Logger logger = LoggerFactory.getLogger(BenchFlowTestResource.class.getSimpleName());

    private final ExecutorService taskExecutorService;
    private final MinioService minioService;
    private final BenchFlowTestModelDAO testModelDAO;
    private final BenchFlowExperimentModelDAO experimentModelDAO;
    private final UserDAO userDAO;
    private final BenchFlowExperimentManagerService experimentManagerService;

    public BenchFlowTestResource(ExecutorService taskExecutorService, MinioService minioService, BenchFlowTestModelDAO testModelDAO, BenchFlowExperimentModelDAO experimentModelDAO, UserDAO userDAO, BenchFlowExperimentManagerService experimentManagerService) {
        this.taskExecutorService = taskExecutorService;
        this.minioService = minioService;
        this.testModelDAO = testModelDAO;
        this.experimentModelDAO = experimentModelDAO;
        this.userDAO = userDAO;
        this.experimentManagerService = experimentManagerService;
    }

    @POST
    @Path("/run")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public RunBenchFlowTestResponse runBenchFlowTest(@PathParam("username") String username,
                                                     @FormDataParam("benchFlowTestBundle") final InputStream benchFlowTestBundle) {

        logger.info("request received: POST " + BenchFlowConstants.getPathFromUsername(username) + RUN_PATH);

        if (benchFlowTestBundle == null) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        ZipInputStream archiveZipInputStream = new ZipInputStream(benchFlowTestBundle);

        User user = new User(username);

        // TODO - check valid user
        // TODO - move user creating into separate Class for handling users
        if (!userDAO.userExists(user)) {

            try {
                userDAO.addUser(BenchFlowConstants.BENCHFLOW_USER.getUsername());
            } catch (UserIDAlreadyExistsException e) {
                // since we already checked that the user doesn't exist it cannot happen
                throw new WebApplicationException(Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        try {

            // validate archive
            // Get the contents of archive and check if valid Test ID
            String testDefinitionString = BenchFlowTestArchiveExtractor.extractBenchFlowTestDefinitionString(
                    archiveZipInputStream);

            if (testDefinitionString == null)
                throw new InvalidTestArchiveException();

            BenchFlowTest benchFlowTest = BenchFlowDSL.testFromYaml(testDefinitionString).get();

            InputStream deploymentDescriptorInputStream = BenchFlowTestArchiveExtractor.extractDeploymentDescriptorInputStream(
                    archiveZipInputStream);
            Map<String, InputStream> bpmnModelsInputStream = BenchFlowTestArchiveExtractor.extractBPMNModelInputStreams(
                    archiveZipInputStream);

            if (deploymentDescriptorInputStream == null || bpmnModelsInputStream.size() == 0) {
                throw new InvalidTestArchiveException();
            }

            // save new experiment
            String testID = testModelDAO.addTestModel(benchFlowTest.name(), BenchFlowConstants.BENCHFLOW_USER);

            // create new task and run it
            RunBenchFlowTestTask task = new RunBenchFlowTestTask(
                    testID,
                    minioService,
                    experimentManagerService,
                    experimentModelDAO,
                    testDefinitionString,
                    deploymentDescriptorInputStream,
                    bpmnModelsInputStream
            );

            // TODO - should go into a stateless queue (so that we can recover)
            taskExecutorService.submit(task);

            return new RunBenchFlowTestResponse(testID);

        } catch (IOException | InvalidTestArchiveException e) {
            throw new InvalidTestArchiveWebException();
        }

    }

    @PUT
    @Path("{testName}/{testNumber}/state")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ChangeBenchFlowTestStateResponse changeBenchFlowTestState(@PathParam("username") String username,
                                                                     @PathParam("testName") String testName,
                                                                     @PathParam("testNumber") int testNumber,
                                                                     @NotNull @Valid final ChangeBenchFlowTestStateRequest stateRequest) {

        String testID = BenchFlowConstants.getTestID(username, testName, testNumber);
        logger.info("request received: PUT " + BenchFlowConstants.getPathFromTestID(testID) + STATE_PATH);

        // TODO - handle the actual state change (e.g. on PE Manager)


        // update the state
        BenchFlowTestModel.BenchFlowTestState newState = null;

        try {
            newState = testModelDAO.setTestState(testID, stateRequest.getState());
        } catch (BenchFlowTestIDDoesNotExistException e) {
            throw new InvalidBenchFlowTestIDWebException();
        }

        // return the state as saved
        return new ChangeBenchFlowTestStateResponse(newState);

    }

    @Path("{testName}/{testNumber}/status")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public BenchFlowTestModel getBenchFlowTestStatus(@PathParam("username") String username,
                                                     @PathParam("testName") String testName,
                                                     @PathParam("testNumber") int testNumber) {

        String testID = BenchFlowConstants.getTestID(username, testName, testNumber);

        logger.info("request received: GET " + BenchFlowConstants.getPathFromTestID(testID) + STATUS_PATH);

        // get the BenchFlowTestModel from DAO
        BenchFlowTestModel benchFlowTestModel = null;

        try {
            benchFlowTestModel = testModelDAO.getTestModel(testID);
        } catch (BenchFlowTestIDDoesNotExistException e) {
            throw new InvalidBenchFlowTestIDWebException();
        }

        return benchFlowTestModel;

    }

}
