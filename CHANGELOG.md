# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [Unreleased]

### Added
- Core memory engine with decay, reinforcement, ranking, and pruning
- REST API: POST /events, GET /focus, GET /next, POST /pathways
- API key authentication (Bearer/Token)
- PostgreSQL persistence: WAL + snapshots with Flyway migrations
- Automatic snapshot scheduling and retention
- Recovery on startup (snapshot + WAL replay)
- Micrometer metrics and Actuator health endpoints
- Docker support with docker-compose
- GitHub Actions CI pipeline
