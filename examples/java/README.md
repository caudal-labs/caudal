# Caudal API Demo (Java)

A Java 21 implementation of the Caudal API demo using `java.net.http.HttpClient`.

## Prerequisites

- Java 21 or later

## Quick Start

```bash
# Compile
cd examples/java && mkdir out
javac --release 21 -d out src/caudal/examples/CaudalDemo.java

# Run with defaults
java -cp out caudal.examples.CaudalDemo

# Or with environment variables
CAUDAL_URL=http://localhost:8080 CAUDAL_API_KEY=changeme java -cp out caudal.examples.CaudalDemo
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

None — uses only standard library classes (`java.net.http.HttpClient`, built-in JSON formatting).
