package io.jenkins.plugins.rust;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.FormValidation;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A tool installer for Rust that automates the download and installation of Rust
 * toolchains via rustup. This class supports different versions, including channels
 * (stable, beta, nightly) and specific version numbers.
 */
public class RustToolInstaller extends ToolInstaller {

    private static final Logger LOGGER = Logger.getLogger(RustToolInstaller.class.getName());

    private final String version;
    private final boolean preferBuiltInTools;
    private final boolean installRustup;

    /**
     * Constructs a new {@code RustToolInstaller}.
     *
     * @param label              The label for this installer (can be null).
     * @param version            The Rust version to install (e.g., "stable", "1.75.0", "nightly").
     * @param preferBuiltInTools If {@code true}, the installer will prefer a system-installed
     *                           Rust if it is available in the PATH.
     * @param installRustup      If {@code true}, the installer will attempt to install rustup
     *                           if it is not already present.
     */
    @DataBoundConstructor
    public RustToolInstaller(String label, String version, boolean preferBuiltInTools, boolean installRustup) {
        super(label);
        this.version = version != null ? version : "stable";
        this.preferBuiltInTools = preferBuiltInTools;
        this.installRustup = installRustup;
    }

    /** Returns the configured Rust version. */
    public String getVersion() {
        return version;
    }

    /** Returns whether to prefer a system-installed Rust. */
    public boolean isPreferBuiltInTools() {
        return preferBuiltInTools;
    }

    /** Returns whether to install rustup if it's not present. */
    public boolean isInstallRustup() {
        return installRustup;
    }

    /**
     * Performs the installation of the Rust toolchain. This method is called by Jenkins
     * when a tool needs to be installed on a node.
     *
     * @param tool The tool to be installed.
     * @param node The node on which to install the tool.
     * @param log  A listener for logging the installation progress.
     * @return The path to the installed tool.
     * @throws IOException If an I/O error occurs during installation.
     * @throws InterruptedException If the installation is interrupted.
     */
    @Override
    public FilePath performInstallation(@Nonnull ToolInstallation tool, @Nonnull Node node, @Nonnull TaskListener log)
            throws IOException, InterruptedException {

        FilePath toolPath = preferredLocation(tool, node);
        File toolHome = new File(toolPath.getRemote());

        log.getLogger().println("Installing Rust " + version + " to " + toolHome.getAbsolutePath());

        // If the tool is already installed and verified, skip the installation
        if (toolHome.exists() && RustInstaller.verifyInstallation(toolHome)) {
            logVersion(toolHome, log);
            return toolPath;
        }

        // If configured to prefer built-in tools and cargo is in the PATH, use it
        if (preferBuiltInTools && RustInstaller.isCargoInPath()) {
            log.getLogger().println("Using system-installed Rust/Cargo from PATH.");
            return toolPath;
        }

        // Ensure the installation directory exists
        if (!toolHome.exists() && !toolHome.mkdirs()) {
            throw new IOException("Failed to create tool installation directory: " + toolHome);
        }

        // Install rustup if it's requested and not already available
        if (installRustup && !RustInstaller.isRustupInPath()) {
            log.getLogger().println("rustup not found. Installing rustup...");
            RustInstaller.installRustup(toolHome);
            log.getLogger().println("rustup installed successfully.");
        }

        // Install the specified toolchain version
        if (installRustup || RustInstaller.isRustupInPath()) {
            log.getLogger().println("Installing Rust toolchain: " + version + "...");
            RustInstaller.installToolchain(toolHome, version);
            log.getLogger().println("Rust toolchain " + version + " installed successfully.");
        } else {
            String warning = "rustup is not available and 'installRustup' is not checked. "
                    + "The installation may fail or default to a system-installed Rust if available.";
            log.getLogger().println("WARNING: " + warning);
            LOGGER.warning(warning);
        }

        // Verify the installation
        log.getLogger().println("Verifying installation...");
        if (!RustInstaller.verifyInstallation(toolHome)) {
            throw new IOException("Rust installation verification failed. "
                    + "Could not find cargo or rustc binaries in: " + toolHome);
        }
        log.getLogger().println("Installation verified successfully.");

        // Log the final installed version
        logVersion(toolHome, log);

        return toolPath;
    }

    /**
     * Helper method to log the installed Rust version.
     */
    private void logVersion(File toolHome, TaskListener log) {
        try {
            String installedVersion = RustInstaller.getCargoVersion(toolHome);
            if (installedVersion != null) {
                log.getLogger().println("Successfully installed Rust " + installedVersion);
            } else {
                log.getLogger().println("Successfully installed Rust (version detection unavailable).");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get installed Rust version", e);
            log.getLogger().println("Successfully installed Rust (version check failed: " + e.getMessage() + ")");
        }
    }

    /**
     * Descriptor for {@link RustToolInstaller}. This class provides metadata for the
     * installer and handles UI validation.
     */
    @Extension
    @Symbol("rustInstaller")
    public static class DescriptorImpl extends ToolInstallerDescriptor<RustToolInstaller> {

        /**
         * Returns the display name for this installer, which is shown in the Jenkins UI.
         *
         * @return The display name.
         */
        @Nonnull
        @Override
        public String getDisplayName() {
            return "Install Rust via rustup";
        }

        /**
         * Indicates whether this installer is applicable to the given tool type.
         *
         * @param toolType The type of tool to check.
         * @return {@code true} if the tool is a {@link RustInstallation}, {@code false} otherwise.
         */
        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == RustInstallation.class;
        }

        /**
         * Performs validation on the Rust version field in the UI.
         *
         * @param value The version string entered by the user.
         * @return A {@link FormValidation} object indicating the result of the validation.
         */
        public FormValidation doCheckVersion(@QueryParameter String value) {
            if (value == null || value.trim().isEmpty()) {
                return FormValidation.error(
                        "Version cannot be empty. Please specify 'stable', 'beta', 'nightly', or a specific version like '1.75.0'");
            }

            String trimmedValue = value.trim();

            // Check for valid channel names
            if (trimmedValue.equals("stable") || trimmedValue.equals("beta") || trimmedValue.equals("nightly")) {
                return FormValidation.ok("Using Rust channel: " + trimmedValue);
            }

            // Check for version number format (e.g., 1.75.0)
            if (trimmedValue.matches("^\\d+\\.\\d+(\\.\\d+)?$")) {
                return FormValidation.ok("Using specific Rust version: " + trimmedValue);
            }

            // Check for version with date (e.g., nightly-2024-01-01)
            // Stricter regex for months 01-12 and days 01-31
            if (trimmedValue.matches("^(stable|beta|nightly)-\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$")) {
                return FormValidation.ok("Using dated toolchain: " + trimmedValue);
            }

            // Provide helpful error message
            StringBuilder errorMsg = new StringBuilder();
            errorMsg.append("Invalid Rust version format: '").append(trimmedValue).append("'.\n\n");
            errorMsg.append("Valid formats:\n");
            errorMsg.append("  • Channels: stable, beta, nightly\n");
            errorMsg.append("  • Version numbers: 1.75.0, 1.76, 1.80.1\n");
            errorMsg.append("  • Dated toolchains: nightly-2024-01-15, beta-2024-02-01\n\n");

            // Give specific suggestions based on what they entered
            if (trimmedValue.contains("/") || trimmedValue.contains("\\")) {
                errorMsg.append("Tip: Don't include paths - just the version identifier");
            } else if (trimmedValue.toLowerCase().contains("rust")) {
                errorMsg.append("Tip: Don't include 'rust' in the version - just use '")
                        .append(trimmedValue.replace("rust", "").replace("Rust", "").trim()).append("'");
            } else if (trimmedValue.matches(".*[a-zA-Z].*") && !trimmedValue.matches("^(stable|beta|nightly).*")) {
                errorMsg.append(
                        "Tip: Use 'stable', 'beta', or 'nightly' for channel names, or a numeric version like '1.75.0'");
            } else {
                errorMsg.append("Examples: stable, 1.75.0, nightly-2024-01-15");
            }

            return FormValidation.error(errorMsg.toString());
        }
    }
}
