package io.jenkins.plugins.rust;

import hudson.Extension;
import hudson.FilePath;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolInstaller;
import hudson.tools.ToolInstallerDescriptor;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tool installer for Rust that handles version management and installation.
 * Supports automatic download and installation of Rust via rustup.
 */
public class RustToolInstaller extends ToolInstaller {
    private static final Logger LOGGER = Logger.getLogger(RustToolInstaller.class.getName());

    private final String version;
    private final boolean preferBuiltInTools;
    private final boolean installRustup;

    /**
     * Constructor for RustToolInstaller.
     *
     * @param label              Label for this installer (can be null)
     * @param version            Rust version to install (e.g., "stable", "1.75.0",
     *                           "nightly")
     * @param preferBuiltInTools If true, prefer system-installed Rust if available
     * @param installRustup      If true, install rustup if not present
     */
    @DataBoundConstructor
    public RustToolInstaller(String label, String version, boolean preferBuiltInTools, boolean installRustup) {
        super(label);
        this.version = version != null ? version : "stable";
        this.preferBuiltInTools = preferBuiltInTools;
        this.installRustup = installRustup;
    }

    public String getVersion() {
        return version;
    }

    public boolean isPreferBuiltInTools() {
        return preferBuiltInTools;
    }

    public boolean isInstallRustup() {
        return installRustup;
    }

    @Override
    public FilePath performInstallation(ToolInstallation tool, Node node, TaskListener log)
            throws IOException, InterruptedException {

        FilePath toolPath = preferredLocation(tool, node);
        File toolHome = new File(toolPath.getRemote());

        log.getLogger().println("Installing Rust " + version + " to " + toolHome.getAbsolutePath());

        // If tool is already installed, verify and return
        if (toolHome.exists() && RustInstaller.verifyInstallation(toolHome)) {
            try {
                String installedVersion = RustInstaller.getCargoVersion(toolHome);
                if (installedVersion != null) {
                    log.getLogger().println("Rust " + installedVersion + " is already installed");
                } else {
                    log.getLogger().println("Rust " + version + " is already installed");
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to get installed version", e);
                log.getLogger().println("Rust " + version + " is already installed");
            }
            return toolPath;
        }

        // Check if we should use system Rust/Cargo
        if (preferBuiltInTools && RustInstaller.isCargoInPath()) {
            log.getLogger().println("Using system Rust/Cargo installation");
            // Note: When using system tools, we still return the tool path
            // but the actual binaries will be found via PATH
            return toolPath;
        }

        // Ensure directory exists
        if (!toolHome.exists() && !toolHome.mkdirs()) {
            throw new IOException("Failed to create tool directory: " + toolHome);
        }

        // Ensure rustup is installed if requested
        if (installRustup && !RustInstaller.isRustupInPath()) {
            log.getLogger().println("Installing rustup...");
            RustInstaller.installRustup(toolHome);
            log.getLogger().println("rustup installed successfully");
        }

        // Install the specific toolchain version
        if (installRustup || RustInstaller.isRustupInPath()) {
            log.getLogger().println("Installing Rust toolchain " + version + "...");
            RustInstaller.installToolchain(toolHome, version);
            log.getLogger().println("Rust toolchain " + version + " installed successfully");
        } else {
            String warning = "rustup not available and installRustup is false. " +
                    "Installation may fail or use system Rust if available.";
            log.getLogger().println("WARNING: " + warning);
            LOGGER.warning(warning);
        }

        // Verify installation
        log.getLogger().println("Verifying installation...");
        if (!RustInstaller.verifyInstallation(toolHome)) {
            throw new IOException("Rust installation verification failed. " +
                    "cargo or rustc binary not found in: " + toolHome);
        }

        try {
            String installedVersion = RustInstaller.getCargoVersion(toolHome);
            if (installedVersion != null) {
                log.getLogger().println("Successfully installed Rust " + installedVersion);
            } else {
                log.getLogger().println("Successfully installed Rust (version detection unavailable)");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get installed version", e);
            log.getLogger().println("Successfully installed Rust (version check failed: " + e.getMessage() + ")");
        }

        return toolPath;
    }

    /**
     * Descriptor for RustToolInstaller.
     */
    @Extension
    @Symbol("rustInstaller")
    public static class DescriptorImpl extends ToolInstallerDescriptor<RustToolInstaller> {

        @Override
        public String getDisplayName() {
            return "Install Rust via rustup";
        }

        @Override
        public boolean isApplicable(Class<? extends ToolInstallation> toolType) {
            return toolType == RustInstallation.class;
        }

        /**
         * Validate version string.
         */
        @POST
        public FormValidation doCheckVersion(@QueryParameter String value) {
            Jenkins.get().checkPermission(Jenkins.ADMINISTER);

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
            errorMsg.append("Invalid Rust version format: '").append(trimmedValue).append(".\n\n");
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
