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
 * Build wrapper that sets up Rust environment for a build.
 * Supports both freestyle jobs and Pipeline.
 */
public class RustBuildWrapper extends hudson.tasks.BuildWrapper implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RustBuildWrapper.class.getName());

    private final String rustInstallationName;

    @DataBoundConstructor
    public RustBuildWrapper(String rustInstallationName) {
        this.rustInstallationName = rustInstallationName;
    }

    public String getRustInstallationName() {
        return rustInstallationName;
    }

    @Override
    public Environment setUp(AbstractBuild build, Launcher launcher, BuildListener listener)
            throws IOException, InterruptedException {

        if (rustInstallationName == null || rustInstallationName.trim().isEmpty()) {
            listener.getLogger().println("No Rust installation specified");
            return new Environment() {
            };
        }

        // Get the installation
        RustInstallation installation = null;
        RustInstallation[] installations = RustInstallation.allInstallations();

        for (RustInstallation inst : installations) {
            if (inst.getName().equals(rustInstallationName)) {
                installation = inst;
                break;
            }
        }

        if (installation == null) {
            listener.error("Rust installation not found: " + rustInstallationName);
            LOGGER.log(Level.WARNING, "Rust installation not found: {0}", rustInstallationName);
            return new Environment() {
            };
        }

        listener.getLogger().println("Setting up Rust environment: " + rustInstallationName);

        final String rustHome = installation.getHome();
        listener.getLogger().println("Rust Home: " + rustHome);

        return new Environment() {
            @Override
            public void buildEnvVars(Map<String, String> env) {
                // Set CARGO_HOME and RUSTUP_HOME
                env.put("CARGO_HOME", rustHome);
                env.put("RUSTUP_HOME", rustHome + File.separator + "rustup");

                // Add Rust bin to PATH
                String rustBinPath = rustHome + File.separator + "bin";
                String pathValue = env.get("PATH");

                if (pathValue == null || pathValue.isEmpty()) {
                    env.put("PATH", rustBinPath);
                } else {
                    env.put("PATH", rustBinPath + File.pathSeparator + pathValue);
                }

                listener.getLogger().println("Rust bin added to PATH: " + rustBinPath);
                listener.getLogger().println("CARGO_HOME=" + rustHome);
                listener.getLogger().println("RUSTUP_HOME=" + rustHome + File.separator + "rustup");

                // Verify installation
                File homeDir = new File(rustHome);
                if (!RustInstaller.verifyInstallation(homeDir)) {
                    listener.error("WARNING: Rust installation verification failed. " +
                            "cargo or rustc binary not found in: " + rustBinPath);
                    LOGGER.log(Level.WARNING, "Rust installation verification failed for: {0}", rustHome);
                } else {
                    try {
                        String version = RustInstaller.getCargoVersion(homeDir);
                        if (version != null) {
                            listener.getLogger().println("Using Rust/Cargo " + version);
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.FINE, "Could not determine Rust version", e);
                    }
                }
            }
        };
    }

    /**
     * Descriptor for RustBuildWrapper.
     */
    @Extension
    @Symbol("withRust")
    public static class DescriptorImpl extends BuildWrapperDescriptor {

        @Override
        public String getDisplayName() {
            return "Use Rust";
        }

        @Override
        public boolean isApplicable(AbstractProject<?, ?> project) {
            return true;
        }

        /**
         * Fill the Rust installation dropdown.
         */
        public ListBoxModel doFillRustInstallationNameItems() {
            ListBoxModel items = new ListBoxModel();
            items.add("(Default)", "");

            RustInstallation[] installations = RustInstallation.allInstallations();
            for (RustInstallation installation : installations) {
                items.add(installation.getName(), installation.getName());
            }

            return items;
        }
    }
}
