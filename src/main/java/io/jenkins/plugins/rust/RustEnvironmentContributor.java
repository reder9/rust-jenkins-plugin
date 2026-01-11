package io.jenkins.plugins.rust;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Contributes Rust-specific environment variables to all builds. This class automatically
 * injects the PATH, CARGO_HOME, and RUSTUP_HOME variables for the first configured
 * Rust installation.
 * <p>
 * TODO: This implementation is simplistic and may not be suitable for environments with
 * multiple Rust installations. It should be revisited to allow for more sophisticated
 * selection of the Rust toolchain, possibly based on build parameters.
 */
@Extension
public class RustEnvironmentContributor extends EnvironmentContributor {

    private static final Logger LOGGER = Logger.getLogger(RustEnvironmentContributor.class.getName());

    /**
     * Called by Jenkins to contribute environment variables to a build. This method
     * injects variables for the first globally configured Rust installation.
     *
     * @param run The current build.
     * @param envVars The environment variables for the build, which will be modified by this method.
     * @param listener A listener for reporting build progress.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the build is interrupted.
     */
    @Override
    public void buildEnvironmentFor(Run run, EnvVars envVars, TaskListener listener)
            throws IOException, InterruptedException {

        LOGGER.fine("Evaluating Rust environment contribution");

        // Get the global Rust plugin configuration
        RustGlobalConfiguration config = RustGlobalConfiguration.get();
        if (config == null) {
            LOGGER.fine("Rust plugin not configured; skipping environment contribution.");
            return;
        }

        // Get the list of configured Rust installations
        RustInstallation[] installations = config.getInstallations().toArray(new RustInstallation[0]);
        if (installations.length == 0) {
            LOGGER.fine("No Rust installations configured; skipping environment contribution.");
            return;
        }

        // TODO: This logic is brittle as it only considers the first installation.
        // This should be improved to allow for selection of a specific installation.
        RustInstallation installation = installations[0];
        String rustHome = installation.getHome();

        if (rustHome != null && !rustHome.isEmpty()) {
            LOGGER.log(Level.INFO, "Contributing Rust environment for installation: {0}", installation.getName());

            // Define the path to the bin directory
            String rustBinPath = rustHome + File.separator + "bin";
            String currentPath = envVars.get("PATH");

            // Prepend the Rust bin directory to the PATH
            if (currentPath != null && !currentPath.isEmpty()) {
                String pathSeparator = System.getProperty("path.separator", ":");
                envVars.put("PATH", rustBinPath + pathSeparator + currentPath);
            } else {
                envVars.put("PATH", rustBinPath);
            }

            // Set CARGO_HOME and RUSTUP_HOME
            envVars.put("CARGO_HOME", rustHome);
            envVars.put("RUSTUP_HOME", rustHome + File.separator + "rustup");

            LOGGER.log(Level.INFO, "PATH updated to: {0}", envVars.get("PATH"));
            LOGGER.log(Level.INFO, "CARGO_HOME set to: {0}", rustHome);
            LOGGER.log(Level.INFO, "RUSTUP_HOME set to: {0}", rustHome + File.separator + "rustup");
        } else {
            LOGGER.warning("Configured Rust installation has a blank or null home directory.");
        }
    }
}
