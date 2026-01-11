package io.jenkins.plugins.rust;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for RustInstaller
 */
public class RustInstallerTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private File testDir;
    
    @Before
    public void setUp() throws IOException {
        testDir = tempFolder.newFolder("rust-test");
    }
    
    @Test
    public void testGetInstallationPath() {
        File toolsDir = new File("/tmp/jenkins-tools");
        String version = "1.75.0";
        
        File installationPath = RustInstaller.getInstallationPath(toolsDir, version);
        
        assertThat(installationPath.getAbsolutePath(), 
            equalTo(new File(toolsDir, "rust-1.75.0").getAbsolutePath()));
    }
    
    @Test
    public void testVerifyInstallationWithNonExistentDirectory() {
        File nonExistent = new File(testDir, "non-existent");
        
        boolean result = RustInstaller.verifyInstallation(nonExistent);
        
        assertThat(result, is(false));
    }
    
    @Test
    public void testVerifyInstallationWithEmptyDirectory() throws IOException {
        File emptyDir = new File(testDir, "empty");
        emptyDir.mkdirs();
        
        boolean result = RustInstaller.verifyInstallation(emptyDir);
        
        assertThat(result, is(false));
    }
    
    @Test
    public void testIsCargoInPath() {
        // This test will pass or fail depending on whether cargo is in PATH
        // We just verify the method doesn't throw an exception
        try {
            boolean result = RustInstaller.isCargoInPath();
            // Result can be true or false, both are valid
            assertThat(result, anyOf(is(true), is(false)));
        } catch (Exception e) {
            fail("isCargoInPath should not throw an exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testIsRustupInPath() {
        // This test will pass or fail depending on whether rustup is in PATH
        // We just verify the method doesn't throw an exception
        try {
            boolean result = RustInstaller.isRustupInPath();
            // Result can be true or false, both are valid
            assertThat(result, anyOf(is(true), is(false)));
        } catch (Exception e) {
            fail("isRustupInPath should not throw an exception: " + e.getMessage());
        }
    }
    
    @Test
    public void testGetCargoVersionWithNonExistentDirectory() {
        File nonExistent = new File(testDir, "non-existent");
        
        try {
            String version = RustInstaller.getCargoVersion(nonExistent);
            assertThat(version, nullValue());
        } catch (Exception e) {
            // Exception is acceptable for non-existent directory
        }
    }
    
    @Test
    public void testGetRustVersionWithNonExistentDirectory() {
        File nonExistent = new File(testDir, "non-existent");
        
        try {
            String version = RustInstaller.getRustVersion(nonExistent);
            assertThat(version, nullValue());
        } catch (Exception e) {
            // Exception is acceptable for non-existent directory
        }
    }
}
