package io.jenkins.plugins.rust;

import hudson.Extension;
import hudson.model.EnvironmentContributor;
import hudson.model.Run;
import hudson.model.TaskListener;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * Contributes Rust environment variables to builds
 */
@Extension
public class RustEnvironmentContributor extends EnvironmentContributor {
    private static final Logger LOGGER = Logger.getLogger(RustEnvironmentContributor.class.getName());

    @Override
    public void buildEnvironmentFor(Run run, hudson.EnvVars envVars, TaskListener listener)
            throws IOException, InterruptedException {

        RustGlobalConfiguration config = RustGlobalConfiguration.get();
        if (config == null) {
            return;
        }

        // Check if this build uses Rust
        RustInstallation[] installations = config.getInstallations().toArray(new RustInstallation[0]);
        if (installations == null || installations.length == 0) {
            return;
        }

        // For now, add the first installation's path to PATH
        // This could be made more sophisticated with build parameters
        RustInstallation installation = installations[0];
        String rustHome = installation.getHome();

        if (rustHome != null && !rustHome.isEmpty()) {
            String rustBinPath = rustHome + java.io.File.separator + "bin";
            String currentPath = envVars.get("PATH");

            if (currentPath != null && !currentPath.isEmpty()) {
                String pathSeparator = System.getProperty("path.separator", ":");
                envVars.put("PATH", rustBinPath + pathSeparator + currentPath);
            } else {
                envVars.put("PATH", rustBinPath);
            }

            envVars.put("CARGO_HOME", rustHome);
            envVars.put("RUSTUP_HOME", rustHome + java.io.File.separator + "rustup");
        }
    }
}
