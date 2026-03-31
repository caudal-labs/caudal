# Caudal API Demo (Scala 3)

A Scala 3 implementation of the Caudal API demo using [scala-cli](https://scala-cli.virtuslab.org/).

## Prerequisites

- [scala-cli](https://scala-cli.virtuslab.org/) installed

## Quick Start

```bash
# Run with defaults (http://localhost:8080, API key: changeme)
scala-cli run CaudalDemo.scala

# Or with environment variables
CAUDAL_URL=http://localhost:8080 CAUDAL_API_KEY=changeme scala-cli run CaudalDemo.scala
```

## Configuration

| Environment Variable | Default | Description |
|---------------------|---------|-------------|
| `CAUDAL_URL` | `http://localhost:8080` | Base URL of Caudal server |
| `CAUDAL_API_KEY` | `changeme` | API key for authentication |

## Features

- Ingest events into the `demo` space
- Query focus items
- Get next hops from a source node
- Calculate pathways from a starting node
- Health check

## Dependencies

Managed by scala-cli using directives:
- `sttp.client3` - HTTP client
- `circe` - JSON handling
