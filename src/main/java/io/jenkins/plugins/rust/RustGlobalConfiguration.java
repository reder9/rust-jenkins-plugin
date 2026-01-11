package io.jenkins.plugins.rust;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global configuration for Rust tool installations. This class manages the list of
 * Rust installations that are available to Jenkins jobs. Users can configure multiple
 * Rust versions in the Jenkins global configuration page.
 */
@Extension
public class RustGlobalConfiguration extends GlobalConfiguration {

    private static final Logger LOGGER = Logger.getLogger(RustGlobalConfiguration.class.getName());

    private List<RustInstallation> installations = Collections.emptyList();

    /**
     * Default constructor. Loads the configuration from disk.
     */
    public RustGlobalConfiguration() {
        super();
        // The configuration should only be loaded if a Jenkins instance is running
        if (jenkins.model.Jenkins.getInstanceOrNull() != null) {
            load();
        }
    }

    /**
     * Returns the singleton instance of this global configuration.
     *
     * @return The {@link RustGlobalConfiguration} instance.
     */
    public static RustGlobalConfiguration get() {
        return GlobalConfiguration.all().get(RustGlobalConfiguration.class);
    }

    /**
     * Returns the display name for this configuration, which is shown in the Jenkins UI.
     *
     * @return The display name.
     */
    @Override
    public String getDisplayName() {
        return "Rust Installations";
    }

    /**
     * Returns the list of configured Rust installations.
     *
     * @return A list of {@link RustInstallation} objects.
     */
    public List<RustInstallation> getInstallations() {
        return installations;
    }

    /**
     * Sets the list of Rust installations. This method is typically called by Jenkins
     * when the global configuration is saved.
     *
     * @param installations The new list of installations.
     */
    public void setInstallations(List<RustInstallation> installations) {
        this.installations = installations != null ? installations : Collections.emptyList();
        save();
    }

    /**
     * Handles the submission of the global configuration form. This method binds the
     * submitted JSON data to this instance and saves the configuration.
     *
     * @param req The Stapler request.
     * @param json The JSON object representing the form data.
     * @return {@code true} if the configuration was saved successfully.
     * @throws hudson.model.Descriptor.FormException If an error occurs while processing the form data.
     */
    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws hudson.model.Descriptor.FormException {
        try {
            LOGGER.fine("Configuring Rust installations");
            req.bindJSON(this, json);
            save();
            LOGGER.info("Rust installations configured successfully");
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error configuring Rust installations", e);
            throw new hudson.model.Descriptor.FormException(
                "Failed to configure Rust installations: " + e.getMessage(), "rustConfig");
        }
    }

    /**
     * Retrieves a Rust installation by its name.
     *
     * @param name The name of the installation to find.
     * @return The {@link RustInstallation} object, or {@code null} if no installation
     * with the given name is found.
     */
    public RustInstallation getInstallation(String name) {
        if (name == null || name.isEmpty()) {
            LOGGER.warning("Requested Rust installation with a null or empty name.");
            return null;
        }

        for (RustInstallation installation : installations) {
            if (name.equals(installation.getName())) {
                return installation;
            }
        }

        LOGGER.log(Level.WARNING, "Rust installation not found: {0}", name);
        return null;
    }

    /**
     * Checks if a Rust installation with the given name exists.
     *
     * @param name The name of the installation to check.
     * @return {@code true} if an installation with the given name exists, {@code false} otherwise.
     */
    public boolean installationExists(String name) {
        return getInstallation(name) != null;
    }
}
