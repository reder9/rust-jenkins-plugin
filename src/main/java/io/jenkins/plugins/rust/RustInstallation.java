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

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Represents a Rust tool installation, which is a specific version of the Rust
 * toolchain (including rustc, cargo, and rustup). This class supports multiple
 * installations on different nodes and allows for environment variable expansion
 * in the installation path.
 *
 * @author RederSoft
 */
public class RustInstallation extends ToolInstallation
        implements NodeSpecific<RustInstallation>, EnvironmentSpecific<RustInstallation> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(RustInstallation.class.getName());

    /**
     * Constructs a new {@code RustInstallation}.
     *
     * @param name       The user-defined name for this installation (e.g., "Rust 1.75").
     * @param home       The path to the Rust installation directory (typically CARGO_HOME).
     * @param properties A list of tool properties for this installation.
     */
    @DataBoundConstructor
    public RustInstallation(String name, String home, List<? extends ToolProperty<?>> properties) {
        super(name, home, properties);
    }

    /**
     * Retrieves all globally configured Rust installations.
     *
     * @return An array of all configured {@link RustInstallation}s. Returns an empty
     *         array if Jenkins is not running or if the descriptor is not found.
     */
    public static RustInstallation[] allInstallations() {
        Jenkins instance = Jenkins.getInstanceOrNull();
        if (instance != null) {
            DescriptorImpl descriptor = instance.getDescriptorByType(DescriptorImpl.class);
            if (descriptor != null) {
                return descriptor.getInstallations();
            }
        }
        return new RustInstallation[0];
    }

    /**
     * Constructs the full path to the {@code cargo} binary for the current operating system.
     *
     * @return The absolute path to the {@code cargo} executable.
     */
    public String getCargo() {
        return getBinaryPath("cargo");
    }

    /**
     * Constructs the full path to the {@code rustc} binary for the current operating system.
     *
     * @return The absolute path to the {@code rustc} executable.
     */
    public String getRustc() {
        return getBinaryPath("rustc");
    }

    /**
     * Constructs the full path to the {@code rustup} binary for the current operating system.
     *
     * @return The absolute path to the {@code rustup} executable.
     */
    public String getRustup() {
        return getBinaryPath("rustup");
    }

    /**
     * Helper method to construct the path to a binary within the installation.
     *
     * @param binaryName The name of the binary (e.g., "cargo").
     * @return The full path to the binary, including the ".exe" suffix on Windows.
     */
    private String getBinaryPath(String binaryName) {
        String binDir = getHome() + File.separator + "bin" + File.separator;
        return binDir + (isWindows() ? binaryName + ".exe" : binaryName);
    }

    /**
     * Checks if the current operating system is Windows.
     *
     * @return {@code true} if the OS is Windows, {@code false} otherwise.
     */
    private boolean isWindows() {
        return File.pathSeparatorChar == ';';
    }

    /**
     * Creates a node-specific instance of this installation. This is called when the
     * installation is used on a specific agent node, allowing for path translation.
     *
     * @param node The node on which this installation is being used.
     * @param log  A listener for logging.
     * @return A new {@link RustInstallation} with the translated path.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the build is interrupted.
     */
    @Override
    public RustInstallation forNode(@Nonnull Node node, TaskListener log) throws IOException, InterruptedException {
        String translatedHome = translateFor(node, log);
        LOGGER.log(Level.FINE, "Translated Rust home for node {0}: {1}", new Object[]{node.getDisplayName(), translatedHome});
        return new RustInstallation(getName(), translatedHome, Collections.emptyList());
    }

    /**
     * Creates an environment-specific instance of this installation. This is used to
     * expand any environment variables present in the installation path.
     *
     * @param environment The environment variables to use for expansion.
     * @return A new {@link RustInstallation} with the expanded path.
     */
    @Override
    public RustInstallation forEnvironment(@Nonnull EnvVars environment) {
        String expandedHome = environment.expand(getHome());
        LOGGER.log(Level.FINE, "Expanded Rust home with environment variables: {0}", expandedHome);
        return new RustInstallation(getName(), expandedHome, Collections.emptyList());
    }

    /**
     * Contributes environment variables to a build using this installation. This method
     * sets CARGO_HOME, RUSTUP_HOME, and prepends the Rust bin directory to the PATH.
     *
     * @param env The environment variables map to be modified.
     */
    @Override
    public void buildEnvVars(EnvVars env) {
        String home = getHome();
        if (home == null || home.isEmpty()) {
            LOGGER.warning("Rust installation home is not set, cannot contribute environment variables.");
            return;
        }

        // Set CARGO_HOME and RUSTUP_HOME
        env.put("CARGO_HOME", home);
        env.put("RUSTUP_HOME", home + File.separator + "rustup");

        // Add the Rust bin directory to the PATH
        String bin = home + File.separator + "bin";
        String path = env.get("PATH");
        if (path != null && !path.isEmpty()) {
            env.put("PATH", bin + File.pathSeparator + path);
        } else {
            env.put("PATH", bin);
        }
        LOGGER.log(Level.FINE, "Injected Rust environment variables: CARGO_HOME={0}, RUSTUP_HOME={1}, PATH={2}",
                new Object[]{env.get("CARGO_HOME"), env.get("RUSTUP_HOME"), env.get("PATH")});
    }

    /**
     * Returns the descriptor for this class.
     *
     * @return The {@link DescriptorImpl} instance.
     * @throws AssertionError if the Jenkins instance is not running.
     */
    @Override
    public DescriptorImpl getDescriptor() {
        Jenkins jenkinsInstance = Jenkins.getInstanceOrNull();
        if (jenkinsInstance == null) {
            throw new AssertionError("Jenkins instance is null, cannot retrieve descriptor");
        }
        return (DescriptorImpl) jenkinsInstance.getDescriptorOrDie(getClass());
    }

    /**
     * Descriptor for {@link RustInstallation}. This class provides metadata for the
     * tool installation and handles UI validation.
     */
    @Extension
    @Symbol("rust")
    public static class DescriptorImpl extends ToolDescriptor<RustInstallation> {

        /**
         * Default constructor. Loads the descriptor's configuration from disk.
         */
        public DescriptorImpl() {
            super();
            load();
        }

        /**
         * Returns the display name for this tool, which is shown in the Jenkins UI.
         *
         * @return The display name.
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Rust";
        }

        /**
         * Performs validation on the Rust installation home directory field in the UI.
         *
         * @param value The path entered by the user.
         * @return A {@link FormValidation} object indicating the result of the validation.
         */
        public FormValidation doCheckHome(@QueryParameter final String value) {
            Jenkins jenkins = Jenkins.getInstanceOrNull();
            if (jenkins != null) {
                jenkins.checkPermission(Jenkins.ADMINISTER);
            }

            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("Please specify a Rust installation directory.");
            }

            File home = new File(value);
            if (!home.exists()) {
                return FormValidation.warning("The specified directory does not exist: " + value);
            }

            if (!home.isDirectory()) {
                return FormValidation.error("The specified path is not a directory: " + value);
            }

            if (!RustInstaller.verifyInstallation(home)) {
                return FormValidation.warning(
                        "Could not find rustc or cargo binaries in the specified directory: "
                        + new File(home, "bin").getPath());
            }

            try {
                String version = RustInstaller.getCargoVersion(home);
                if (version != null) {
                    return FormValidation.ok("Successfully verified Rust/Cargo " + version);
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Could not determine Rust version", e);
                return FormValidation.warning("Could not determine Rust version: " + e.getMessage());
            }

            return FormValidation.ok();
        }

        /**
         * Performs validation on the Rust installation name field in the UI.
         *
         * @param value The name entered by the user.
         * @return A {@link FormValidation} object indicating the result of the validation.
         */
        public FormValidation doCheckName(@QueryParameter final String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error("The Rust installation name cannot be empty.");
            }

            if (!value.matches("^[a-zA-Z0-9_.-]+$")) {
                return FormValidation.error(
                    "The installation name should only contain alphanumeric characters, underscores, hyphens, and periods.");
            }

            return FormValidation.ok();
        }
    }
}
