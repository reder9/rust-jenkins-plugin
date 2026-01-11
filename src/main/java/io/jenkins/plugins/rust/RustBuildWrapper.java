package io.jenkins.plugins.rust;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ListBoxModel;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A build wrapper that sets up a Rust environment for a build. This class is responsible
 * for locating the specified Rust installation, configuring the necessary environment
 * variables (such as PATH, CARGO_HOME, and RUSTUP_HOME), and making them available
 * to the build. It supports both freestyle jobs and Pipeline.
 */
public class RustBuildWrapper extends hudson.tasks.BuildWrapper implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RustBuildWrapper.class.getName());

    private final String rustInstallationName;

    /**
     * Constructs a new {@code RustBuildWrapper}.
     *
     * @param rustInstallationName The name of the Rust installation to use, as configured
     * in the Jenkins global tool configuration.
     */
    @DataBoundConstructor
    public RustBuildWrapper(String rustInstallationName) {
        this.rustInstallationName = rustInstallationName;
    }

    /**
     * Returns the configured Rust installation name.
     *
     * @return The name of the Rust installation.
     */
    public String getRustInstallationName() {
        return rustInstallationName;
    }

    /**
     * Sets up the Rust environment for the build. This method is called by Jenkins
     * before the build starts. It finds the configured Rust installation and returns
     * an {@link Environment} object that provides the necessary environment variables.
     *
     * @param build The current build.
     * @param launcher A launcher for running commands.
     * @param listener A listener for reporting build progress and results.
     * @return An {@link Environment} object that contributes variables to the build.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the build is interrupted.
     */
    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {

        LOGGER.fine("Starting Rust environment setup with BuildWrapper");

        // If no installation is specified, log it and return a no-op environment
        if (rustInstallationName == null || rustInstallationName.trim().isEmpty()) {
            listener.getLogger().println("No Rust installation specified. Skipping setup.");
            LOGGER.info("No Rust installation specified. Skipping setup.");
            return new Environment() {
            };
        }

        // Find the configured Rust installation
        RustInstallation installation = findInstallation(rustInstallationName);

        // If the installation is not found, log an error and return a no-op environment
        if (installation == null) {
            listener.error("Rust installation not found: " + rustInstallationName);
            LOGGER.log(Level.WARNING, "Rust installation not found: {0}", rustInstallationName);
            return new Environment() {
            };
        }

        // Log details about the selected installation
        final String rustHome = installation.getHome();
        listener.getLogger().println("Setting up Rust environment: " + rustInstallationName);
        listener.getLogger().println("Rust Home: " + rustHome);
        LOGGER.log(Level.INFO, "Setting up Rust environment: {0} with home: {1}",
                new Object[]{rustInstallationName, rustHome});

        // Return a new Environment with the necessary variables
        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                LOGGER.fine("Injecting Rust environment variables");

                // Set CARGO_HOME and RUSTUP_HOME
                env.put("CARGO_HOME", rustHome);
                env.put("RUSTUP_HOME", rustHome + File.separator + "rustup");

                // Add the Rust bin directory to the PATH
                String rustBinPath = rustHome + File.separator + "bin";
                String pathValue = env.get("PATH");

                if (pathValue == null || pathValue.isEmpty()) {
                    env.put("PATH", rustBinPath);
                } else {
                    env.put("PATH", rustBinPath + File.pathSeparator + pathValue);
                }

                // Log the environment variables being set
                listener.getLogger().println("Rust bin added to PATH: " + rustBinPath);
                listener.getLogger().println("CARGO_HOME=" + rustHome);
                listener.getLogger().println("RUSTUP_HOME=" + rustHome + File.separator + "rustup");
                LOGGER.log(Level.INFO, "PATH updated: {0}", env.get("PATH"));
                LOGGER.log(Level.INFO, "CARGO_HOME set to: {0}", rustHome);
                LOGGER.log(Level.INFO, "RUSTUP_HOME set to: {0}", rustHome + File.separator + "rustup");

                // Verify the installation and log the version if possible
                verifyInstallation(rustBinPath, listener);
            }

            private void verifyInstallation(String rustBinPath, BuildListener listener) {
                File homeDir = new File(rustHome);
                if (!RustInstaller.verifyInstallation(homeDir)) {
                    String errorMessage = "WARNING: Rust installation verification failed. "
                            + "cargo or rustc binary not found in: " + rustBinPath;
                    listener.error(errorMessage);
                    LOGGER.log(Level.WARNING, "Rust installation verification failed for: {0}", rustHome);
                } else {
                    try {
                        String version = RustInstaller.getCargoVersion(homeDir);
                        if (version != null) {
                            listener.getLogger().println("Using Rust/Cargo " + version);
                            LOGGER.log(Level.INFO, "Found Rust/Cargo version: {0}", version);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "Could not determine Rust version", e);
                    }
                }
            }
        };
    }

    /**
     * Finds a Rust installation by name from the globally configured installations.
     *
     * @param name The name of the installation to find.
     * @return The {@link RustInstallation} object, or {@code null} if not found.
     */
    private RustInstallation findInstallation(String name) {
        for (RustInstallation inst : RustInstallation.allInstallations()) {
            if (inst.getName().equals(name)) {
                return inst;
            }
        }
        return null;
    }

    /**
     * Descriptor for the {@link RustBuildWrapper}. This class provides metadata about
     * the build wrapper and is used by Jenkins to display it in the UI. It also
     * populates the dropdown list of available Rust installations.
     */
    @Extension
    @Symbol("withRust")
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        /**
         * Returns the display name for the build wrapper, which is shown in the Jenkins UI.
         *
         * @return The display name.
         */
        @Override
        public String getDisplayName() {
            return "Use Rust";
        }

        /**
         * Indicates whether this build wrapper is applicable to the given project.
         *
         * @param project The project to check.
         * @return Always {@code true}, as this wrapper is applicable to all project types.
         */
        @Override
        public boolean isApplicable(AbstractProject<?, ?> project) {
            return true;
        }

        /**
         * Populates the dropdown list for the {@code rustInstallationName} field in the
         * Jenkins UI. It lists all globally configured Rust installations.
         *
         * @return A {@link ListBoxModel} containing the available Rust installations.
         */
        public ListBoxModel doFillRustInstallationNameItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("(Default)", "");

            for (RustInstallation installation : RustInstallation.allInstallations()) {
                items.add(installation.getName(), installation.getName());
            }

            return items;
        }
    }
}
