package io.jenkins.plugins.rust;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.jenkinsci.plugins.workflow.steps.SynchronousNonBlockingStepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

/**
 * Pipeline step for running Cargo commands.
 * Usage: cargo(command: 'build', args: '--release')
 */
public class CargoStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String command;
    private String args;
    private String rustInstallationName;

    @DataBoundConstructor
    public CargoStep(String command) {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public String getArgs() {
        return args;
    }

    @DataBoundSetter
    public void setArgs(String args) {
        this.args = args;
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
        return new CargoStepExecution(this, context);
    }

    @Symbol("cargo")
    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "cargo";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Run Cargo command";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        /**
         * Fill the Rust installation dropdown.
         */
        public ListBoxModel doFillRustInstallationNameItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("(Use tools block)", "");

            RustInstallation[] installations = RustInstallation.allInstallations();
            for (RustInstallation installation : installations) {
                items.add(installation.getName(), installation.getName());
            }

            return items;
        }
    }

    /**
     * Execution for CargoStep.
     */
    public static class CargoStepExecution extends SynchronousNonBlockingStepExecution<Void> {

        private static final long serialVersionUID = 1L;

        private final CargoStep step;

        protected CargoStepExecution(CargoStep step, StepContext context) {
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

            // Get Rust installation
            RustInstallation installation = null;

            if (step.rustInstallationName != null && !step.rustInstallationName.trim().isEmpty()) {
                // Use specified installation
                for (RustInstallation inst : RustInstallation.allInstallations()) {
                    if (step.rustInstallationName.equals(inst.getName())) {
                        installation = inst;
                        break;
                    }
                }

                if (installation == null) {
                    listener.error("Rust installation not found: " + step.rustInstallationName);
                    throw new IOException("Rust installation not found: " + step.rustInstallationName);
                }
            } else {
                // Try to get from tools block (environment)
                String cargoHome = run.getEnvironment(listener).get("CARGO_HOME");
                if (cargoHome != null) {
                    listener.getLogger().println("Using Rust from tools block (CARGO_HOME: " + cargoHome + ")");
                } else {
                    // Use first available installation or system cargo
                    RustInstallation[] installations = RustInstallation.allInstallations();
                    if (installations.length > 0) {
                        installation = installations[0];
                        listener.getLogger().println("Using default Rust installation: " + installation.getName());
                    } else {
                        listener.getLogger().println("No Rust installation configured, using system cargo");
                    }
                }
            }

            // Prepare the cargo command
            StringBuilder commandBuilder = new StringBuilder();

            if (installation != null) {
                // Expand variables and translate for node
                installation = installation.forNode(run.getExecutor().getOwner().getNode(), listener);
                installation = installation.forEnvironment(run.getEnvironment(listener));

                String cargoPath = installation.getCargo();
                commandBuilder.append(cargoPath);
            } else {
                commandBuilder.append("cargo");
            }

            commandBuilder.append(" ").append(step.command);

            if (step.args != null && !step.args.trim().isEmpty()) {
                commandBuilder.append(" ").append(step.args);
            }

            String fullCommand = commandBuilder.toString();
            listener.getLogger().println("Executing: " + fullCommand);

            // Ensure workspace exists
            if (!workspace.exists()) {
                workspace.mkdirs();
            }

            // Prepare environment variables for rustup/cargo
            hudson.EnvVars envVars = run.getEnvironment(listener);

            // If we have a Rust installation, set CARGO_HOME and RUSTUP_HOME
            if (installation != null) {
                String rustHome = installation.getHome();
                envVars.put("CARGO_HOME", rustHome);
                envVars.put("RUSTUP_HOME", rustHome + File.separator + "rustup");
                listener.getLogger().println("Setting CARGO_HOME=" + rustHome);
                listener.getLogger().println("Setting RUSTUP_HOME=" + rustHome + File.separator + "rustup");
            }

            // Execute the command
            int exitCode = launcher.launch()
                    .cmds(launcher.isUnix() ? new String[] { "sh", "-c", fullCommand }
                            : new String[] { "cmd", "/c", fullCommand })
                    .pwd(workspace)
                    .envs(envVars)
                    .stdout(listener)
                    .stderr(listener.getLogger())
                    .join();

            if (exitCode != 0) {
                throw new IOException("Cargo command failed with exit code: " + exitCode);
            }

            return null;
        }
    }
}
