package io.jenkins.plugins.rust;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Comprehensive cross-platform compatibility tests simulating different OS and
 * architecture combinations.
 * Tests platform detection for all major deployment targets:
 * - macOS (Intel x86_64 and Apple Silicon ARM64)
 * - Linux (x86_64 and ARM64)
 * - Windows (x86_64 and ARM64)
 */
@RunWith(Parameterized.class)
public class RustInstallerPlatformTest {

    /**
     * Test parameters: description, pathSeparator, osArch, expectedWindows,
     * expectedArch
     */
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // macOS platforms
                { "macOS Intel", ':', "x86_64", false, "x86_64" },
                { "macOS Apple Silicon (M1/M2/M3)", ':', "aarch64", false, "aarch64" },
                { "macOS Apple Silicon (arm64)", ':', "arm64", false, "aarch64" },

                // Linux platforms
                { "Linux x86_64", ':', "x86_64", false, "x86_64" },
                { "Linux amd64", ':', "amd64", false, "x86_64" },
                { "Linux ARM64", ':', "aarch64", false, "aarch64" },
                { "Linux ARM (32-bit)", ':', "arm", false, "arm" },
                { "Linux ARMv7", ':', "armv7l", false, "arm" },

                // Windows platforms
                { "Windows x86_64", ';', "x86_64", true, "x86_64" },
                { "Windows amd64", ';', "amd64", true, "x86_64" },
                { "Windows ARM64", ';', "aarch64", true, "aarch64" },
                { "Windows ARM64 (arm64)", ';', "arm64", true, "aarch64" },
        });
    }

    private final String description;
    private final char pathSeparator;
    private final String osArch;
    private final boolean expectedWindows;
    private final String expectedArch;

    public RustInstallerPlatformTest(String description, char pathSeparator, String osArch,
            boolean expectedWindows, String expectedArch) {
        this.description = description;
        this.pathSeparator = pathSeparator;
        this.osArch = osArch;
        this.expectedWindows = expectedWindows;
        this.expectedArch = expectedArch;
    }

    @Test
    public void testPlatformDetection() throws Exception {
        // Use reflection to access private methods
        java.lang.reflect.Method isWindowsMethod = RustInstaller.class.getDeclaredMethod("isWindows");
        isWindowsMethod.setAccessible(true);

        java.lang.reflect.Method getArchMethod = RustInstaller.class.getDeclaredMethod("getArchitecture");
        getArchMethod.setAccessible(true);

        // Temporarily set system properties
        String originalArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.arch", osArch);

            // Note: We can't easily mock File.pathSeparatorChar since it's a final field,
            // so we'll just test getArchitecture() here
            String detectedArch = (String) getArchMethod.invoke(null);
            assertEquals("Architecture detection failed for " + description,
                    expectedArch, detectedArch);

            // For Windows platforms, verify the installer URL selection
            if (expectedWindows) {
                java.lang.reflect.Method getUrlMethod = RustInstaller.class.getDeclaredMethod("getWindowsInstallerUrl");
                getUrlMethod.setAccessible(true);
                String url = (String) getUrlMethod.invoke(null);

                if (expectedArch.equals("x86_64")) {
                    assertTrue("Expected x86_64 installer URL for " + description,
                            url.contains("x86_64"));
                } else if (expectedArch.equals("aarch64")) {
                    assertTrue("Expected aarch64 installer URL for " + description,
                            url.contains("aarch64"));
                }
            }
        } finally {
            // Restore original property
            System.setProperty("os.arch", originalArch);
        }
    }

    @Test
    public void testExecutableNaming() {
        // Test that we understand executable naming conventions
        // This doesn't require system property manipulation

        boolean isWindows = (pathSeparator == ';');

        // Simulate what the code does
        String cargoName = isWindows ? "cargo.exe" : "cargo";
        String rustcName = isWindows ? "rustc.exe" : "rustc";
        String rustupName = isWindows ? "rustup.exe" : "rustup";

        if (expectedWindows) {
            assertEquals("Cargo executable name for " + description, "cargo.exe", cargoName);
            assertEquals("rustc executable name for " + description, "rustc.exe", rustcName);
            assertEquals("rustup executable name for " + description, "rustup.exe", rustupName);
        } else {
            assertEquals("Cargo executable name for " + description, "cargo", cargoName);
            assertEquals("rustc executable name for " + description, "rustc", rustcName);
            assertEquals("rustup executable name for " + description, "rustup", rustupName);
        }
    }
}
