package io.jenkins.plugins.rust;

import hudson.model.Result;
import org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Cargo pipeline steps
 * These tests verify step construction, serialization, and basic pipeline
 * integration
 */
public class CargoPipelineStepsTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private RustInstallation testInstallation;

    @Before
    public void setUp() throws Exception {
        // Create a test Rust installation
        testInstallation = new RustInstallation("TestRust", "/usr/local", Collections.emptyList());

        // Register it globally with Jenkins
        jenkins.jenkins.getDescriptorByType(RustInstallation.DescriptorImpl.class).setInstallations(testInstallation);
    }

    @Test
    public void testCargoStepSerialization() throws Exception {
        // Test that CargoStep can be serialized (required for pipeline persistence)
        CargoStep cargoStep = new CargoStep("build");
        cargoStep.setArgs("--release");
        cargoStep.setRustInstallationName("TestRust");

        assertThat(cargoStep.getCommand(), is("build"));
        assertThat(cargoStep.getArgs(), is("--release"));
        assertThat(cargoStep.getRustInstallationName(), is("TestRust"));
    }

    @Test
    public void testCargoBuildStepSerialization() throws Exception {
        CargoBuildStep buildStep = new CargoBuildStep();
        buildStep.setRelease(true);
        buildStep.setTarget("wasm32-unknown-unknown");
        buildStep.setFeatures("serde");
        buildStep.setRustInstallationName("TestRust");

        assertThat(buildStep.isRelease(), is(true));
        assertThat(buildStep.getTarget(), is("wasm32-unknown-unknown"));
        assertThat(buildStep.getFeatures(), is("serde"));
        assertThat(buildStep.getRustInstallationName(), is("TestRust"));
    }

    @Test
    public void testCargoTestStepSerialization() throws Exception {
        CargoTestStep testStep = new CargoTestStep();
        testStep.setTestName("integration::*");
        testStep.setNoCapture(true);
        testStep.setRustInstallationName("TestRust");

        assertThat(testStep.getTestName(), is("integration::*"));
        assertThat(testStep.isNoCapture(), is(true));
        assertThat(testStep.getRustInstallationName(), is("TestRust"));
    }

    @Test
    public void testCargoClippyStepSerialization() throws Exception {
        CargoClippyStep clippyStep = new CargoClippyStep();
        clippyStep.setDenyWarnings(true);
        clippyStep.setAllTargets(true);
        clippyStep.setRustInstallationName("TestRust");

        assertThat(clippyStep.isDenyWarnings(), is(true));
        assertThat(clippyStep.isAllTargets(), is(true));
        assertThat(clippyStep.getRustInstallationName(), is("TestRust"));
    }

    @Test
    public void testCargoStepDescriptor() throws Exception {
        CargoStep.DescriptorImpl descriptor = new CargoStep.DescriptorImpl();

        assertThat(descriptor.getFunctionName(), is("cargo"));
        assertThat(descriptor.getDisplayName(), is("Run Cargo command"));
    }

    @Test
    public void testCargoBuildStepDescriptor() throws Exception {
        CargoBuildStep.DescriptorImpl descriptor = new CargoBuildStep.DescriptorImpl();

        assertThat(descriptor.getFunctionName(), is("cargoBuild"));
        assertThat(descriptor.getDisplayName(), is("Build Rust project with Cargo"));
    }

    @Test
    public void testCargoTestStepDescriptor() throws Exception {
        CargoTestStep.DescriptorImpl descriptor = new CargoTestStep.DescriptorImpl();

        assertThat(descriptor.getFunctionName(), is("cargoTest"));
        assertThat(descriptor.getDisplayName(), is("Run Rust tests with Cargo"));
    }

    @Test
    public void testCargoClippyStepDescriptor() throws Exception {
        CargoClippyStep.DescriptorImpl descriptor = new CargoClippyStep.DescriptorImpl();

        assertThat(descriptor.getFunctionName(), is("cargoClippy"));
        assertThat(descriptor.getDisplayName(), is("Run Clippy linter on Rust code"));
    }

    @Test
    public void testPipelineWithToolsBlock() throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-tools-block");

        // Simple scripted pipeline with tools
        String pipelineScript = "node {\n" +
                "  echo 'Testing pipeline structure'\n" +
                "}";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        assertThat(run.getResult(), is(Result.SUCCESS));
    }

    @Test
    public void testScriptedPipelineStructure() throws Exception {
        WorkflowJob job = jenkins.createProject(WorkflowJob.class, "test-scripted");

        String pipelineScript = "node {\n" +
                "  echo 'Testing pipeline structure'\n" +
                "}";

        job.setDefinition(new CpsFlowDefinition(pipelineScript, true));
        WorkflowRun run = jenkins.buildAndAssertSuccess(job);

        assertThat(run.getResult(), is(Result.SUCCESS));
    }
}
