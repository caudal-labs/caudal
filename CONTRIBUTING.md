# Contributing to Caudal

Thank you for your interest in contributing! We welcome contributions from the community.

## Getting started

1. Fork the repository
2. Clone your fork and create a feature branch
3. Make sure you have Java 21+ and Maven 3.9+ installed

## Building

```bash
mvn clean verify
```

## Running tests

```bash
# Unit + integration tests (no Docker required)
mvn test

# Full test suite including Testcontainers (Docker required)
mvn verify
```

## Submitting changes

1. Create a branch from `main`
2. Make your changes with clear commit messages
3. Ensure all tests pass
4. Open a pull request with a description of the change

## Code style

- Follow existing code conventions
- Add tests for new functionality
- Keep the public API clean: no internal terminology in public JSON

## Reporting issues

Use GitHub Issues. Include:
- Steps to reproduce
- Expected vs actual behavior
- Version / environment details
