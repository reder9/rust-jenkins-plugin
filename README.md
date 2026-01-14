# Rust Plugin for Jenkins

A comprehensive Jenkins plugin for automated Rust toolchain management, providing seamless integration of Rust and Cargo into your CI/CD pipelines.

[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)
[![Jenkins Plugin](https://img.shields.io/badge/Jenkins-Plugin-red.svg)](https://www.jenkins.io/)

## Overview

The Rust Plugin provides complete integration of Rust toolchains into Jenkins builds with support for automatic installation, version management, and Pipeline DSL steps. Whether you're building a simple library or a complex multi-crate workspace, this plugin streamlines your Rust CI/CD workflow.

## Key Features

### Core Capabilities
- **Automatic Installation**: Installs rustup and Rust toolchains automatically on any Jenkins agent
- **Version Management**: Support for stable, beta, nightly, and specific version numbers
- **Cross-Platform**: Full support for Windows, macOS, and Linux on both x86_64 and ARM64 architectures
- **Pipeline Steps**: Native pipeline steps for `cargo`, `cargoBuild`, `cargoTest`, `cargoClippy`, and `withRust`
- **Tool Integration**: Seamless integration with Jenkins Global Tool Configuration
- **Environment Management**: Automatic PATH, CARGO_HOME, and RUSTUP_HOME configuration

### Pipeline Steps

#### `cargo` - Run Cargo Commands
Execute any cargo command with full flexibility:
```groovy
cargo(command: 'build', args: '--release')
cargo(command: 'test')
cargo(command: 'clippy', args: '-- -D warnings')
```

#### `cargoBuild` - Build Your Project
Convenience step for building Rust projects:
```groovy
cargoBuild()                                    // Debug build
cargoBuild(release: true)                       // Release build
cargoBuild(release: true, features: 'extra')    // With features
```

#### `cargoTest` - Run Tests with JUnit Reports
Run tests and automatically publish JUnit results:
```groovy
cargoTest()                          // Run all tests
cargoTest(noCapture: true)           // Show test output
cargoTest(package: 'my-crate')       // Test specific package
```

#### `cargoClippy` - Lint Your Code
Run Clippy linter for code quality checks:
```groovy
cargoClippy()                              // Basic clippy
cargoClippy(denyWarnings: true)            // Fail on warnings
cargoClippy(allTargets: true)              // Check all targets
```

#### `withRust` - Use Specific Toolchain
Wrap commands to use a specific Rust installation:
```groovy
withRust(rustInstallationName: 'rust-nightly') {
    sh 'rustc --version'
    sh 'cargo build --release'
}
```

## Installation

### Via Jenkins Plugin Manager
1. Navigate to **Manage Jenkins** → **Manage Plugins**
2. Go to the **Available** tab
3. Search for "Rust Plugin"
4. Check the box and click **Install without restart**

### Manual Installation
1. Download the latest `.hpi` file from the [releases page](https://github.com/jenkinsci/rust-plugin/releases)
2. Go to **Manage Jenkins** → **Manage Plugins** → **Advanced**
3. Upload the `.hpi` file
4. Restart Jenkins

## Quick Start

### 1. Configure Rust Installation
Navigate to **Manage Jenkins** → **Global Tool Configuration** → **Rust Installations**:

```
Name: rust-stable
☑ Install automatically
  Version: stable
  ☑ Install rustup: true
```

### 2. Use in Pipeline
```groovy
pipeline {
    agent any
    
    tools {
        rust 'rust-stable'
    }
    
    stages {
        stage('Build') {
            steps {
                cargoBuild(release: true)
            }
        }
        
        stage('Test') {
            steps {
                cargoTest()
            }
        }
        
        stage('Lint') {
            steps {
                cargoClippy(denyWarnings: true)
            }
        }
    }
}
```

See [QUICKSTART.md](QUICKSTART.md) for more examples.

## Platform Support

The plugin fully supports all major operating systems and CPU architectures:

| Platform | x86_64 | ARM64 (aarch64) | Notes |
|----------|--------|-----------------|-------|
| **Windows** | ✅ | ✅ | PowerShell required for installation |
| **macOS** | ✅ | ✅ | Intel and Apple Silicon (M1/M2/M3/M4) |
| **Linux** | ✅ | ✅ | All major distributions |

The plugin automatically detects the system architecture and downloads the appropriate rustup installer.

## Configuration

### Global Tool Configuration

Configure Rust installations globally for reuse across jobs:

1. **Navigate**: **Manage Jenkins** → **Global Tool Configuration**
2. **Find**: **Rust Installations** section
3. **Add Installation**:
   - **Name**: Unique identifier (e.g., "rust-stable", "rust-1.75.0")
   - **Home Directory**: Installation path (auto-configured)
   - **Install automatically**: Enable for automatic installation

#### Auto-Installation Options

- **Rust Version**: Channel or version number
  - Channels: `stable`, `beta`, `nightly`
  - Specific versions: `1.75.0`, `1.76.0`
  - Dated toolchains: `nightly-2024-01-15`
- **Prefer Built-in Tools**: Use system Rust if available
- **Install rustup**: Automatically install rustup if not present

### Pipeline Configuration

#### Using Global Tools Block
```groovy
pipeline {
    agent any
    tools {
        rust 'rust-stable'
    }
    stages {
        stage('Build') {
            steps {
                cargoBuild()
            }
        }
    }
}
```

#### Explicit Installation Selection
```groovy
cargo(command: 'build', rustInstallationName: 'rust-nightly')
```

#### Environment Wrapper
```groovy
withRust(rustInstallationName: 'rust-1.75.0') {
    sh 'cargo build --release'
    sh 'cargo test'
}
```

## Advanced Usage

### Multi-Toolchain Builds
Test against multiple Rust versions:
```groovy
stage('Test Multiple Versions') {
    parallel {
        stage('Stable') {
            steps {
                withRust(rustInstallationName: 'rust-stable') {
                    sh 'cargo test'
                }
            }
        }
        stage('Nightly') {
            steps {
                withRust(rustInstallationName: 'rust-nightly') {
                    sh 'cargo test'
                }
            }
        }
    }
}
```

### Custom Cargo Commands
```groovy
// Format check
cargo(command: 'fmt', args: '-- --check')

// Generate documentation
cargo(command: 'doc', args: '--no-deps')

// Dependency tree
cargo(command: 'tree')

// Audit dependencies
cargo(command: 'audit')
```

### Workspace Builds
```groovy
// Build all workspace members
cargoBuild(workspace: true)

// Test specific package
cargoTest(package: 'my-package')

// Build with all features
cargoBuild(release: true, allFeatures: true)
```

## Requirements

- **Jenkins**: 2.387.3 or later
- **Java**: 11 or later
- **Network**: Internet access for downloading rustup and toolchains (if using auto-installation)
- **Windows**: PowerShell (for rustup installation)
- **Unix**: curl (for rustup installation)

## Troubleshooting

### Installation Fails

**Issue**: Rustup installation fails
- ✅ **Check**: Network connectivity to `rustup.rs`
- ✅ **Verify**: Installation directory is writable
- ✅ **Ensure**: System requirements (curl on Unix, PowerShell on Windows)
- ✅ **Review**: Jenkins system logs for detailed error messages

### Version Not Found

**Issue**: Specified Rust version not found
- ✅ **Validate**: Version format (use `stable`, `beta`, `nightly`, or semantic version like `1.75.0`)
- ✅ **Check**: Version exists on rustup (visit [rust-lang.org](https://www.rust-lang.org/))
- ✅ **Try**: Running `rustup toolchain list` on the agent

### PATH Issues

**Issue**: Rust binaries not found in PATH
- ✅ **Verify**: `CARGO_HOME` and `RUSTUP_HOME` environment variables are set
- ✅ **Check**: `bin` directory exists in installation path
- ✅ **Ensure**: Binaries have execute permissions (Unix/Linux)
- ✅ **Use**: `withRust` wrapper to guarantee correct environment

### Test Reports Not Published

**Issue**: JUnit test reports not appearing
- ✅ **Use**: `cargoTest()` step (not generic `cargo` step)
- ✅ **Check**: Tests are actually running (check console output)
- ✅ **Verify**: Workspace contains `target/nextest/ci/junit.xml` or `target/surefire-reports/`

## Development

### Building from Source
```bash
mvn clean package
```

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test
mvn test -Dtest=RustInstallerTest

# Run with debug logging
mvn test -X
```

### Local Development
```bash
# Start Jenkins with plugin
mvn hpi:run

# Access at http://localhost:8080/jenkins
```

See [TESTING.md](TESTING.md) for comprehensive testing guidelines.

## Contributing

We welcome contributions! Please follow these steps:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Make** your changes
4. **Add** tests for new functionality
5. **Ensure** all tests pass (`mvn test`)
6. **Commit** your changes (`git commit -m 'Add amazing feature'`)
7. **Push** to the branch (`git push origin feature/amazing-feature`)
8. **Open** a Pull Request

### Coding Standards
- Follow existing code style
- Add JavaDoc for public APIs
- Write unit tests for new features
- Update documentation as needed

## License

This plugin is licensed under the [MIT License](LICENSE).

```
MIT License

Copyright (c) 2026 RederSoft

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```

## Support & Resources

- **Documentation**: [QUICKSTART.md](QUICKSTART.md), [TESTING.md](TESTING.md)
- **Changelog**: [CHANGELOG.md](CHANGELOG.md)
- **Issues**: [GitHub Issue Tracker](https://github.com/jenkinsci/rust-plugin/issues)
- **Rust**: [rust-lang.org](https://www.rust-lang.org/)
- **Rustup**: [rustup.rs](https://rustup.rs/)
- **Jenkins**: [jenkins.io](https://www.jenkins.io/)

## Acknowledgments

Built with ❤️ for the Rust and Jenkins communities.

Special thanks to:
- The Rust team for creating an amazing language and toolchain
- The Jenkins project for providing an extensible CI/CD platform
- All contributors who have helped improve this plugin
