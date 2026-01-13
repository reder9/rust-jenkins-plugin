package io.jenkins.plugins.rust;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * Convenience pipeline step for running Rust tests.
 * Usage: cargoTest(testName: 'integration::*', publishResults: true)
 */
public class CargoTestStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private String testName;
    private boolean noCapture;
    private boolean publishResults = true; // Default to publishing
    private String rustInstallationName;

    @DataBoundConstructor
    public CargoTestStep() {
    }

    public String getTestName() {
        return testName;
    }

    @DataBoundSetter
    public void setTestName(String testName) {
        this.testName = testName;
    }

    public boolean isNoCapture() {
        return noCapture;
    }

    @DataBoundSetter
    public void setNoCapture(boolean noCapture) {
        this.noCapture = noCapture;
    }

    public boolean isPublishResults() {
        return publishResults;
    }

    @DataBoundSetter
    public void setPublishResults(boolean publishResults) {
        this.publishResults = publishResults;
    }

    public String getRustInstallationName() {
        return rustInstallationName;
    }

    @DataBoundSetter
    public void setRustInstallationName(String rustInstallationName) {
        this.rustInstallationName = rustInstallationName;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new CargoTestStepExecution(this, context);
    }

    @Symbol("cargoTest")
    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "cargoTest";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Run Rust tests with Cargo";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }
    }

    /**
     * Execution logic for CargoTestStep with test result publishing
     */
    public static class CargoTestStepExecution extends SynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;
        private final CargoTestStep step;

        protected CargoTestStepExecution(CargoTestStep step, StepContext context) {
            super(context);
            this.step = step;
        }

        @Override
        protected Void run() throws Exception {
            FilePath workspace = getContext().get(FilePath.class);
            Launcher launcher = getContext().get(Launcher.class);
            TaskListener listener = getContext().get(TaskListener.class);
            Run<?, ?> run = getContext().get(Run.class);

            if (workspace == null || launcher == null || listener == null || run == null) {
                throw new IOException("Missing required context");
            }

            // Build the args
            StringBuilder argsBuilder = new StringBuilder();

            if (step.testName != null && !step.testName.trim().isEmpty()) {
                argsBuilder.append(step.testName);
                argsBuilder.append(" ");
            }

            // Add nocapture if requested
            if (step.noCapture) {
                argsBuilder.append("-- --nocapture");
            }

            // Delegate to CargoStep
            CargoStep cargoStep = new CargoStep("test");
            if (argsBuilder.length() > 0) {
                cargoStep.setArgs(argsBuilder.toString().trim());
            }
            if (step.rustInstallationName != null) {
                cargoStep.setRustInstallationName(step.rustInstallationName);
            }

            try {
                // Create and execute CargoStepExecution directly
                CargoStep.CargoStepExecution execution = new CargoStep.CargoStepExecution(cargoStep, getContext());
                execution.run();

                // If publishing results, provide guidance
                if (step.publishResults) {
                    listener.getLogger().println("");
                    listener.getLogger().println("=== Test Result Publishing ===");
                    listener.getLogger().println("For JUnit XML test results, consider using cargo-nextest:");
                    listener.getLogger().println("  1. Install: cargo install cargo-nextest");
                    listener.getLogger().println("  2. Run tests: cargo nextest run --profile ci");
                    listener.getLogger().println("  3. Publish: junit '**/target/nextest/ci/junit.xml'");
                    listener.getLogger().println("===============================");
                }
            } catch (Exception e) {
                // Tests may have failed, but we still want to publish results
                if (step.publishResults) {
                    listener.getLogger()
                            .println("Tests completed with failures (to publish results, use cargo-nextest)");
                }
                throw e;
            }

            return null;
        }
    }
}
