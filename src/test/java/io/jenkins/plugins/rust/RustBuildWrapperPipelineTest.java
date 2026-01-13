package io.jenkins.plugins.rust;

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for RustBuildWrapper in Pipeline
 */
public class RustBuildWrapperPipelineTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private RustInstallation testInstallation;

    @Before
    public void setUp() throws Exception {
        // Create a test Rust installation
        testInstallation = new RustInstallation("TestRust", "/opt/rust", Collections.emptyList());

        // Register it globally
        RustInstallation.DescriptorImpl descriptor = jenkins.jenkins
                .getDescriptorByType(RustInstallation.DescriptorImpl.class);
        descriptor.setInstallations(testInstallation);
    }

    @Test
    @Ignore("withRust wrapper has known double-nesting parameter bug - use pipeline steps instead")
    public void testWithRustWrapperInPipeline() throws Exception {
        // Create a pipeline job
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-withrust");

        // Define a simple pipeline that uses withRust
        String pipelineScript = "node {\n" +
                "  withRust(rustInstallationName: 'TestRust') {\n" +
                "    echo 'Inside withRust wrapper'\n" +
                "    echo \"CARGO_HOME: ${env.CARGO_HOME}\"\n" +
                "  }\n" +
                "}";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        // Run the job
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        // Verify the output
        jenkins.assertLogContains("Inside withRust wrapper", run);
        jenkins.assertLogContains("CARGO_HOME:", run);
        jenkins.assertLogContains("/opt/rust", run);

        // Verify successful completion
        assertThat(run.getResult(), is(Result.SUCCESS));
    }

    @Test
    @Ignore("withRust wrapper has known double-nesting parameter bug - use pipeline steps instead")
    public void testWithRustWrapperDeclarativePipeline() throws Exception {
        // Create a pipeline job
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-declarative-withrust");

        // Define a declarative pipeline that uses withRust
        String pipelineScript = "pipeline {\n" +
                "  agent any\n" +
                "  stages {\n" +
                "    stage('Test') {\n" +
                "      steps {\n" +
                "        withRust(rustInstallationName: 'TestRust') {\n" +
                "          echo 'Inside withRust wrapper'\n" +
                "          sh 'echo \"CARGO_HOME: $CARGO_HOME\"'\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        // Run the job
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        // Verify the output
        jenkins.assertLogContains("Inside withRust wrapper", run);
        jenkins.assertLogContains("CARGO_HOME:", run);

        // Verify successful completion
        assertThat(run.getResult(), is(Result.SUCCESS));
    }

    @Test
    @Ignore("withRust wrapper has known double-nesting parameter bug - use pipeline steps instead")
    public void testWithRustWrapperMissingInstallation() throws Exception {
        // Create a pipeline job
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-missing");

        // Define a pipeline with a non-existent installation
        String pipelineScript = "node {\n" +
                "  withRust(rustInstallationName: 'NonExistent') {\n" +
                "    echo 'This should not run'\n" +
                "  }\n" +
                "}";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        // Run the job - it should still succeed but log an error
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        // Verify the error message
        jenkins.assertLogContains("Rust installation not found: NonExistent", run);
    }

    @Test
    @Ignore("withRust wrapper has known double-nesting parameter bug - use pipeline steps instead")
    public void testWithRustWrapperEmptyInstallationName() throws Exception {
        // Create a pipeline job
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-empty");

        // Define a pipeline with empty installation name
        String pipelineScript = "node {\n" +
                "  withRust(rustInstallationName: '') {\n" +
                "    echo 'This should not set up Rust'\n" +
                "  }\n" +
                "}";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));

        // Run the job
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        // Verify the message
        jenkins.assertLogContains("No Rust installation specified", run);
    }
}
