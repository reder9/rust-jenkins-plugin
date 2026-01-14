package io.jenkins.plugins.rust;

import hudson.EnvVars;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildWrapperDescriptor;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildWrapper;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.verb.POST;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Build wrapper that sets up Rust environment for a build.
 * Supports both freestyle jobs and Pipeline.
 */
public class RustBuildWrapper extends SimpleBuildWrapper implements Serializable {
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
    public void setUp(Context context, Run<?, ?> build, FilePath workspace, Launcher launcher,
            TaskListener listener, EnvVars initialEnvironment) throws IOException, InterruptedException {

        if (rustInstallationName == null || rustInstallationName.trim().isEmpty()) {
            listener.getLogger().println("No Rust installation specified");
            return;
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
            return;
        }

        listener.getLogger().println("Setting up Rust environment: " + rustInstallationName);

        // Expand variables and translate for node
        installation = installation.forNode(build.getExecutor().getOwner().getNode(), listener);
        installation = installation.forEnvironment(build.getEnvironment(listener));

        final String rustHome = installation.getHome();
        listener.getLogger().println("Rust Home: " + rustHome);

        // Set CARGO_HOME and RUSTUP_HOME
        context.env("CARGO_HOME", rustHome);
        context.env("RUSTUP_HOME", rustHome + File.separator + "rustup");

        // Add Rust bin to PATH using the PATH+ prefix syntax
        // Jenkins handles merging this correctly
        context.env("PATH+RUST", rustHome + File.separator + "bin");

        listener.getLogger().println("Rust environment setup complete");
        listener.getLogger().println("CARGO_HOME=" + rustHome);

        // Verify installation
        File homeDir = new File(rustHome);
        if (!RustInstaller.verifyInstallation(homeDir)) {
            listener.error("WARNING: Rust installation verification failed. " +
                    "cargo or rustc binary not found in bin directory.");
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
        @POST
        public ListBoxModel doFillRustInstallationNameItems() {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);

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
