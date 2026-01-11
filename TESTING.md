# Rust Jenkins Plugin - Testing Guide

## Setting Up for Testing

### 1. Install the Plugin

After building the plugin with `mvn package`, you'll have a `rust.hpi` file in the `target/` directory.

**Option A: Install via Jenkins UI**
1. Go to Jenkins → Manage Jenkins → Manage Plugins → Advanced
2. Upload `target/rust.hpi`
3. Restart Jenkins

**Option B: Manual Installation**
```bash
cp target/rust.hpi $JENKINS_HOME/plugins/rust.hpi
# Restart Jenkins
```

### 2. Configure Rust Installation

1. Go to Jenkins → Manage Jenkins → Global Tool Configuration
2. Find "Rust" section
3. Click "Add Rust"
4. Configure installation:
   - **Name**: `Rust-Stable` (or your preferred name)
   - **Installation directory**: Path to your Rust installation (e.g., `/Users/yourusername/.cargo`)
   - Or use **Install automatically** with Rust installer:
     - **Label**: (optional) for specific nodes  
     - **Version**: `stable`, `beta`, `nightly`, or specific version like `1.75.0`
     - ☑️ **Install rustup**: Check this to auto-install rustup
     - ☐ **Prefer system tools**: Check if you want to use system Rust if available

### 3. Configure Additional Rust Versions (Optional)

To test with multiple Rust versions, repeat step 2 with different names:
- `Rust-Stable` → version: `stable`
- `Rust-Nightly` → version: `nightly`  
- `Rust-Beta` → version: `beta`
- `Rust-1.75` → version: `1.75.0`

## Running the Test Jenkinsfile

### Prerequisites

1. **Mac Agent**: Ensure you have a Jenkins agent with label `mac`
   - Or change the `label 'mac'` line in Jenkinsfile to match your setup
   - Or remove the agent block to run on any available agent

2. **Rust Installation**: Configure at least one Rust installation named `Rust-Stable`

### Create a Pipeline Job

1. New Item → Pipeline → Name it "Rust Plugin Test"
2. In Pipeline section:
   - **Definition**: Pipeline script from SCM
   - **SCM**: Git
   - **Repository URL**: Your repository URL
   - **Script Path**: `Jenkinsfile`
3. Save and Build

Or use this minimal inline Jenkinsfile for quick testing:

```groovy
pipeline {
    agent any
    
    stages {
        stage('Test Rust') {
            steps {
                withRust(rustInstallationName: 'Rust-Stable') {
                    sh '''
                        rustc --version
                        cargo --version
                        echo "CARGO_HOME: $CARGO_HOME"
                    '''
                }
            }
        }
    }
}
```

## What the Jenkinsfile Tests

The provided Jenkinsfile tests the following features:

### ✅ Tool Installation
- Verifies Rust is available on PATH
- Checks rustc, cargo, and rustup versions
- Validates CARGO_HOME and RUSTUP_HOME environment variables

### ✅ withRust Wrapper
- Tests the `withRust` build wrapper  
- Verifies environment is properly set up
- Confirms tools are accessible inside the wrapper

### ✅ Building Rust Projects
- Creates a new Rust project with `cargo new`
- Builds the project with `cargo build`
- Runs the compiled binary
- Executes tests with `cargo test`

### ✅ Multiple Rust Versions
- Tests switching between different Rust installations
- Verifies each version is correctly activated

### ✅ Rust Tooling
- Tests clippy (Rust linter)
- Tests rustfmt (code formatter)
- Installs components if missing

## Expected Output

When the pipeline runs successfully, you should see:

```
✅ All Rust plugin tests passed successfully!
```

In the build logs, you'll see output like:

```
=== Verifying Rust Installation ===
Checking rustc version...
rustc 1.75.0 (82e1608df 2023-12-21)
Checking cargo version...
cargo 1.75.0 (1bc8dbc342 2023-12-07)

=== Environment Variables ===
CARGO_HOME: /Users/yourusername/.cargo
RUSTUP_HOME: /Users/yourusername/.cargo/rustup
PATH: /Users/yourusername/.cargo/bin:...

=== Testing withRust build wrapper ===
Inside withRust wrapper...
/Users/yourusername/.cargo/bin/rustc
/Users/yourusername/.cargo/bin/cargo

=== Building a sample Rust project ===
   Compiling hello_rust v0.1.0
    Finished dev [unoptimized + debuginfo] target(s) in 1.23s
     Running `target/debug/hello_rust`
Hello, world!
```

## Troubleshooting

### Plugin Not Showing Up
- Verify the plugin installed: Jenkins → Manage Jenkins → Manage Plugins → Installed
- Check Jenkins logs: `$JENKINS_HOME/logs/`
- Restart Jenkins completely

### "Rust installation not found" Error
- Verify the installation name matches exactly (case-sensitive)
- Check Global Tool Configuration has at least one Rust installation configured
- Installation name in Jenkinsfile must match configuration

### Tools Not in PATH
- Check CARGO_HOME and RUSTUP_HOME are set correctly
- Verify bin directory exists: `$CARGO_HOME/bin`
- Check file permissions on Rust binaries

### Mac-Specific Issues
- Ensure Xcode Command Line Tools are installed: `xcode-select --install`
- Check privacy/security settings allowing execution
- Verify agent has proper permissions

## Freestyle Project Usage

You can also use the plugin with freestyle projects:

1. Create a New Freestyle Project
2. In **Build Environment**:
   - ☑️ Check "Use Rust"
   - Select your Rust installation from dropdown
3. In **Build** section, add Execute Shell:
   ```bash
   cargo --version
   cargo build --release
   ```

## Pipeline with Declarative Syntax

```groovy
pipeline {
    agent any
    tools {
        rust 'Rust-Stable'
    }
    stages {
        stage('Build') {
            steps {
                sh 'cargo build --release'
            }
        }
        stage('Test') {
            steps {
                sh 'cargo test'
            }
        }
    }
}
```

## Pipeline with Scripted Syntax

```groovy
node {
    stage('Build') {
        withRust(rustInstallationName: 'Rust-Stable') {
            sh 'cargo build --release'
            sh 'cargo test'
        }
    }
}
```

## Next Steps

After verifying the plugin works:

1. **Run Full Test Suite**: `mvn test` (after we fix remaining test issues)
2. **Test on Real Projects**: Use the plugin with your actual Rust projects
3. **Multi-Platform Testing**: Test on Linux and Windows agents
4. **Performance Testing**: Verify installation caching works correctly
5. **Document Issues**: Report any bugs or enhancement requests

## Support

If you encounter issues:
1. Check Jenkins system logs
2. Enable debug logging for `io.jenkins.plugins.rust`
3. Review the walkthrough.md for troubleshooting tips
