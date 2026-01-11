package io.jenkins.plugins.rust;

import hudson.Plugin;
import java.util.logging.Logger;

/**
 * The main entry point for the Rust plugin. This class handles the plugin's
 * lifecycle events, such as startup and initialization.
 */
public class RustPlugin extends Plugin {

    private static final Logger LOGGER = Logger.getLogger(RustPlugin.class.getName());

    /**
     * Called by Jenkins when the plugin is started.
     *
     * @throws Exception If an error occurs during startup.
     */
    @Override
    public void start() throws Exception {
        super.start();
        LOGGER.info("Rust Plugin started");
    }

    /**
     * Called by Jenkins after all plugins have been loaded and initialized.
     *
     * @throws Exception If an error occurs during post-initialization.
     */
    @Override
    public void postInitialize() throws Exception {
        super.postInitialize();
        LOGGER.info("Rust Plugin post-initialization complete");
    }
}
