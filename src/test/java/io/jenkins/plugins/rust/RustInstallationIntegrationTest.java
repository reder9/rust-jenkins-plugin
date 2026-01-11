package io.jenkins.plugins.rust;

import hudson.EnvVars;
import hudson.model.TaskListener;
import hudson.slaves.DumbSlave;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;

import java.io.PrintStream;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Integration tests for RustInstallation
 */
public class RustInstallationIntegrationTest {

    @Rule
    public JenkinsRule jenkins = new JenkinsRule();

    private RustInstallation installation;

    @Before
    public void setUp() {
        installation = new RustInstallation("Test Rust", "/opt/rust", Collections.emptyList());
    }

    @Test
    public void testGetName() {
        assertThat(installation.getName(), equalTo("Test Rust"));
    }

    @Test
    public void testGetHome() {
        assertThat(installation.getHome(), equalTo("/opt/rust"));
    }

    @Test
    public void testForEnvironmentExpandsVariables() {
        EnvVars env = new EnvVars();
        env.put("RUST_BASE", "/usr/local");

        RustInstallation installWithVar = new RustInstallation(
                "Expandable",
                "${RUST_BASE}/rust",
                Collections.emptyList());

        RustInstallation expanded = installWithVar.forEnvironment(env);
        assertThat(expanded.getHome(), equalTo("/usr/local/rust"));
    }

    @Test
    public void testForEnvironmentNoVariables() {
        EnvVars env = new EnvVars();

        RustInstallation expanded = installation.forEnvironment(env);
        assertThat(expanded.getHome(), equalTo("/opt/rust"));
    }

    @Test
    public void testForNodeOnMaster() throws Exception {
        TaskListener listener = TaskListener.NULL;

        RustInstallation nodeSpecific = installation.forNode(jenkins.jenkins, listener);
        assertThat(nodeSpecific.getName(), equalTo(installation.getName()));
        assertThat(nodeSpecific.getHome(), equalTo(installation.getHome()));
    }

    @Test
    public void testForNodeOnAgent() throws Exception {
        DumbSlave agent = jenkins.createOnlineSlave();
        TaskListener listener = TaskListener.NULL;

        RustInstallation nodeSpecific = installation.forNode(agent, listener);
        assertThat(nodeSpecific, notNullValue());
        assertThat(nodeSpecific.getName(), equalTo(installation.getName()));
    }

    @Test
    public void testGetCargoPath() {
        String cargoPath = installation.getCargo();
        assertThat(cargoPath, endsWith("bin/cargo"));
        assertThat(cargoPath, startsWith("/opt/rust"));
    }

    @Test
    public void testGetRustcPath() {
        String rustcPath = installation.getRustc();
        assertThat(rustcPath, endsWith("bin/rustc"));
        assertThat(rustcPath, startsWith("/opt/rust"));
    }

    @Test
    public void testGetRustupPath() {
        String rustupPath = installation.getRustup();
        assertThat(rustupPath, endsWith("bin/rustup"));
        assertThat(rustupPath, startsWith("/opt/rust"));
    }

    @Test
    public void testAllInstallations() {
        RustInstallation[] installations = RustInstallation.allInstallations();
        assertThat(installations, notNullValue());
        // Should be an array (may be empty initially)
        assertThat(installations.length >= 0, is(true));
    }

    @Test
    public void testDescriptorDisplayName() {
        RustInstallation.DescriptorImpl descriptor = new RustInstallation.DescriptorImpl();
        assertThat(descriptor.getDisplayName(), equalTo("Rust"));
    }

    @Test
    public void testDescriptorDoCheckName() {
        RustInstallation.DescriptorImpl descriptor = new RustInstallation.DescriptorImpl();

        // Valid name
        assertThat(descriptor.doCheckName("Rust-Stable").kind,
                equalTo(hudson.util.FormValidation.Kind.OK));

        // Empty name
        assertThat(descriptor.doCheckName("").kind,
                equalTo(hudson.util.FormValidation.Kind.ERROR));

        // Null name
        assertThat(descriptor.doCheckName(null).kind,
                equalTo(hudson.util.FormValidation.Kind.ERROR));
    }

    @Test
    public void testDescriptorDoCheckHome() {
        RustInstallation.DescriptorImpl descriptor = new RustInstallation.DescriptorImpl();

        // Non-existent home should return WARNING, not OK
        assertThat(descriptor.doCheckHome("/non/existent/path").kind,
                equalTo(hudson.util.FormValidation.Kind.WARNING));

        // Empty home should return ERROR
        assertThat(descriptor.doCheckHome("").kind,
                equalTo(hudson.util.FormValidation.Kind.ERROR));

        // Null home should return ERROR
        assertThat(descriptor.doCheckHome((String) null).kind,
                equalTo(hudson.util.FormValidation.Kind.ERROR));
    }
}
