package io.jenkins.plugins.rust;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles Rust version detection and installation via rustup.
 * Provides robust error handling, retry logic, and progress logging.
 */
public class RustInstaller {
    private static final Logger LOGGER = Logger.getLogger(RustInstaller.class.getName());

    private static final String RUSTUP_INSTALLER_URL = "https://sh.rustup.rs";
    private static final String RUSTUP_INSTALLER_WINDOWS_URL = "https://win.rustup.rs/x86_64";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    /**
     * Get the installed Cargo version.
     *
     * @param rustHome Rust installation home directory
     * @return Version string, or null if not found
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If interrupted while waiting for process
     */
    public static String getCargoVersion(File rustHome) throws IOException, InterruptedException {
        if (!rustHome.exists() || !rustHome.isDirectory()) {
            LOGGER.log(Level.FINE, "Rust home does not exist or is not a directory: {0}", rustHome);
            return null;
        }

        File cargoBin = getCargoExecutable(rustHome);
        if (cargoBin == null || !cargoBin.exists()) {
            LOGGER.log(Level.FINE, "Cargo binary not found in: {0}", rustHome);
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(cargoBin.getAbsolutePath(), "--version");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = IOUtils.toString(process.getInputStream(), "UTF-8");
            int exitCode = process.waitFor();

            if (exitCode == 0 && output != null) {
                // Parse version from output like "cargo 1.75.0 (1bc8dbc342 2023-12-07)"
                String[] parts = output.trim().split("\\s+");
                if (parts.length >= 2) {
                    return parts[1];
                }
            } else {
                LOGGER.log(Level.WARNING, "cargo --version failed with exit code {0}: {1}",
                        new Object[] { exitCode, output });
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Error getting Cargo version", e);
            throw e;
        }

        return null;
    }

    /**
     * Get the installed Rust version.
     *
     * @param rustHome Rust installation home directory
     * @return Version string, or null if not found
     * @throws IOException          If an I/O error occurs
     * @throws InterruptedException If interrupted while waiting for process
     */
    public static String getRustVersion(File rustHome) throws IOException, InterruptedException {
        if (!rustHome.exists() || !rustHome.isDirectory()) {
            LOGGER.log(Level.FINE, "Rust home does not exist or is not a directory: {0}", rustHome);
            return null;
        }

        File rustcBin = getRustcExecutable(rustHome);
        if (rustcBin == null || !rustcBin.exists()) {
            LOGGER.log(Level.FINE, "rustc binary not found in: {0}", rustHome);
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(rustcBin.getAbsolutePath(), "--version");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = IOUtils.toString(process.getInputStream(), "UTF-8");
            int exitCode = process.waitFor();

            if (exitCode == 0 && output != null) {
                // Parse version from output like "rustc 1.75.0 (82e1608df 2023-12-21)"
                String[] parts = output.trim().split("\\s+");
                if (parts.length >= 2) {
                    return parts[1];
                }
            } else {
                LOGGER.log(Level.WARNING, "rustc --version failed with exit code {0}: {1}",
                        new Object[] { exitCode, output });
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Error getting Rust version", e);
            throw e;
        }

        return null;
    }

    /**
     * Verify Rust installation in directory.
     *
     * @param rustHome Rust installation home directory
     * @return true if installation is valid
     */
    public static boolean verifyInstallation(File rustHome) {
        if (!rustHome.exists() || !rustHome.isDirectory()) {
            LOGGER.log(Level.FINE, "Rust home does not exist or is not a directory: {0}", rustHome);
            return false;
        }

        File cargoBin = getCargoExecutable(rustHome);
        File rustcBin = getRustcExecutable(rustHome);

        boolean cargoExists = cargoBin != null && cargoBin.exists() && cargoBin.canExecute();
        boolean rustcExists = rustcBin != null && rustcBin.exists() && rustcBin.canExecute();

        if (!cargoExists) {
            LOGGER.log(Level.FINE, "cargo binary not found or not executable: {0}", cargoBin);
        }
        if (!rustcExists) {
            LOGGER.log(Level.FINE, "rustc binary not found or not executable: {0}", rustcBin);
        }

        return cargoExists && rustcExists;
    }

    /**
     * Check if Cargo is available in system PATH.
     *
     * @return true if cargo is in PATH
     */
    public static boolean isCargoInPath() {
        return isCommandInPath("cargo");
    }

    /**
     * Check if rustup is available in system PATH.
     *
     * @return true if rustup is in PATH
     */
    public static boolean isRustupInPath() {
        return isCommandInPath("rustup");
    }

    /**
     * Check if a command is available in the system PATH.
     *
     * @param command Command name to check
     * @return true if command is in PATH
     */
    private static boolean isCommandInPath(String command) {
        String checkCommand = isWindows() ? "where " + command : "which " + command;
        ProcessBuilder pb = new ProcessBuilder(
                isWindows() ? new String[] { "cmd", "/c", checkCommand } : new String[] { "sh", "-c", checkCommand });
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            LOGGER.log(Level.FINE, "Error checking for " + command + " in PATH", e);
            return false;
        }
    }

    /**
     * Get the installation path for a specific Rust version.
     *
     * @param toolsDirectory Base tools directory
     * @param version        Rust version
     * @return Installation path
     */
    public static File getInstallationPath(File toolsDirectory, String version) {
        return new File(toolsDirectory, "rust-" + version);
    }

    /**
     * Install rustup if not present.
     *
     * @param destination Installation destination directory
     * @throws IOException          If installation fails
     * @throws InterruptedException If interrupted during installation
     */
    public static void installRustup(File destination) throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Installing rustup to: {0}", destination.getAbsolutePath());

        if (!destination.exists() && !destination.mkdirs()) {
            throw new IOException("Failed to create destination directory: " + destination);
        }

        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                LOGGER.log(Level.INFO, "Installation attempt {0} of {1}", new Object[] { attempt, MAX_RETRIES });

                if (isWindows()) {
                    installRustupWindows(destination);
                } else {
                    installRustupUnix(destination);
                }

                LOGGER.info("rustup installation completed successfully");
                return;

            } catch (IOException e) {
                lastException = e;
                LOGGER.log(Level.WARNING, "Installation attempt " + attempt + " failed: " + e.getMessage(), e);

                if (attempt < MAX_RETRIES) {
                    LOGGER.log(Level.INFO, "Retrying in {0}ms...", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }

        throw new IOException("Failed to install rustup after " + MAX_RETRIES + " attempts", lastException);
    }

    /**
     * Install rustup on Unix-like systems (Linux, macOS).
     */
    private static void installRustupUnix(File destination) throws IOException, InterruptedException {
        LOGGER.info("Installing rustup for Unix-like system");

        File installer = new File(destination, "rustup-init.sh");

        // Download rustup-init.sh
        LOGGER.log(Level.INFO, "Downloading rustup installer from: {0}", RUSTUP_INSTALLER_URL);
        ProcessBuilder downloadPb = new ProcessBuilder(
                "curl", "--proto", "=https", "--tlsv1.2", "-sSf", RUSTUP_INSTALLER_URL,
                "-o", installer.getAbsolutePath());
        downloadPb.redirectErrorStream(true);

        Process downloadProcess = downloadPb.start();
        String downloadOutput = IOUtils.toString(downloadProcess.getInputStream(), "UTF-8");
        int downloadExitCode = downloadProcess.waitFor();

        if (downloadExitCode != 0) {
            throw new IOException("Failed to download rustup installer. Exit code: " +
                    downloadExitCode + ", Output: " + downloadOutput);
        }

        // Make executable
        try {
            Set<PosixFilePermission> perms = EnumSet.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.GROUP_EXECUTE,
                    PosixFilePermission.OTHERS_READ,
                    PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(installer.toPath(), perms);
            LOGGER.fine("Set executable permissions on installer");
        } catch (UnsupportedOperationException e) {
            LOGGER.log(Level.FINE, "Could not set POSIX permissions (filesystem doesn't support it)", e);
        }

        // Run installer with -y flag for non-interactive mode
        LOGGER.info("Running rustup installer");
        ProcessBuilder installPb = new ProcessBuilder(
                "sh", installer.getAbsolutePath(), "-y", "--default-toolchain", "none", "--no-modify-path");
        installPb.environment().put("CARGO_HOME", destination.getAbsolutePath());
        installPb.environment().put("RUSTUP_HOME", new File(destination, "rustup").getAbsolutePath());
        installPb.redirectErrorStream(true);

        Process installProcess = installPb.start();
        String installOutput = IOUtils.toString(installProcess.getInputStream(), "UTF-8");
        int installExitCode = installProcess.waitFor();

        if (installExitCode != 0) {
            throw new IOException("Failed to install rustup. Exit code: " +
                    installExitCode + ", Output: " + installOutput);
        }

        LOGGER.log(Level.FINE, "rustup installation output: {0}", installOutput);
    }

    /**
     * Install rustup on Windows.
     */
    private static void installRustupWindows(File destination) throws IOException, InterruptedException {
        LOGGER.info("Installing rustup for Windows");

        File installer = new File(destination, "rustup-init.exe");

        // Download rustup-init.exe
        LOGGER.log(Level.INFO, "Downloading rustup installer from: {0}", RUSTUP_INSTALLER_WINDOWS_URL);
        ProcessBuilder downloadPb = new ProcessBuilder(
                "powershell", "-NoProfile", "-Command",
                "Invoke-WebRequest -Uri '" + RUSTUP_INSTALLER_WINDOWS_URL + "' -OutFile '" +
                        installer.getAbsolutePath() + "' -UseBasicParsing");
        downloadPb.redirectErrorStream(true);

        Process downloadProcess = downloadPb.start();
        String downloadOutput = IOUtils.toString(downloadProcess.getInputStream(), "UTF-8");
        int downloadExitCode = downloadProcess.waitFor();

        if (downloadExitCode != 0) {
            throw new IOException("Failed to download rustup installer. Exit code: " +
                    downloadExitCode + ", Output: " + downloadOutput);
        }

        // Run installer with -y flag for non-interactive mode
        LOGGER.info("Running rustup installer");
        ProcessBuilder installPb = new ProcessBuilder(
                installer.getAbsolutePath(), "-y", "--default-toolchain", "none", "--no-modify-path");
        installPb.environment().put("CARGO_HOME", destination.getAbsolutePath());
        installPb.environment().put("RUSTUP_HOME", new File(destination, "rustup").getAbsolutePath());
        installPb.redirectErrorStream(true);

        Process installProcess = installPb.start();
        String installOutput = IOUtils.toString(installProcess.getInputStream(), "UTF-8");
        int installExitCode = installProcess.waitFor();

        if (installExitCode != 0) {
            throw new IOException("Failed to install rustup. Exit code: " +
                    installExitCode + ", Output: " + installOutput);
        }

        LOGGER.log(Level.FINE, "rustup installation output: {0}", installOutput);
    }

    /**
     * Install a specific Rust toolchain version using rustup.
     *
     * @param rustHome Rust installation home directory
     * @param version  Toolchain version (e.g., "stable", "1.75.0", "nightly")
     * @throws IOException          If installation fails
     * @throws InterruptedException If interrupted during installation
     */
    public static void installToolchain(File rustHome, String version) throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Installing Rust toolchain {0} to: {1}",
                new Object[] { version, rustHome.getAbsolutePath() });

        String rustupBin = getRustupBinary(rustHome);
        if (rustupBin == null) {
            throw new IOException("rustup binary not found. Please install rustup first.");
        }

        // Install toolchain
        LOGGER.log(Level.INFO, "Running: rustup toolchain install {0}", version);
        ProcessBuilder pb = new ProcessBuilder(rustupBin, "toolchain", "install", version);
        pb.environment().put("CARGO_HOME", rustHome.getAbsolutePath());
        pb.environment().put("RUSTUP_HOME", new File(rustHome, "rustup").getAbsolutePath());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        String output = IOUtils.toString(process.getInputStream(), "UTF-8");
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException("Failed to install Rust toolchain " + version +
                    ". Exit code: " + exitCode + ", Output: " + output);
        }

        LOGGER.log(Level.FINE, "Toolchain installation output: {0}", output);

        // Set as default
        LOGGER.log(Level.INFO, "Setting {0} as default toolchain", version);
        ProcessBuilder defaultPb = new ProcessBuilder(rustupBin, "default", version);
        defaultPb.environment().put("CARGO_HOME", rustHome.getAbsolutePath());
        defaultPb.environment().put("RUSTUP_HOME", new File(rustHome, "rustup").getAbsolutePath());
        defaultPb.redirectErrorStream(true);

        Process defaultProcess = defaultPb.start();
        String defaultOutput = IOUtils.toString(defaultProcess.getInputStream(), "UTF-8");
        int defaultExitCode = defaultProcess.waitFor();

        if (defaultExitCode != 0) {
            LOGGER.log(Level.WARNING, "Failed to set default toolchain, but installation succeeded. Output: {0}",
                    defaultOutput);
        } else {
            LOGGER.log(Level.FINE, "Default toolchain output: {0}", defaultOutput);
        }
    }

    /**
     * Get the cargo executable file.
     */
    private static File getCargoExecutable(File rustHome) {
        String cargoName = isWindows() ? "cargo.exe" : "cargo";
        File cargoBin = new File(rustHome, "bin" + File.separator + cargoName);

        if (!cargoBin.exists()) {
            // Try alternative location
            cargoBin = new File(rustHome, "cargo" + File.separator + "bin" + File.separator + cargoName);
        }

        return cargoBin.exists() ? cargoBin : null;
    }

    /**
     * Get the rustc executable file.
     */
    private static File getRustcExecutable(File rustHome) {
        String rustcName = isWindows() ? "rustc.exe" : "rustc";
        File rustcBin = new File(rustHome, "bin" + File.separator + rustcName);

        if (!rustcBin.exists()) {
            // Try alternative location
            rustcBin = new File(rustHome, "rustc" + File.separator + "bin" + File.separator + rustcName);
        }

        return rustcBin.exists() ? rustcBin : null;
    }

    /**
     * Get the rustup binary path.
     *
     * @return Path to rustup binary, or null if not found
     */
    private static String getRustupBinary(File rustHome) {
        String rustupName = isWindows() ? "rustup.exe" : "rustup";
        File rustupBin = new File(rustHome, "bin" + File.separator + rustupName);

        if (rustupBin.exists()) {
            return rustupBin.getAbsolutePath();
        }

        // Check if rustup is in PATH
        if (isRustupInPath()) {
            return "rustup";
        }

        return null;
    }

    /**
     * Check if running on Windows.
     *
     * @return true if running on Windows
     */
    private static boolean isWindows() {
        return File.pathSeparatorChar == ';';
    }
}
