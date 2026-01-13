package io.jenkins.plugins.rust;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import org.jenkinsci.Symbol;
import org.jenkinsci.plugins.workflow.steps.Step;
import org.jenkinsci.plugins.workflow.steps.StepContext;
import org.jenkinsci.plugins.workflow.steps.StepDescriptor;
import org.jenkinsci.plugins.workflow.steps.StepExecution;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Convenience pipeline step for building Rust projects.
 * Usage: cargoBuild(release: true, features: 'feature1,feature2')
 */
public class CargoBuildStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean release;
    private String target;
    private String features;
    private String rustInstallationName;

    @DataBoundConstructor
    public CargoBuildStep() {
    }

    public boolean isRelease() {
        return release;
    }

    @DataBoundSetter
    public void setRelease(boolean release) {
        this.release = release;
    }

    public String getTarget() {
        return target;
    }

    @DataBoundSetter
    public void setTarget(String target) {
        this.target = target;
    }

    public String getFeatures() {
        return features;
    }

    @DataBoundSetter
    public void setFeatures(String features) {
        this.features = features;
    }

    public String getRustInstallationName() {
        return rustInstallationName;
    }

    @DataBoundSetter
    public void setRustInstallationName(String rustInstallationName) {
        this.rustInstallationName = rustInstallationName;
    }

    @Override
    public StepExecution start(StepContext context) throws Exception {
        // Build the args for the underlying CargoStep
        List<String> argsList = new ArrayList<>();

        if (release) {
            argsList.add("--release");
        }

        if (target != null && !target.trim().isEmpty()) {
            argsList.add("--target");
            argsList.add(target);
        }

        if (features != null && !features.trim().isEmpty()) {
            argsList.add("--features");
            argsList.add(features);
        }

        String args = argsList.isEmpty() ? null : String.join(" ", argsList);

        // Delegate to CargoStep
        CargoStep cargoStep = new CargoStep("build");
        if (args != null) {
            cargoStep.setArgs(args);
        }
        if (rustInstallationName != null) {
            cargoStep.setRustInstallationName(rustInstallationName);
        }

        return cargoStep.start(context);
    }

    @Symbol("cargoBuild")
    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "cargoBuild";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Build Rust project with Cargo";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }
    }
}
