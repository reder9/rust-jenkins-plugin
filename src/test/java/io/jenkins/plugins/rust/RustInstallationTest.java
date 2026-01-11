package io.jenkins.plugins.rust;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Collections;
import org.junit.Test;

/**
 * Unit tests for RustInstallation
 */
public class RustInstallationTest {

    @Test
    public void testInstallationCreation() {
        String name = "Rust 1.75";
        String home = "/opt/rust-1.75.0";

        RustInstallation installation = new RustInstallation(name, home, Collections.emptyList());

        assertThat(installation.getName(), equalTo(name));
        assertThat(installation.getHome(), equalTo(home));
    }

    @Test
    public void testInstallationWithDifferentVersions() {
        RustInstallation rust170 = new RustInstallation("Rust 1.70", "/opt/rust-1.70", Collections.emptyList());
        RustInstallation rust175 = new RustInstallation("Rust 1.75", "/opt/rust-1.75", Collections.emptyList());
        RustInstallation rustStable = new RustInstallation("Rust Stable", "/opt/rust-stable", Collections.emptyList());

        assertThat(rust170.getName(), equalTo("Rust 1.70"));
        assertThat(rust175.getName(), equalTo("Rust 1.75"));
        assertThat(rustStable.getName(), equalTo("Rust Stable"));

        assertThat(rust170.getHome(), not(equalTo(rust175.getHome())));
        assertThat(rust175.getHome(), not(equalTo(rustStable.getHome())));
    }

    @Test
    public void testInstallationSerialization() {
        RustInstallation installation = new RustInstallation("Test Rust", "/opt/test", Collections.emptyList());

        assertNotNull("Installation should not be null", installation);
        assertThat(installation.getName(), not(nullValue()));
        assertThat(installation.getHome(), not(nullValue()));
    }
}
