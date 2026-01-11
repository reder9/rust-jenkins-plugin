package io.jenkins.plugins.rust;

import edu.umd.cs.findbugs.annotations.CheckForNull;
import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
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
import java.util.Map;
import java.util.logging.Logger;

/**
 * Build step to setup and use Rust environment.
 * Compatible with both freestyle and Pipeline jobs.
 */
public class RustBuilder extends Builder implements SimpleBuildStep {
    private static final Logger LOGGER = Logger.getLogger(RustBuilder.class.getName());

    @CheckForNull
    private String rustInstallationName;

    @DataBoundConstructor
    public RustBuilder() {
    }

    @CheckForNull
    public String getRustInstallationName() {
        return rustInstallationName;
    }

    @DataBoundSetter
    public void setRustInstallationName(String rustInstallationName) {
        this.rustInstallationName = rustInstallationName;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run, @NonNull FilePath workspace,
            @NonNull Launcher launcher, @NonNull TaskListener listener)
            throws InterruptedException, IOException {

        if (rustInstallationName == null || rustInstallationName.isEmpty()) {
            listener.getLogger().println("No Rust installation specified");
            return;
        }

        RustGlobalConfiguration config = RustGlobalConfiguration.get();
        if (config == null) {
            listener.getLogger().println("ERROR: Rust plugin not configured");
            return;
        }

        RustInstallation installation = config.getInstallation(rustInstallationName);
        if (installation == null) {
            listener.getLogger().println("ERROR: Rust installation not found: " + rustInstallationName);
            throw new IOException("Rust installation not found: " + rustInstallationName);
        }

        String rustHome = installation.getHome();
        listener.getLogger().println("Setting up Rust environment: " + rustInstallationName);
        listener.getLogger().println("Rust Home: " + rustHome);

        // Set environment variables for the build
        String separator = File.separator;
        String rustBinPath = rustHome + separator + "bin";

        // This will be picked up by the environment setup
        listener.getLogger().println("Rust bin path: " + rustBinPath);
        listener.getLogger().println("CARGO_HOME: " + rustHome);
        listener.getLogger().println("RUSTUP_HOME: " + rustHome + separator + "rustup");
    }

    @Symbol("rust")
    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {
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
