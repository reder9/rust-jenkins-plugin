package io.jenkins.plugins.rust;

import hudson.util.FormValidation;
import org.junit.Test;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Comprehensive tests for RustToolInstaller validation
 */
public class RustToolInstallerValidationTest {

    private RustToolInstaller.DescriptorImpl descriptor = new RustToolInstaller.DescriptorImpl();

    @Test
    public void testValidChannelNames() {
        // Test stable
        FormValidation result = descriptor.doCheckVersion("stable");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
        assertThat(result.getMessage(), containsString("stable"));

        // Test beta
        result = descriptor.doCheckVersion("beta");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
        assertThat(result.getMessage(), containsString("beta"));

        // Test nightly
        result = descriptor.doCheckVersion("nightly");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
        assertThat(result.getMessage(), containsString("nightly"));
    }

    @Test
    public void testValidVersionNumbers() {
        // Test full version
        FormValidation result = descriptor.doCheckVersion("1.75.0");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
        assertThat(result.getMessage(), containsString("1.75.0"));

        // Test short version
        result = descriptor.doCheckVersion("1.76");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));

        // Test patch version
        result = descriptor.doCheckVersion("1.80.1");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }

    @Test
    public void testValidDatedToolchains() {
        // Test nightly dated
        FormValidation result = descriptor.doCheckVersion("nightly-2024-01-15");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
        assertThat(result.getMessage(), containsString("dated toolchain"));

        // Test beta dated
        result = descriptor.doCheckVersion("beta-2024-02-01");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));

        // Test stable dated
        result = descriptor.doCheckVersion("stable-2024-03-20");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }

    @Test
    public void testEmptyVersion() {
        FormValidation result = descriptor.doCheckVersion("");
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
        String msg = result.getMessage();
        assertThat(msg, containsString("cannot be empty"));
        assertThat(msg, containsString("stable"));
    }

    @Test
    public void testNullVersion() {
        FormValidation result = descriptor.doCheckVersion(null);
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(), containsString("cannot be empty"));
    }

    @Test
    public void testWhitespaceOnlyVersion() {
        FormValidation result = descriptor.doCheckVersion("   ");
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(), containsString("cannot be empty"));
    }

    @Test
    public void testInvalidVersionWithRustPrefix() {
        FormValidation result = descriptor.doCheckVersion("rust-1.75.0");
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
        String msg = result.getMessage();
        assertThat(msg, containsString("Invalid Rust version format"));
        // Handle HTML escaping of quotes
        assertThat(msg,
                anyOf(containsString("Don't include 'rust'"), containsString("Don&#039;t include &#039;rust&#039;")));
        assertThat(msg, containsString("1.75.0"));
    }

    @Test
    public void testInvalidVersionWithPath() {
        FormValidation result = descriptor.doCheckVersion("/usr/local/rust");
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(),
                anyOf(containsString("Don't include paths"), containsString("Don&#039;t include paths")));
    }

    @Test
    public void testInvalidVersionWithBackslashPath() {
        FormValidation result = descriptor.doCheckVersion("C:\\rust\\bin");
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(), containsString("Don&#039;t include paths"));
    }

    @Test
    public void testInvalidChannelName() {
        FormValidation result = descriptor.doCheckVersion("latest");
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
        String msg = result.getMessage();
        assertThat(msg, containsString("Invalid Rust version format"));
        assertThat(msg, containsString("stable"));
        assertThat(msg, containsString("beta"));
        assertThat(msg, containsString("nightly"));
    }

    @Test
    public void testInvalidVersionFormat() {
        FormValidation result = descriptor.doCheckVersion("1.75.0.0");
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(), containsString("Invalid Rust version format"));
        assertThat(result.getMessage(), containsString("Examples"));
    }

    @Test
    public void testInvalidDatedToolchain() {
        FormValidation result = descriptor.doCheckVersion("nightly-2024-13-45");
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
        assertThat(result.getMessage(), containsString("Invalid Rust version format"));
    }

    @Test
    public void testVersionWithWhitespace() {
        // Should trim whitespace
        FormValidation result = descriptor.doCheckVersion("  stable  ");
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }

    @Test
    public void testHelpfulErrorMessages() {
        FormValidation result = descriptor.doCheckVersion("bad-version");
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));

        // Check that error message contains helpful information
        String message = result.getMessage();
        assertThat(message, containsString("Valid formats"));
        assertThat(message, containsString("Channels"));
        assertThat(message, containsString("Version numbers"));
        assertThat(message, containsString("Dated toolchains"));
    }
}
