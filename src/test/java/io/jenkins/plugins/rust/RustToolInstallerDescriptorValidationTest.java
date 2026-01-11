package io.jenkins.plugins.rust;

import hudson.util.FormValidation;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class RustToolInstallerDescriptorValidationTest {

    private RustToolInstaller.DescriptorImpl descriptor;

    @Before
    public void setUp() {
        descriptor = new RustToolInstaller.DescriptorImpl();
    }

    @Test
    public void testValidChannelNames() {
        assertThat(descriptor.doCheckVersion("stable").kind, equalTo(FormValidation.Kind.OK));
        assertThat(descriptor.doCheckVersion("beta").kind, equalTo(FormValidation.Kind.OK));
        assertThat(descriptor.doCheckVersion("nightly").kind, equalTo(FormValidation.Kind.OK));
    }

    @Test
    public void testValidVersionNumbers() {
        assertThat(descriptor.doCheckVersion("1.75.0").kind, equalTo(FormValidation.Kind.OK));
        assertThat(descriptor.doCheckVersion("1.76").kind, equalTo(FormValidation.Kind.OK));
        assertThat(descriptor.doCheckVersion("1.80.1").kind, equalTo(FormValidation.Kind.OK));
    }

    @Test
    public void testValidDatedToolchains() {
        assertThat(descriptor.doCheckVersion("nightly-2024-01-15").kind, equalTo(FormValidation.Kind.OK));
        assertThat(descriptor.doCheckVersion("beta-2024-02-01").kind, equalTo(FormValidation.Kind.OK));
    }

    @Test
    public void testEmptyVersion() {
        FormValidation validation = descriptor.doCheckVersion("");
        assertThat(validation.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(validation.getMessage(), containsString("cannot be empty"));
    }

    @Test
    public void testInvalidVersionWithRustPrefix() {
        FormValidation validation = descriptor.doCheckVersion("rust-1.75.0");
        assertThat(validation.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(validation.getMessage(), containsString("Don&#039;t include &#039;rust&#039;"));
    }

    @Test
    public void testInvalidVersionWithPath() {
        FormValidation validation = descriptor.doCheckVersion("/usr/local/rust");
        assertThat(validation.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(validation.getMessage(), containsString("Don&#039;t include paths"));
    }

    @Test
    public void testInvalidVersionWithBackslashPath() {
        FormValidation validation = descriptor.doCheckVersion("C:\\rust\\bin");
        assertThat(validation.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(validation.getMessage(), containsString("Don&#039;t include paths"));
    }
}
