# Caudal API Demo (JavaScript/TypeScript)

JavaScript and TypeScript implementations of the Caudal API demo using native `fetch`.

## Prerequisites

- **JavaScript**: Node.js 18+ (native fetch)
- **TypeScript**: Node.js 18+, TypeScript, and `tsx` or `ts-node`

## Quick Start

### JavaScript

```bash
# Run with defaults
node caudal/examples/js/CaudalDemo.js

# Or with arguments
node caudal/examples/js/CaudalDemo.js http://localhost:8080 changeme

# Or with environment variables
CAUDAL_URL=http://localhost:8080 CAUDAL_API_KEY=changeme node caudal/examples/js/CaudalDemo.js
```

### TypeScript

```bash
# Install TypeScript and types
npm install --save-dev typescript @types/node tsx

# Run with defaults
npx tsx caudal/examples/js/CaudalDemo.ts

# Or with arguments
npx tsx caudal/examples/js/CaudalDemo.ts http://localhost:8080 changeme

# Or with environment variables
CAUDAL_URL=http://localhost:8080 CAUDAL_API_KEY=changeme npx tsx caudal/examples/js/CaudalDemo.ts
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

## TypeScript Benefits

- Full type safety with interfaces
- Generic typed HTTP functions
- Better IDE support and autocomplete

## Dependencies

**JavaScript**: None (uses native `fetch`)  
**TypeScript**: `typescript`, `@types/node`, `tsx` (for running)
