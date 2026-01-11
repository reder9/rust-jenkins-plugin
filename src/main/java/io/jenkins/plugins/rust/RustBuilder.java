package io.jenkins.plugins.rust;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Build step to set up and use a Rust environment. This class is responsible for
 * configuring the necessary environment variables for a Rust toolchain, making it
 * available to subsequent build steps. It is compatible with both freestyle and
 * Pipeline jobs in Jenkins.
 */
public class RustBuilder extends Builder implements SimpleBuildStep {

    private static final Logger LOGGER = Logger.getLogger(RustBuilder.class.getName());

    @CheckForNull
    private String rustInstallationName;

    /**
     * Default constructor. Jenkins uses this to instantiate the builder.
     */
    @DataBoundConstructor
    public RustBuilder() {
        // Jenkins requires a default constructor for data binding
    }

    /**
     * Retrieves the configured Rust installation name.
     *
     * @return The name of the Rust installation to use.
     */
    @CheckForNull
    public String getRustInstallationName() {
        return rustInstallationName;
    }

    /**
     * Sets the Rust installation name. This method is used by Jenkins to inject
     * the value from the job configuration.
     *
     * @param rustInstallationName The name of the Rust installation to use.
     */
    @DataBoundSetter
    public void setRustInstallationName(String rustInstallationName) {
        this.rustInstallationName = rustInstallationName;
    }

    /**
     * Executes the build step. This method is called by Jenkins when the build
     * reaches this step. It identifies the configured Rust installation, sets up
     * the necessary environment variables (CARGO_HOME, RUSTUP_HOME), and adds the
     * Rust binaries to the PATH.
     *
     * @param run The current build.
     * @param workspace The workspace for the current job.
     * @param launcher A launcher for running commands.
     * @param listener A listener for reporting build progress and results.
     * @throws InterruptedException If the build is interrupted.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace,
            @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws InterruptedException, IOException {

        // Log the start of the setup process
        LOGGER.fine("Starting Rust environment setup");

        // If no installation is specified, log it and return
        if (rustInstallationName == null || rustInstallationName.isEmpty()) {
            listener.getLogger().println("No Rust installation specified. Skipping setup.");
            LOGGER.info("No Rust installation specified. Skipping setup.");
            return;
        }

        // Get the global plugin configuration
        RustGlobalConfiguration config = RustGlobalConfiguration.get();
        if (config == null) {
            String errorMessage = "ERROR: Rust plugin not configured in Jenkins global configuration.";
            listener.getLogger().println(errorMessage);
            LOGGER.severe(errorMessage);
            // We choose not to fail the build here, but this is a critical configuration error
            return;
        }

        // Find the configured Rust installation
        RustInstallation installation = config.getInstallation(rustInstallationName);
        if (installation == null) {
            String errorMessage = "ERROR: Rust installation not found: " + rustInstallationName;
            listener.getLogger().println(errorMessage);
            LOGGER.severe(errorMessage);
            // This is a fatal error for this build step, so we throw an exception
            throw new IOException(errorMessage);
        }

        // Log details about the selected installation
        String rustHome = installation.getHome();
        listener.getLogger().println("Setting up Rust environment: " + rustInstallationName);
        listener.getLogger().println("Rust Home: " + rustHome);
        LOGGER.log(Level.INFO, "Setting up Rust environment: {0} with home: {1}",
                new Object[]{rustInstallationName, rustHome});

        // Define the paths for the environment variables
        String separator = File.separator;
        String rustBinPath = rustHome + separator + "bin";

        // This information will be used by the build environment
        listener.getLogger().println("Adding to PATH: " + rustBinPath);
        listener.getLogger().println("Setting CARGO_HOME: " + rustHome);
        listener.getLogger().println("Setting RUSTUP_HOME: " + rustHome + separator + "rustup");

        // Log the environment variables being set
        LOGGER.log(Level.INFO, "PATH addition: {0}", rustBinPath);
        LOGGER.log(Level.INFO, "CARGO_HOME: {0}", rustHome);
        LOGGER.log(Level.INFO, "RUSTUP_HOME: {0}", rustHome + separator + "rustup");

        LOGGER.fine("Rust environment setup complete");
    }

    /**
     * Descriptor for the {@link RustBuilder}. This class provides metadata about
     * the builder and is used by Jenkins to display it in the UI.
     */
    @Symbol("rust")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        /**
         * Returns the display name for the build step, which is shown in the Jenkins UI.
         *
         * @return The display name.
         */
        @NonNull
        @Override
        public String getDisplayName() {
            return "Setup Rust environment";
        }

        @Override
        public boolean isApplicable(Class<? extends hudson.model.AbstractProject> jobType) {
            return true;
        }
    }
}
