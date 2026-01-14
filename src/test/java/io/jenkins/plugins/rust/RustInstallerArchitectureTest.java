package io.jenkins.plugins.rust;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;

/**
 * Cross-platform architecture compatibility tests for RustInstaller.
 * Tests various OS and architecture combinations to ensure proper platform
 * detection.
 */
@RunWith(Parameterized.class)
public class RustInstallerArchitectureTest {

    /**
     * Test parameters: osArch, expectedNormalizedArch
     */
    @Parameters(name = "{index}: osArch={0} => normalized={1}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                // x86_64 variations
                { "x86_64", "x86_64" },
                { "amd64", "x86_64" },
                { "X86_64", "x86_64" },
                { "AMD64", "x86_64" },

                // ARM64/aarch64 variations
                { "aarch64", "aarch64" },
                { "arm64", "aarch64" },
                { "AARCH64", "aarch64" },
                { "ARM64", "aarch64" },

                // Generic ARM (32-bit, not fully supported)
                { "arm", "arm" },
                { "armv7", "arm" },
                { "armv7l", "arm" },
        });
    }

    private final String osArch;
    private final String expectedNormalizedArch;

    public RustInstallerArchitectureTest(String osArch, String expectedNormalizedArch) {
        this.osArch = osArch;
        this.expectedNormalizedArch = expectedNormalizedArch;
    }

    @Test
    public void testArchitectureNormalization() throws Exception {
        // Use reflection to test the private getArchitecture() method
        java.lang.reflect.Method method = RustInstaller.class.getDeclaredMethod("getArchitecture");
        method.setAccessible(true);

        // Temporarily set the os.arch system property
        String originalArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.arch", osArch);
            String result = (String) method.invoke(null);
            assertEquals("Architecture normalization failed for: " + osArch,
                    expectedNormalizedArch, result);
        } finally {
            // Restore original property
            System.setProperty("os.arch", originalArch);
        }
    }
}
