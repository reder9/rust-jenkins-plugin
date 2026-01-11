package io.jenkins.plugins.rust;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.kohsuke.stapler.StaplerRequest;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;

import net.sf.json.JSONObject;

/**
 * Global configuration for Rust tool installations
 * Allows users to configure multiple Rust versions in Jenkins global config
 */
@Extension
public class RustGlobalConfiguration extends GlobalConfiguration {
    private static final Logger LOGGER = Logger.getLogger(RustGlobalConfiguration.class.getName());
    
    private List<RustInstallation> installations = Collections.emptyList();
    
    public RustGlobalConfiguration() {
        super();
        // Only load if Jenkins instance is available
        if (jenkins.model.Jenkins.getInstanceOrNull() != null) {
            load();
        }
    }
    
    public static RustGlobalConfiguration get() {
        return GlobalConfiguration.all().get(RustGlobalConfiguration.class);
    }
    
    @Override
    public String getDisplayName() {
        return "Rust Installations";
    }
    
    public List<RustInstallation> getInstallations() {
        return installations;
    }
    
    public void setInstallations(List<RustInstallation> installations) {
        this.installations = installations != null ? installations : Collections.emptyList();
        save();
    }
    
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
        try {
            req.bindJSON(this, json);
            save();
            return true;
        } catch (Exception e) {
            LOGGER.severe("Error configuring Rust installations: " + e.getMessage());
            throw new hudson.model.Descriptor.FormException("Failed to configure Rust installations: " + e.getMessage(), 
                "rustConfig");
        }
    }
    
    /**
     * Get a Rust installation by name
     */
    public RustInstallation getInstallation(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        for (RustInstallation installation : installations) {
            if (name.equals(installation.getName())) {
                return installation;
            }
        }
        
        return null;
    }
    
    /**
     * Check if a Rust installation exists
     */
    public boolean installationExists(String name) {
        return getInstallation(name) != null;
    }
}
