# Changelog

All notable changes to the Rust Jenkins Plugin will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-01-14

### Added
- **Core Features**
  - Automated Rust and Cargo installation via rustup
  - Support for multiple Rust installations in Jenkins global configuration
  - Build wrapper for easy job configuration
  - Integration with Jenkins tool auto-installation
  
- **Pipeline Support**
  - `cargo` step for running arbitrary cargo commands
  - `cargoBuild` convenience step for building Rust projects
  - `cargoTest` step with JUnit test report publishing
  - `cargoClippy` step for linting
  - `withRust` wrapper for using specific Rust installations in pipeline blocks
  
- **Cross-Platform Architecture Support**
  - Full support for Windows x86_64 and ARM64 (aarch64)
  - Full support for macOS Intel (x86_64) and Apple Silicon (ARM64/M1/M2/M3)
  - Full support for Linux x86_64 and ARM64 (aarch64)
  - Automatic architecture detection and installer selection
  - Proper handling of 32-bit ARM with warnings
  
- **Rust Version Management**
  - Support for stable, beta, and nightly Rust channels
  - Support for specific version numbers (e.g., 1.75.0)
  - Support for dated toolchains (e.g., nightly-2024-01-15)
  - Automatic toolchain installation via rustup
  - Default toolchain configuration
  
- **Environment Configuration**
  - Automatic PATH setup for Rust binaries
  - CARGO_HOME environment variable configuration
  - RUSTUP_HOME environment variable configuration
  
- **Testing & Quality**
  - Comprehensive test suite with 42+ cross-platform tests
  - Architecture normalization tests
  - Windows installer URL selection tests
  - Platform-specific executable naming tests
  - Integration tests for all pipeline steps

### Fixed
- JUnit test report publishing in `cargoTest` step
- Default toolchain not being set during rustup installation
- `withRust` wrapper properly setting environment variables
- Windows ARM64 installer URL selection (previously hardcoded to x86_64)

### Documentation
- Comprehensive README with installation instructions
- QUICKSTART.md for rapid setup
- TESTING.md with testing guidelines
- Pipeline step usage examples in Jenkinsfile
- Cross-platform compatibility matrix
- MIT LICENSE file

### Technical Details
- Minimum Jenkins version: 2.387.3
- Java version: 11 or later
- Plugin packaging: HPI (Jenkins plugin format)
- Build system: Apache Maven
- Test framework: JUnit with Jenkins Test Harness

## [Unreleased]

### Planned Features
- Cargo workspace support
- Cargo feature flags management
- Rust component installation (rustfmt, rust-analyzer, etc.)
- Custom rustup mirror configuration
- Cargo cache management

---

For more information, see the [README](README.md) and [issue tracker](https://github.com/jenkinsci/rust-plugin/issues).
