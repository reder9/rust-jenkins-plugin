package io.jenkins.plugins.rust;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import org.junit.Test;

import hudson.util.FormValidation;

/**
 * Unit tests for RustToolInstallerDescriptor
 */
public class RustToolInstallerDescriptorTest {
    
    private RustToolInstallerDescriptor descriptor = new RustToolInstallerDescriptor();
    
    @Test
    public void testValidateVersionEmpty() {
        FormValidation result = descriptor.doCheckVersion("");
        
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
    }
    
    @Test
    public void testValidateVersionNull() {
        FormValidation result = descriptor.doCheckVersion(null);
        
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
    }
    
    @Test
    public void testValidateVersionStable() {
        FormValidation result = descriptor.doCheckVersion("stable");
        
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }
    
    @Test
    public void testValidateVersionBeta() {
        FormValidation result = descriptor.doCheckVersion("beta");
        
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }
    
    @Test
    public void testValidateVersionNightly() {
        FormValidation result = descriptor.doCheckVersion("nightly");
        
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }
    
    @Test
    public void testValidateVersionSemantic() {
        FormValidation result = descriptor.doCheckVersion("1.75.0");
        
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }
    
    @Test
    public void testValidateVersionMajorMinor() {
        FormValidation result = descriptor.doCheckVersion("1.75");
        
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }
    
    @Test
    public void testValidateVersionInvalid() {
        FormValidation result = descriptor.doCheckVersion("invalid-version");
        
        assertThat(result.kind, equalTo(FormValidation.Kind.ERROR));
    }
    
    @Test
    public void testValidateVersionWithPrerelease() {
        FormValidation result = descriptor.doCheckVersion("1.75.0-alpha");
        
        assertThat(result.kind, equalTo(FormValidation.Kind.OK));
    }
}
