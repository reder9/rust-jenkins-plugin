package io.jenkins.plugins.rust;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

/**
 * Unit tests for RustGlobalConfiguration
 */
public class RustGlobalConfigurationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private RustGlobalConfiguration config;

    @Before
    public void setUp() {
        config = RustGlobalConfiguration.get();
    }

    @Test
    public void testGetDisplayName() {
        assertThat(config.getDisplayName(), equalTo("Rust Installations"));
    }

    @Test
    public void testGetInstallationsInitiallyEmpty() {
        List<RustInstallation> installations = config.getInstallations();

        assertThat(installations, not(nullValue()));
        assertThat(installations.size(), equalTo(0));
    }

    @Test
    public void testSetInstallations() {
        List<RustInstallation> installations = new ArrayList<>();
        installations.add(new RustInstallation("Rust 1.75", "/opt/rust-1.75", Collections.emptyList()));
        installations.add(new RustInstallation("Rust Stable", "/opt/rust-stable", Collections.emptyList()));

        config.setInstallations(installations);

        assertThat(config.getInstallations().size(), equalTo(2));
    }

    @Test
    public void testSetInstallationsNull() {
        config.setInstallations(null);

        assertThat(config.getInstallations(), not(nullValue()));
        assertThat(config.getInstallations().size(), equalTo(0));
    }

    @Test
    public void testGetInstallationByName() {
        List<RustInstallation> installations = new ArrayList<>();
        RustInstallation rust175 = new RustInstallation("Rust 1.75", "/opt/rust-1.75", Collections.emptyList());
        installations.add(rust175);
        installations.add(new RustInstallation("Rust Stable", "/opt/rust-stable", Collections.emptyList()));

        config.setInstallations(installations);

        RustInstallation found = config.getInstallation("Rust 1.75");

        assertThat(found, not(nullValue()));
        assertThat(found.getName(), equalTo("Rust 1.75"));
        assertThat(found.getHome(), equalTo("/opt/rust-1.75"));
    }

    @Test
    public void testGetInstallationByNameNotFound() {
        List<RustInstallation> installations = new ArrayList<>();
        installations.add(new RustInstallation("Rust 1.75", "/opt/rust-1.75", Collections.emptyList()));

        config.setInstallations(installations);

        RustInstallation found = config.getInstallation("NonExistent");

        assertThat(found, nullValue());
    }

    @Test
    public void testGetInstallationByNameNull() {
        RustInstallation found = config.getInstallation(null);

        assertThat(found, nullValue());
    }

    @Test
    public void testGetInstallationByNameEmpty() {
        RustInstallation found = config.getInstallation("");

        assertThat(found, nullValue());
    }

    @Test
    public void testInstallationExists() {
        List<RustInstallation> installations = new ArrayList<>();
        installations.add(new RustInstallation("Rust 1.75", "/opt/rust-1.75", Collections.emptyList()));

        config.setInstallations(installations);

        assertThat(config.installationExists("Rust 1.75"), is(true));
        assertThat(config.installationExists("NonExistent"), is(false));
    }
}
