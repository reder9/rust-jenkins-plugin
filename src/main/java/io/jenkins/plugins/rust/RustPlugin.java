package io.jenkins.plugins.rust;

import hudson.Plugin;
import java.util.logging.Logger;

/**
 * Main plugin class for the Rust Plugin
 * Handles plugin initialization and setup
 */
public class RustPlugin extends Plugin {
    private static final Logger LOGGER = Logger.getLogger(RustPlugin.class.getName());
    
    @Override
    public void start() throws Exception {
        super.start();
        LOGGER.info("Rust Plugin started");
    }
    
    @Override
    public void postInitialize() throws Exception {
        super.postInitialize();
        LOGGER.info("Rust Plugin post-initialization complete");
    }
}
