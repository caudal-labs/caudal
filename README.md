# Caudal

> **Caudal is an attention engine for AI agents.**
> It tells an agent what matters *right now*.

Modern agents have tools, vector search, and long‑term storage — but they still behave strangely:

* they bring up old topics
* they lose track of current user intent
* they repeat irrelevant actions
* they need custom heuristics for recency and prioritization

The missing piece is not more knowledge.

It is **attention**.

Caudal provides a continuously evolving “attention signal” derived from real interactions.
Agents send events. Caudal learns which entities and relationships are currently important, and naturally forgets what is no longer relevant.

No prompts.
No fine‑tuning.
No heuristics.

Just behavior → relevance.

**Documentation:** [caudal-labs.com](https://www.caudal-labs.com/)

---

## What Caudal is

Caudal is not:

* a database
* a vector store
* a conversation history

It is a new layer:

| System         | Answers                         |
| -------------- | ------------------------------- |
| SQL / Graph DB | What is true?                   |
| Vector DB      | What is similar?                |
| LLM            | What can I reason about?        |
| **Caudal**     | **What should I focus on now?** |

Caudal stores interaction traces and continuously computes a ranked relevance field with built‑in forgetting.

---

## Quickstart (Docker)

```bash
# 1) start dependencies (Postgres) + Caudal
cd docker
docker compose up -d

# 2) check health
curl http://localhost:8080/actuator/health
```

Default dev mode allows requests without auth when `AUTH_DISABLED=true`.

---

## How an agent uses Caudal

An agent uses Caudal in two simple steps:

### 1) Write: emit events while working

Whenever the agent observes meaningful activity, it sends events:

* user mentions a topic
* a tool is used
* a document is retrieved
* a task succeeds or fails

```bash
curl -X POST http://localhost:8080/api/v1/events \
  -H "Authorization: Bearer <api_key>" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "user:123",
    "events": [
      {
        "timestamp": "2026-02-28T10:05:12Z",
        "src": "user:123",
        "dst": "topic:car-buying",
        "type": "chat",
        "intensity": 2.0
      },
      {
        "timestamp": "2026-02-28T10:05:30Z",
        "src": "agent:planner",
        "dst": "tool:car-comparison",
        "type": "tool_use",
        "intensity": 1.0
      }
    ]
  }'
```

Caudal interprets these as **signals of attention**, not facts.

### 2) Read: ask what matters now

Before deciding what to do next, the agent queries Caudal:

```bash
curl "http://localhost:8080/api/v1/focus?space=user:123&k=5" \
  -H "Authorization: Bearer <api_key>"
```

```json
{
  "asOf": "2026-02-28T10:06:00Z",
  "items": [
    { "id": "topic:car-buying", "score": 0.83 },
    { "id": "topic:stroller", "score": 0.61 },
    { "id": "topic:cycling", "score": 0.22 }
  ]
}
```

The agent now knows what to prioritize in:

* conversation
* planning
* tool selection
* retrieval

### 3) Follow associations

Agents can also explore what is likely to come next:

```bash
curl "http://localhost:8080/api/v1/next?space=user:123&src=topic:car-buying&k=5" \
  -H "Authorization: Bearer <api_key>"
```

```json
{
  "asOf": "2026-02-28T10:06:00Z",
  "items": [
    { "id": "user:123", "score": 0.83 },
    { "id": "tool:car-comparison", "score": 0.45 },
    { "id": "topic:stroller", "score": 0.12 }
  ]
}
```

This enables recommendations, coherent planning, and contextual tool use.

### 4) Explore multi-hop pathways (planning & explanations)

For deeper associations across several steps, agents can ask Caudal to sample pathways starting from an entity. This helps with planning, recommendations, and explaining *why* something is relevant.

```bash
curl -X POST http://localhost:8080/api/v1/pathways \
  -H "Authorization: Bearer <api_key>" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "user:123",
    "start": "user:123",
    "k": 5,
    "mode": "deep"
  }'
```

The optional `mode` parameter controls exploration depth:

| Mode         | Description                              |
| ------------ | ---------------------------------------- |
| `"fast"`     | Fewer samples, shorter walks — low latency |
| `"balanced"` | Good default (used when `mode` is omitted) |
| `"deep"`     | More samples, longer walks — thorough      |

Example response:

```json
{
  "asOf": "2026-02-28T10:06:00Z",
  "topEntities": [
    { "id": "topic:car-buying", "score": 0.72 },
    { "id": "brand:toyota", "score": 0.51 },
    { "id": "model:yaris_cross", "score": 0.44 }
  ],
  "paths": [
    { "nodes": ["user:123","topic:car-buying","brand:toyota","model:yaris_cross"], "score": 0.34 },
    { "nodes": ["user:123","topic:car-buying","doc:comparison_guide"], "score": 0.28 }
  ]
}
```

Use cases:
- choose the next best tool or document
- generate coherent suggestions
- provide human-readable explanations of relevance ("because you were looking at cars → Toyota → Yaris Cross")

This enables agents to discover non‑obvious connections between entities — useful for proactive suggestions, root‑cause analysis, and planning across multiple domains.

### 5) Modulate attention (suppress or amplify)

Sometimes you need to tell Caudal "stop thinking about X, focus on Y" without erasing the memory of X. Attention modulation lets you suppress or amplify how much an entity appears in query results.

```bash
# Inline with events (recommended — zero extra latency):
curl -X POST http://localhost:8080/api/v1/events \
  -H "Authorization: Bearer <api_key>" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "user:123",
    "events": [
      {"src": "user:123", "dst": "topic:math", "intensity": 3.0}
    ],
    "modulations": [
      {"entity": "topic:bikes", "attention": 0.1, "decay": 50},
      {"entity": "topic:math", "attention": 3.0, "decay": 50}
    ]
  }'

# Or standalone:
curl -X POST http://localhost:8080/api/v1/modulate \
  -H "Authorization: Bearer <api_key>" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "user:123",
    "modulations": [
      {"entity": "topic:bikes", "attention": 0.1, "decay": 50}
    ]
  }'
```

| Attention value | Effect |
|----------------|--------|
| `0.0` | Fully suppress — entity disappears from results |
| `0.1` | Strongly suppress — 10% of normal |
| `1.0` | Normal (resets any modulation) |
| `3.0` | Triple the score |

The `decay` field controls how many events until the modulation fades to half strength. Omit it for a persistent modulation.

---

## Why this improves agents

Without Caudal, agent developers implement:

* recency windows
* TTL memory
* custom ranking formulas
* manual heuristics

With Caudal:

* recent behavior reinforces relevance
* old context fades naturally
* focus emerges automatically

Caudal gives agents **working memory**.
Vector databases give agents recall.
LLMs give agents reasoning.

All three together produce far more stable behavior.

---

## API Overview

Base path: `/api/v1`

* `POST /events` — ingest interaction events (append‑only)
* `GET /focus?space=&k=` — ranked items that matter now
* `GET /next?space=&src=&k=` — ranked next hops from an entity
* `POST /pathways` — multi‑hop associations via sampled walks
* `POST /modulate` — suppress or amplify entity attention

Auth: `Authorization: Bearer <api_key>` (or `Token <api_key>`). Dev mode can disable auth.

---

## Architecture (high level)

* **core‑engine (pure Java):** decay, reinforcement, ranking, pathways
* **server (Spring Boot):** REST, auth, persistence, metrics
* **PostgreSQL:** event log (WAL) + periodic snapshots

Time is handled internally via discrete buckets for deterministic recovery and testing.

---

## Development

Requirements:

* Java 21 (or 17+)
* Docker (for Postgres/Testcontainers)

```bash
# Start Postgres
cd docker && docker compose up -d

# Run the application
mvn spring-boot:run -pl server

# Run all tests
mvn clean verify
```

Quality gates include unit tests, integration tests, and Testcontainers DB tests.

---

## Docker image

Caudal uses [Spring Boot Buildpacks](https://docs.spring.io/spring-boot/reference/packaging/container-images/cloud-native-buildpacks.html) to produce an OCI image — no Dockerfile needed.

```bash
# Build the image locally
mvn spring-boot:build-image -pl server -DskipTests

# Run the full stack (Postgres + Caudal)
cd docker && docker compose --profile full up -d
```

The image is published to GHCR as `ghcr.io/caudal-labs/caudal-server`.

---

## Claude Code Skill

Caudal ships a **Claude Code skill** (`caudal-attention`) that integrates Caudal's temporal attention signal directly into Claude's workflow. Once installed, Claude emits events as it works and queries what matters now before every decision — so focus emerges from real interactions and naturally fades when no longer relevant, instead of relying on heuristics or treating all context as equally important.

### Install in Claude Code

```bash
/plugin install caudal-attention@caudal-skills
```

### Set environment variables

The skill connects to your Caudal instance via environment variables:

```bash
export CAUDAL_URL=http://localhost:8080   # your Caudal server URL
export CAUDAL_API_KEY=your-api-key        # your API key (omit if auth is disabled)
```

### Recommended: add to your project's `CLAUDE.md`

For reliable activation at the start of every session, add this line to your project's `CLAUDE.md`:

```
At the start of every conversation, invoke the `caudal-attention` skill before doing anything else.
```

This ensures Claude uses Caudal's temporal memory in every session, not just when it happens to notice the skill trigger.

---

## Contributing

We welcome issues and PRs! Please read `CONTRIBUTING.md` and follow the code style and test guidelines. Good first issues are labeled in the tracker.

---

## License

Apache License 2.0
