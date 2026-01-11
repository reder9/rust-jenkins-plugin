# Rust Plugin

A Jenkins plugin for automated Rust version management and installation.

## Overview

The Rust Plugin provides seamless integration of Rust toolchains into Jenkins builds. It supports:

- Automatic installation of Rust via rustup
- Multiple Rust version management
- Cross-platform support (Linux, macOS, Windows)
- Integration with Jenkins tool auto-installation
- Build wrapper for easy job configuration

## Features

- **Automatic Installation**: Installs rustup and Rust toolchains automatically
- **Version Management**: Support for stable, beta, nightly, and specific version numbers
- **Global Configuration**: Configure multiple Rust installations in Jenkins global settings
- **Build Integration**: Easy-to-use build wrapper for jobs
- **Path Management**: Automatically sets up PATH, CARGO_HOME, and RUSTUP_HOME environment variables

## Installation

1. Install the plugin through the Jenkins Plugin Manager
2. Navigate to **Manage Jenkins** → **Global Tool Configuration**
3. Find **Rust Installations** section
4. Add your Rust installations

## Configuration

### Global Configuration

1. Go to **Manage Jenkins** → **Global Tool Configuration**
2. Scroll to **Rust Installations**
3. Click **Add Rust Installation**
4. Configure:
   - **Name**: A unique name for this installation (e.g., "Rust 1.75")
   - **Home Directory**: Path to the Rust installation directory

### Tool Auto-Installation

The plugin supports automatic installation of Rust toolchains:

1. In the tool configuration, enable **Install automatically**
2. Configure the installer:
   - **Rust Version**: Specify version (e.g., "stable", "1.75.0", "nightly")
   - **Prefer Built-in Tools**: Use system Rust/Cargo if available
   - **Install rustup**: Automatically install rustup if not present

### Using in Jobs

#### Build Wrapper

1. In your job configuration, go to **Build Environment**
2. Check **Use Rust**
3. Select the Rust installation from the dropdown

The build wrapper will:
- Add Rust to PATH
- Set CARGO_HOME environment variable
- Set RUSTUP_HOME environment variable

## Supported Versions

The plugin supports:
- **Stable channel**: Latest stable Rust release
- **Beta channel**: Beta Rust release
- **Nightly channel**: Nightly Rust builds
- **Specific versions**: Semantic versioning (e.g., 1.75.0, 1.76.0)

## Requirements

- Jenkins 2.387.3 or later
- Java 11 or later
- Network access for downloading rustup and toolchains (if using auto-installation)

## Platform Support

- **Linux**: Full support via rustup
- **macOS**: Full support via rustup
- **Windows**: Full support via rustup (PowerShell required for installation)

## Development

### Building

```bash
mvn clean package
```

### Testing

```bash
mvn test
```

### Running Locally

```bash
mvn hpi:run
```

## Troubleshooting

### Installation Fails

- Ensure network connectivity for downloading rustup
- Check that the installation directory is writable
- Verify system requirements (curl on Unix, PowerShell on Windows)

### Version Not Found

- Ensure the specified version is valid
- Check rustup toolchain list: `rustup toolchain list`
- Use "stable", "beta", or "nightly" for channel names

### PATH Issues

- Verify CARGO_HOME and RUSTUP_HOME are set correctly
- Check that the bin directory exists in the installation path
- Ensure binaries have execute permissions

## Contributing

Contributions are welcome! Please:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This plugin is licensed under the MIT License.

## Support

For issues and feature requests, please use the [GitHub issue tracker](https://github.com/jenkinsci/rust-plugin/issues).

## Changelog

### 1.0.0

- Initial release
- Support for rustup installation
- Support for multiple Rust versions
- Build wrapper integration
- Cross-platform support
