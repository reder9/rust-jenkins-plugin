package io.jenkins.plugins.rust;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

/**
 * Unit tests for RustToolInstaller
 */
public class RustToolInstallerTest {

    @Test
    public void testToolInstallerCreation() {
        RustToolInstaller installer = new RustToolInstaller(null, "1.75.0", false, true);

        assertThat(installer.getVersion(), equalTo("1.75.0"));
        assertThat(installer.isPreferBuiltInTools(), is(false));
        assertThat(installer.isInstallRustup(), is(true));
    }

    @Test
    public void testToolInstallerWithStable() {
        RustToolInstaller installer = new RustToolInstaller(null, "stable", true, false);

        assertThat(installer.getVersion(), equalTo("stable"));
        assertThat(installer.isPreferBuiltInTools(), is(true));
        assertThat(installer.isInstallRustup(), is(false));
    }

    @Test
    public void testToolInstallerWithNightly() {
        RustToolInstaller installer = new RustToolInstaller(null, "nightly", false, true);

        assertThat(installer.getVersion(), equalTo("nightly"));
        assertThat(installer.isPreferBuiltInTools(), is(false));
        assertThat(installer.isInstallRustup(), is(true));
    }
}
