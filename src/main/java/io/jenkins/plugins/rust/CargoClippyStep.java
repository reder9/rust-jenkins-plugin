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
 * Convenience pipeline step for running Clippy linter.
 * Usage: cargoClippy(denyWarnings: true)
 */
public class CargoClippyStep extends Step implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean denyWarnings;
    private boolean allTargets;
    private String rustInstallationName;

    @DataBoundConstructor
    public CargoClippyStep() {
    }

    public boolean isDenyWarnings() {
        return denyWarnings;
    }

    @DataBoundSetter
    public void setDenyWarnings(boolean denyWarnings) {
        this.denyWarnings = denyWarnings;
    }

    public boolean isAllTargets() {
        return allTargets;
    }

    @DataBoundSetter
    public void setAllTargets(boolean allTargets) {
        this.allTargets = allTargets;
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
        // Build the args
        List<String> argsList = new ArrayList<>();

        if (allTargets) {
            argsList.add("--all-targets");
        }

        // Add clippy args after --
        if (denyWarnings) {
            argsList.add("--");
            argsList.add("-D");
            argsList.add("warnings");
        }

        String args = argsList.isEmpty() ? null : String.join(" ", argsList);

        // Delegate to CargoStep
        CargoStep cargoStep = new CargoStep("clippy");
        if (args != null) {
            cargoStep.setArgs(args);
        }
        if (rustInstallationName != null) {
            cargoStep.setRustInstallationName(rustInstallationName);
        }

        return cargoStep.start(context);
    }

    @Symbol("cargoClippy")
    @Extension
    public static class DescriptorImpl extends StepDescriptor {

        @Override
        public String getFunctionName() {
            return "cargoClippy";
        }

        @Override
        @NonNull
        public String getDisplayName() {
            return "Run Clippy linter on Rust code";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }
    }
}
