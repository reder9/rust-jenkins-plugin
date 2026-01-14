package io.jenkins.plugins.rust;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Cross-platform tests for Windows rustup installer URL selection.
 * Ensures the correct installer is chosen for different Windows architectures.
 */
@RunWith(Parameterized.class)
public class RustInstallerWindowsUrlTest {

    /**
     * Test parameters: osArch, expectedUrl
     */
    @Parameters(name = "{index}: {0} => {1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // x86_64 architectures should use x86_64 installer
                { "x86_64", "https://win.rustup.rs/x86_64" },
                { "amd64", "https://win.rustup.rs/x86_64" },
                { "AMD64", "https://win.rustup.rs/x86_64" },

                // ARM64 architectures should use ARM64 installer
                { "aarch64", "https://win.rustup.rs/aarch64" },
                { "arm64", "https://win.rustup.rs/aarch64" },
                { "AARCH64", "https://win.rustup.rs/aarch64" },
                { "ARM64", "https://win.rustup.rs/aarch64" },
        });
    }

    private final String osArch;
    private final String expectedUrl;

    public RustInstallerWindowsUrlTest(String osArch, String expectedUrl) {
        this.osArch = osArch;
        this.expectedUrl = expectedUrl;
    }

    @Test
    public void testWindowsInstallerUrlSelection() throws Exception {
        // Use reflection to test the private getWindowsInstallerUrl() method
        java.lang.reflect.Method method = RustInstaller.class.getDeclaredMethod("getWindowsInstallerUrl");
        method.setAccessible(true);

        // Temporarily set the os.arch system property
        String originalArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.arch", osArch);
            String result = (String) method.invoke(null);
            assertEquals("Windows installer URL selection failed for architecture: " + osArch,
                    expectedUrl, result);
        } finally {
            // Restore original property
            System.setProperty("os.arch", originalArch);
        }
    }
}
