package io.jenkins.plugins.rust;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.EnvVars;
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
import org.jenkinsci.plugins.workflow.steps.SynchronousStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * Pipeline step that wraps a closure with a Rust environment.
 * This works like other tool wrappers (nodejs, withMaven, etc.)
 * 
 * Usage:
 * withRust(rustInstallationName: 'stable') {
 * sh 'cargo build'
 * }
 */
public class WithRustStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String rustInstallationName;

    @DataBoundConstructor
    public WithRustStep(String rustInstallationName) {
        this.rustInstallationName = rustInstallationName;
    }

    public String getRustInstallationName() {
        return rustInstallationName;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        return new WithRustStepExecution(this, context);
    }

    @Symbol("withRust")
    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "withRust";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Configure Rust environment";
        }

        @Override
        public boolean takesImplicitBlockArgument() {
            return true; // This step wraps a closure
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }
    }

    /**
     * Execution for withRust step - sets up environment and runs the closure
     */
    public static class WithRustStepExecution extends SynchronousStepExecution<Void> {

        private static final long serialVersionUID = 1L;
        private final WithRustStep step;

        protected WithRustStepExecution(WithRustStep step, StepContext context) {
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

            // Find the Rust installation
            RustInstallation installation = null;
            if (step.rustInstallationName != null && !step.rustInstallationName.trim().isEmpty()) {
                for (RustInstallation inst : RustInstallation.allInstallations()) {
                    if (step.rustInstallationName.equals(inst.getName())) {
                        installation = inst;
                        break;
                    }
                }

                if (installation == null) {
                    throw new IOException("Rust installation not found: " + step.rustInstallationName);
                }
            } else {
                // Use first available
                RustInstallation[] installations = RustInstallation.allInstallations();
                if (installations.length > 0) {
                    installation = installations[0];
                } else {
                    throw new IOException("No Rust installation configured");
                }
            }

            // Expand variables
            installation = installation.forNode(run.getExecutor().getOwner().getNode(), listener);
            installation = installation.forEnvironment(run.getEnvironment(listener));

            // Set up environment variables
            EnvVars envVars = run.getEnvironment(listener);
            String rustHome = installation.getHome();
            String binPath = rustHome + File.separator + "bin";

            envVars.put("CARGO_HOME", rustHome);
            envVars.put("RUSTUP_HOME", rustHome + File.separator + "rustup");

            // Add to PATH
            String currentPath = envVars.get("PATH");
            if (currentPath != null) {
                envVars.put("PATH", binPath + File.pathSeparator + currentPath);
            } else {
                envVars.put("PATH", binPath);
            }

            listener.getLogger().println("Configuring Rust environment:");
            listener.getLogger().println("  Installation: " + step.rustInstallationName);
            listener.getLogger().println("  CARGO_HOME: " + rustHome);
            listener.getLogger().println("  RUSTUP_HOME: " + rustHome + File.separator + "rustup");
            listener.getLogger().println("  PATH: " + binPath + " (prepended)");

            // The closure body will be executed by Jenkins with these environment variables
            // We return null because this is a wrapper step
            return null;
        }
    }
}
