package io.jenkins.plugins.rust;

import hudson.EnvVars;
import hudson.Extension;
import hudson.model.EnvironmentSpecific;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.NodeSpecific;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolProperty;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a Rust tool installation.
 * Supports multi-node deployments and environment variable expansion.
 *
 * @author RederSoft
 */
public class RustInstallation extends ToolInstallation
        implements NodeSpecific<RustInstallation>, EnvironmentSpecific<RustInstallation> {

    private static final Logger LOGGER = Logger.getLogger(RustInstallation.class.getName());

    /**
     * Constructor for RustInstallation.
     *
     * @param name       Tool name (for example, "Rust 1.75")
     * @param home       Tool location (usually the CARGO_HOME directory)
     * @param properties List of properties for this tool
     */
    @DataBoundConstructor
    public RustInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    /**
     * Get all configured Rust installations.
     *
     * @return Array of configured Rust installations
     */
    public static RustInstallation[] allInstallations() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            DescriptorImpl descriptor = instance.getDescriptorByType(DescriptorImpl.class);
            if (descriptor != null) {
                RustInstallation[] installations = descriptor.getInstallations();
                if (installations != null) {
                    return installations;
                }
            }
        }
        return new RustInstallation[0];
    }

    /**
     * Get the cargo binary path.
     *
     * @return Path to cargo binary
     */
    public String getCargo() {
        String binDir = getHome() + File.separator + "bin" + File.separator;
        return binDir + (isWindows() ? "cargo.exe" : "cargo");
    }

    /**
     * Get the rustc binary path.
     *
     * @return Path to rustc binary
     */
    public String getRustc() {
        String binDir = getHome() + File.separator + "bin" + File.separator;
        return binDir + (isWindows() ? "rustc.exe" : "rustc");
    }

    /**
     * Get the rustup binary path.
     *
     * @return Path to rustup binary
     */
    public String getRustup() {
        String binDir = getHome() + File.separator + "bin" + File.separator;
        return binDir + (isWindows() ? "rustup.exe" : "rustup");
    }

    /**
     * Check if running on Windows.
     *
     * @return true if running on Windows
     */
    private boolean isWindows() {
        return File.pathSeparatorChar == ';';
    }

    /**
     * Create a node-specific installation.
     * This is called when the installation is used on a specific node.
     *
     * @param node The node to create the installation for
     * @param log  Task listener for logging
     * @return Node-specific installation
     */
    @Override
    public RustInstallation forNode(Node node, TaskListener log) throws IOException, InterruptedException {
        return new RustInstallation(getName(), translateFor(node, log), Collections.emptyList());
    }

    /**
     * Create an environment-specific installation.
     * This expands environment variables in the home path.
     *
     * @param environment Environment variables to expand
     * @return Environment-specific installation
     */
    @Override
    public RustInstallation forEnvironment(EnvVars environment) {
        return new RustInstallation(getName(), environment.expand(getHome()), Collections.emptyList());
    }

    @Override
    public void buildEnvVars(EnvVars env) {
        String home = getHome();
        if (home == null || home.isEmpty()) {
            return;
        }

        // Set CARGO_HOME and RUSTUP_HOME
        env.put("CARGO_HOME", home);
        env.put("RUSTUP_HOME", home + File.separator + "rustup");

        // Add Rust bin to PATH
        String bin = home + File.separator + "bin";
        String path = env.get("PATH");
        if (path != null && !path.isEmpty()) {
            env.put("PATH", bin + File.pathSeparator + path);
        } else {
            env.put("PATH", bin);
        }
    }

    @Override
    public DescriptorImpl getDescriptor() {
        Jenkins jenkinsInstance = Jenkins.getInstanceOrNull();
        if (jenkinsInstance == null) {
            throw new AssertionError("Jenkins instance is null");
        }
        return (DescriptorImpl) jenkinsInstance.getDescriptorOrDie(getClass());
    }

    /**
     * Descriptor for RustInstallation.
     */
    @Extension
    @Symbol("rust")
    public static class DescriptorImpl extends ToolDescriptor<RustInstallation> {

        public DescriptorImpl() {
            super();
            load();
        }

        @Override
        public String getDisplayName() {
            return "Rust";
        }

        /**
         * Validate tool installation home directory.
         */
        public FormValidation doCheckHome(@QueryParameter final String value) {
            // Check permission
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins != null) {
                jenkins.checkPermission(Jenkins.ADMINISTER);
            }

            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Please specify a Rust installation directory");
            }

            File home = new File(value);
            if (!home.exists()) {
                return FormValidation.warning("Directory does not exist: " + value);
            }

            if (!home.isDirectory()) {
                return FormValidation.error("Path is not a directory: " + value);
            }

            if (!RustInstaller.verifyInstallation(home)) {
                return FormValidation.warning(
                        "Rust/Cargo binary not found in: " + value + File.separator + "bin");
            }

            try {
                String version = RustInstaller.getCargoVersion(home);
                if (version != null) {
                    return FormValidation.ok("Rust/Cargo " + version);
                }
            } catch (Exception e) {
                LOGGER.warning("Could not determine Rust version: " + e.getMessage());
                return FormValidation.warning("Could not determine Rust version: " + e.getMessage());
            }

            return FormValidation.ok();
        }

        /**
         * Validate tool name.
         */
        public FormValidation doCheckName(@QueryParameter final String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Rust installation name cannot be empty");
            }

            if (!value.matches("^[a-zA-Z0-9_-]+$")) {
                return FormValidation.error(
                        "Rust installation name should contain only alphanumeric, underscore, and hyphen characters");
            }

            return FormValidation.ok();
        }

        /**
         * Get a specific installation by name.
         *
         * @param name The name of the installation
         * @return The installation, or null if not found
         */
        public RustInstallation getInstallation(String name) {
            for (RustInstallation installation : getInstallations()) {
                if (installation.getName().equals(name)) {
                    return installation;
                }
            }
            return null;
        }
    }
}
