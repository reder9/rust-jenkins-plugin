package io.jenkins.plugins.rust;

import org.apache.commons.io.IOUtils;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles the installation of Rust toolchains, including rustup and specific
 * versions of Rust. This class provides methods for version detection,
 * installation verification, and robust error handling with retry logic.
 */
public class RustInstaller {

    private static final Logger LOGGER = Logger.getLogger(RustInstaller.class.getName());

    private static final String RUSTUP_INSTALLER_URL = "https://sh.rustup.rs";
    private static final String RUSTUP_INSTALLER_WINDOWS_URL = "https://win.rustup.rs/x86_64";
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;

    /**
     * Retrieves the installed Cargo version from a given Rust installation directory.
     *
     * @param rustHome The home directory of the Rust installation.
     * @return The version string (e.g., "1.75.0"), or {@code null} if the version
     *         cannot be determined.
     * @throws IOException If an I/O error occurs while running the command.
     * @throws InterruptedException If the command execution is interrupted.
     */
    public static String getCargoVersion(@Nonnull File rustHome) throws IOException, InterruptedException {
        return getVersion(rustHome, "cargo");
    }

    /**
     * Retrieves the installed Rust compiler (rustc) version from a given Rust
     * installation directory.
     *
     * @param rustHome The home directory of the Rust installation.
     * @return The version string (e.g., "1.75.0"), or {@code null} if the version
     *         cannot be determined.
     * @throws IOException If an I/O error occurs while running the command.
     * @throws InterruptedException If the command execution is interrupted.
     */
    public static String getRustVersion(@Nonnull File rustHome) throws IOException, InterruptedException {
        return getVersion(rustHome, "rustc");
    }

    /**
     * A generic method to retrieve the version of a Rust binary (cargo or rustc).
     *
     * @param rustHome The home directory of the Rust installation.
     * @param binaryName The name of the binary ("cargo" or "rustc").
     * @return The version string, or {@code null} if not found.
     * @throws IOException If an I/O error occurs.
     * @throws InterruptedException If the command execution is interrupted.
     */
    private static String getVersion(@Nonnull File rustHome, @Nonnull String binaryName)
            throws IOException, InterruptedException {
        if (!rustHome.isDirectory()) {
            LOGGER.log(Level.FINE, "{0} home is not a directory: {1}", new Object[]{binaryName, rustHome});
            return null;
        }

        File executable = getExecutable(rustHome, binaryName);
        if (executable == null || !executable.exists()) {
            LOGGER.log(Level.FINE, "{0} binary not found in: {1}", new Object[]{binaryName, rustHome});
            return null;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(executable.getAbsolutePath(), "--version");
            pb.redirectErrorStream(true);

            Process process = pb.start();
            String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();

            if (exitCode == 0 && output != null) {
                // Parses version from output like "cargo 1.75.0 (1bc8dbc342 2023-12-07)"
                // or "rustc 1.75.0 (82e1608df 2023-12-21)"
                String[] parts = output.trim().split("\\s+");
                if (parts.length >= 2) {
                    LOGGER.log(Level.FINE, "Found {0} version: {1}", new Object[]{binaryName, parts[1]});
                    return parts[1];
                }
            } else {
                LOGGER.log(Level.WARNING, "{0} --version failed with exit code {1}: {2}",
                        new Object[]{binaryName, exitCode, output});
            }
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Error getting " + binaryName + " version", e);
            throw e;
        }

        return null;
    }

    /**
     * Verifies that a Rust installation is valid by checking for the presence and
     * executability of the {@code cargo} and {@code rustc} binaries.
     *
     * @param rustHome The home directory of the Rust installation.
     * @return {@code true} if the installation is valid, {@code false} otherwise.
     */
    public static boolean verifyInstallation(@Nonnull File rustHome) {
        if (!rustHome.isDirectory()) {
            LOGGER.log(Level.FINE, "Rust home is not a directory: {0}", rustHome);
            return false;
        }

        File cargoBin = getExecutable(rustHome, "cargo");
        File rustcBin = getExecutable(rustHome, "rustc");

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
     * Checks if cargo is available in the system PATH.
     *
     * @return true if cargo is in PATH
     */
    public static boolean isCargoInPath() {
        return isCommandInPath("cargo");
    }

    /**
     * Checks if rustup is available in the system PATH.
     *
     * @return true if rustup is in PATH
     */
    public static boolean isRustupInPath() {
        return isCommandInPath("rustup");
    }

    /**
     * Checks if a command is available in the system's PATH.
     *
     * @param command The name of the command to check (e.g., "cargo").
     * @return {@code true} if the command is found in the PATH, {@code false} otherwise.
     */
    private static boolean isCommandInPath(@Nonnull String command) {
        String checkCommand = isWindows() ? "where " + command : "which " + command;
        ProcessBuilder pb = new ProcessBuilder(
                isWindows() ? new String[]{"cmd", "/c", checkCommand} : new String[]{"sh", "-c", checkCommand});
        try {
            Process process = pb.start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.FINE, "Error checking for " + command + " in PATH", e);
            return false;
        }
    }

    /**
     * Installs rustup into a specified destination directory. This method handles both
     * Unix-like and Windows systems and includes retry logic for robustness.
     *
     * @param destination The directory where rustup should be installed.
     * @throws IOException If the installation fails after multiple retries.
     * @throws InterruptedException If the installation is interrupted.
     */
    public static void installRustup(@Nonnull File destination) throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Installing rustup to: {0}", destination.getAbsolutePath());

        if (!destination.exists() && !destination.mkdirs()) {
            throw new IOException("Failed to create destination directory: " + destination);
        }

        IOException lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                LOGGER.log(Level.INFO, "rustup installation attempt {0} of {1}", new Object[]{attempt, MAX_RETRIES});

                if (isWindows()) {
                    installRustupWindows(destination);
                } else {
                    installRustupUnix(destination);
                }

                LOGGER.info("rustup installation completed successfully");
                return;

            } catch (IOException e) {
                lastException = e;
                LOGGER.log(Level.WARNING, "Installation attempt " + attempt + " failed", e);

                if (attempt < MAX_RETRIES) {
                    LOGGER.log(Level.INFO, "Retrying in {0}ms...", RETRY_DELAY_MS);
                    Thread.sleep(RETRY_DELAY_MS);
                }
            }
        }

        throw new IOException("Failed to install rustup after " + MAX_RETRIES + " attempts", lastException);
    }

    /**
     * Installs rustup on Unix-like systems (Linux, macOS).
     */
    private static void installRustupUnix(@Nonnull File destination) throws IOException, InterruptedException {
        LOGGER.info("Starting rustup installation for Unix-like system");
        File installer = new File(destination, "rustup-init.sh");

        // Download the rustup-init.sh script
        LOGGER.log(Level.INFO, "Downloading rustup installer from: {0}", RUSTUP_INSTALLER_URL);
        executeCommand(new ProcessBuilder("curl", "--proto", "=https", "--tlsv1.2", "-sSf",
                RUSTUP_INSTALLER_URL, "-o", installer.getAbsolutePath()), "download rustup installer");

        // Make the installer executable
        try {
            Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE, PosixFilePermission.OWNER_EXECUTE);
            Files.setPosixFilePermissions(installer.toPath(), perms);
            LOGGER.fine("Set executable permissions on rustup installer");
        } catch (UnsupportedOperationException e) {
            LOGGER.log(Level.FINE, "Could not set POSIX permissions (filesystem may not support it)", e);
        }

        // Run the installer non-interactively
        LOGGER.info("Running rustup installer");
        ProcessBuilder installPb = new ProcessBuilder("sh", installer.getAbsolutePath(), "-y",
                "--default-toolchain", "none", "--no-modify-path");
        installPb.environment().put("CARGO_HOME", destination.getAbsolutePath());
        installPb.environment().put("RUSTUP_HOME", new File(destination, "rustup").getAbsolutePath());
        executeCommand(installPb, "install rustup");
    }

    /**
     * Installs rustup on Windows systems.
     */
    private static void installRustupWindows(@Nonnull File destination) throws IOException, InterruptedException {
        LOGGER.info("Starting rustup installation for Windows");
        File installer = new File(destination, "rustup-init.exe");

        // Download the rustup-init.exe installer
        LOGGER.log(Level.INFO, "Downloading rustup installer from: {0}", RUSTUP_INSTALLER_WINDOWS_URL);
        String command = "Invoke-WebRequest -Uri '" + RUSTUP_INSTALLER_WINDOWS_URL + "' -OutFile '"
                + installer.getAbsolutePath() + "' -UseBasicParsing";
        executeCommand(new ProcessBuilder("powershell", "-NoProfile", "-Command", command),
                "download rustup installer");

        // Run the installer non-interactively
        LOGGER.info("Running rustup installer");
        ProcessBuilder installPb = new ProcessBuilder(installer.getAbsolutePath(), "-y",
                "--default-toolchain", "none", "--no-modify-path");
        installPb.environment().put("CARGO_HOME", destination.getAbsolutePath());
        installPb.environment().put("RUSTUP_HOME", new File(destination, "rustup").getAbsolutePath());
        executeCommand(installPb, "install rustup");
    }

    /**
     * Installs a specific Rust toolchain version using rustup.
     *
     * @param rustHome The home directory of the Rust installation.
     * @param version  The toolchain to install (e.g., "stable", "1.75.0", "nightly").
     * @throws IOException If the installation fails.
     * @throws InterruptedException If the installation is interrupted.
     */
    public static void installToolchain(@Nonnull File rustHome, @Nonnull String version)
            throws IOException, InterruptedException {
        LOGGER.log(Level.INFO, "Installing Rust toolchain {0} to: {1}",
                new Object[]{version, rustHome.getAbsolutePath()});

        String rustupBin = getRustupBinary(rustHome);
        if (rustupBin == null) {
            throw new IOException("rustup binary not found. Please ensure rustup is installed.");
        }

        // Install the toolchain
        LOGGER.log(Level.INFO, "Running: rustup toolchain install {0}", version);
        ProcessBuilder installPb = new ProcessBuilder(rustupBin, "toolchain", "install", version);
        configureRustupEnvironment(installPb, rustHome);
        executeCommand(installPb, "install toolchain " + version);

        // Set the installed toolchain as the default for this installation
        LOGGER.log(Level.INFO, "Setting {0} as default toolchain", version);
        ProcessBuilder defaultPb = new ProcessBuilder(rustupBin, "default", version);
        configureRustupEnvironment(defaultPb, rustHome);
        executeCommand(defaultPb, "set default toolchain");
    }

    /**
     * Helper method to execute a command and handle its output and exit code.
     */
    private static void executeCommand(ProcessBuilder pb, String description) throws IOException, InterruptedException {
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        int exitCode = process.waitFor();

        if (exitCode != 0) {
            throw new IOException(
                    "Failed to " + description + ". Exit code: " + exitCode + ", Output: " + output);
        }
        LOGGER.log(Level.FINE, "{0} output: {1}", new Object[]{description, output});
    }

    /**
     * Configures the environment variables (CARGO_HOME and RUSTUP_HOME) for a rustup command.
     */
    private static void configureRustupEnvironment(ProcessBuilder pb, File rustHome) {
        pb.environment().put("CARGO_HOME", rustHome.getAbsolutePath());
        pb.environment().put("RUSTUP_HOME", new File(rustHome, "rustup").getAbsolutePath());
    }

    /**
     * Locates a Rust executable (cargo or rustc) within a given installation directory.
     *
     * @param rustHome The home directory of the Rust installation.
     * @param name The name of the binary ("cargo" or "rustc").
     * @return A {@link File} object representing the executable, or {@code null} if not found.
     */
    private static File getExecutable(@Nonnull File rustHome, @Nonnull String name) {
        String exeName = isWindows() ? name + ".exe" : name;
        File binDir = new File(rustHome, "bin");
        File executable = new File(binDir, exeName);
        return executable.exists() ? executable : null;
    }

    /**
     * Locates the rustup binary, checking both the installation directory and the system PATH.
     *
     * @param rustHome The home directory of the Rust installation.
     * @return The path to the rustup binary, or {@code null} if not found.
     */
    private static String getRustupBinary(@Nonnull File rustHome) {
        File rustupFile = getExecutable(rustHome, "rustup");
        if (rustupFile != null) {
            return rustupFile.getAbsolutePath();
        }
        if (isCommandInPath("rustup")) {
            return "rustup";
        }
        return null;
    }

    /**
     * Checks if the current operating system is Windows.
     *
     * @return {@code true} if the OS is Windows, {@code false} otherwise.
     */
    private static boolean isWindows() {
        return File.pathSeparatorChar == ';';
    }
}
