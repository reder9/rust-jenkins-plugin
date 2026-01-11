package io.jenkins.plugins.rust;

import java.util.logging.Logger;

import org.kohsuke.stapler.QueryParameter;

import hudson.Extension;
import hudson.util.FormValidation;

/**
 * Descriptor for RustToolInstaller
 */
@Extension
public class RustToolInstallerDescriptor extends hudson.model.Descriptor {
    private static final Logger LOGGER = Logger.getLogger(RustToolInstallerDescriptor.class.getName());
    
    public RustToolInstallerDescriptor() {
        super(RustToolInstaller.class);
    }
    
    public RustToolInstallerDescriptor(Class<?> clazz) {
        super(clazz);
    }
    
    @Override
    public String getDisplayName() {
        return "Rust Tool Installer";
    }
    
    /**
     * Validate Rust version format
     */
    public FormValidation doCheckVersion(@QueryParameter final String value) {
        if (value == null || value.isEmpty()) {
            return FormValidation.error("Rust version cannot be empty");
        }
        
        // Validate semantic version format (e.g., 1.75.0, stable, beta, nightly)
        // Also allow channel names like stable, beta, nightly
        if (!value.matches("^(stable|beta|nightly|\\d+\\.\\d+(\\.\\d+)?(-[a-zA-Z0-9.]+)?)$")) {
            return FormValidation.error("Invalid Rust version format. Use semantic versioning (e.g., 1.75.0) or channel name (stable, beta, nightly)");
        }
        
        // Check for known Rust versions (major.minor)
        String[] supportedVersions = {
            "1.70", "1.71", "1.72", "1.73", "1.74", "1.75", "1.76", "1.77", "1.78", "1.79", "1.80"
        };
        
        // If it's a version number, check if it's in a reasonable range
        if (value.matches("^\\d+\\.\\d+")) {
            boolean isSupported = false;
            for (String supported : supportedVersions) {
                if (value.startsWith(supported)) {
                    isSupported = true;
                    break;
                }
            }
            
            if (!isSupported && !value.equals("stable") && !value.equals("beta") && !value.equals("nightly")) {
                return FormValidation.warning("Rust version " + value + " may not be officially supported");
            }
        }
        
        return FormValidation.ok();
    }
}
