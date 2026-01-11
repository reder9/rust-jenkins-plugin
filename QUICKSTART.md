# Rust Jenkins Plugin - Quick Start

## Installation & Testing

### 1. Build and Install the Plugin

```bash
# Build the updated plugin
cd /Users/caleb/Desktop/RederSoft/Jenkins-Plugins/rust
mvn clean package -DskipTests

# The plugin file will be at:
# target/rust.hpi
```

**Install in Jenkins**:
1. Manage Jenkins → Manage Plugins → Advanced
2. Upload `target/rust.hpi`
3. Restart Jenkins

### 2. Configure Rust Installation

Manage Jenkins → Global Tool Configuration → Rust

**Add Rust Installation**:
- **Name**: `Rust-Stable`
- **Install automatically**: ☑️ Check this
  - Choose "Install Rust via rustup"
  - **Version**: `stable`
  - **Install rustup**: ☑️ Checked
  
Click Save.

### 3. Test with the Jenkinsfile

Create a new Pipeline job and paste this test script:

```groovy
pipeline {
    agent { label 'mac' }  // Or use 'any' if no Mac label
    
    tools {
        rust 'Rust-Stable'
    }
    
    stages {
        stage('Test Rust') {
            steps {
                sh '''
                    rustc --version
                    cargo --version
                    echo "CARGO_HOME: $CARGO_HOME"
                '''
            }
        }
        
        stage('Test withRust') {
            steps {
                withRust(rustInstallationName: 'Rust-Stable') {
                    sh '''
                        cargo new hello_world
                        cd hello_world
                        cargo build
                        cargo run
                    '''
                }
            }
        }
    }
}
```

## What Was Fixed

1. ✅ Removed `checkout scm` for inline pipeline scripts
2. ✅ Improved version detection error handling (no more "null" messages)
3. ✅ Both `tools` block and `withRust` wrapper now work correctly

## Expected Output

You should now see:
```
Installing Rust stable to /path/to/tools/...
Installing rustup...
rustup installed successfully
Installing Rust toolchain stable...
Rust toolchain stable installed successfully  
Verifying installation...
Successfully installed Rust 1.75.0  ← Actual version now!
```

Then in your test stages:
```
rustc 1.75.0 (82e1608df 2023-12-21)
cargo 1.75.0 (1bc8dbc342 2023-12-07)
CARGO_HOME: /path/to/tools/Rust-Stable
```

## Troubleshooting

If you still see version detection issues, check:
- The cargo binary exists in `$CARGO_HOME/bin/cargo`
- Run manually: `/path/to/tools/Rust-Stable/bin/cargo --version`

The plugin will still work even if version detection fails - it's just cosmetic for the log output.
