# Caudal API Demo (Python)

A Python implementation of the Caudal API demo using the `requests` library.

## Prerequisites

- Python 3.10+
- `requests` library

## Quick Start

```bash
# Install requests if needed
pip install requests

# Run with defaults
python caudal/examples/python/CaudalDemo.py

# Or with arguments
python caudal/examples/python/CaudalDemo.py http://localhost:8080 changeme

# Or with environment variables
CAUDAL_URL=http://localhost:8080 CAUDAL_API_KEY=changeme python caudal/examples/python/CaudalDemo.py
```
Or with [uv](https://docs.astral.sh/uv/getting-started/installation/):

``` bash
uv run --with requests examples/python/CaudalDemo.py
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

- `requests` - HTTP client
