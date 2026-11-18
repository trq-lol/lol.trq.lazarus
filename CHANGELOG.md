# Changelog

All notable changes are recorded here. The format follows [Keep a Changelog](https://keepachangelog.com/) and the
project uses [Semantic Versioning](https://semver.org/).

## [0.1.0] - Unreleased

### Added

- Project scaffold: Gradle build targeting Java 17 bytecode, LGPL-3.0-or-later license, CI.
- `Classpath`: reads `java.class.path` as URLs, the replacement for casting the system class loader to
  `URLClassLoader`.
