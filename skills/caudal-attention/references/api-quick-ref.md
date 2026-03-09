# Caudal API Quick Reference

Base URL: `$CAUDAL_URL/api/v1` (default: `http://localhost:8080/api/v1`)

All endpoints require: `Authorization: Bearer $CAUDAL_API_KEY`

---

## POST /events — Emit interaction events

The primary write operation. Optionally includes attention modulations for
zero-latency top-down control.

**Request:**
```json
{
  "space": "project:my-app",
  "events": [
    {
      "src": "user:alice",
      "dst": "topic:caching",
      "intensity": 4.0,
      "type": "discussion",
      "timestamp": "2026-03-06T10:05:12Z",
      "attrs": {
        "channel": "chat",
        "sessionId": "sess-abc"
      }
    }
  ],
  "modulations": [
    {
      "entity": "topic:old-stuff",
      "attention": 0.1,
      "decay": 50
    }
  ]
}
```

**Event fields:**

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `space` | yes | — | Isolated memory namespace |
| `events` | yes | — | Array of event objects |
| `events[].src` | yes | — | Source entity identifier |
| `events[].dst` | yes | — | Destination entity identifier |
| `events[].intensity` | no | 1.0 | How impactful this event is (0.1 - 10.0) |
| `events[].type` | no | null | Semantic label (chat, edit, tool_use, etc.) |
| `events[].timestamp` | no | null | ISO-8601 original occurrence time (for auditing) |
| `events[].attrs` | no | null | Free-form key-value metadata |

**Modulation fields (optional):**

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `modulations` | no | — | Array of attention modulations to apply atomically with events |
| `modulations[].entity` | yes | — | Entity to modulate (e.g. `topic:bikes`) |
| `modulations[].attention` | yes | — | Multiplier: `0.0`=suppress, `1.0`=reset, `>1.0`=amplify |
| `modulations[].decay` | no | 0 | Events until half-strength. `0`=persistent |

**Response:** `202 Accepted`
```json
{
  "accepted": 1,
  "asOf": "2026-03-06T10:05:00Z"
}
```

---

## GET /focus — What matters right now?

Returns the top-k most relevant entities, ranked by decayed cumulative
interaction score.

**Parameters:**

| Param | Required | Default | Description |
|-------|----------|---------|-------------|
| `space` | yes | — | Space to query |
| `k` | no | 20 | Max entities to return |

**Example:**
```bash
curl "$CAUDAL_URL/api/v1/focus?space=project:my-app&k=5" \
  -H "Authorization: Bearer $CAUDAL_API_KEY"
```

**Response:**
```json
{
  "items": [
    { "id": "topic:caching", "score": 0.83 },
    { "id": "file:src/cache/Redis.java", "score": 0.61 },
    { "id": "user:alice", "score": 0.45 }
  ],
  "asOf": "2026-03-06T10:06:00Z"
}
```

---

## GET /next — What's associated with this entity?

Returns the top-k outgoing neighbors of a source entity, ranked by edge
strength.

**Parameters:**

| Param | Required | Default | Description |
|-------|----------|---------|-------------|
| `space` | yes | — | Space to query |
| `src` | yes | — | Source entity to explore from |
| `k` | no | 20 | Max neighbors to return |

**Example:**
```bash
curl "$CAUDAL_URL/api/v1/next?space=project:my-app&src=user:alice&k=5" \
  -H "Authorization: Bearer $CAUDAL_API_KEY"
```

**Response:** Same schema as `/focus`.

---

## POST /pathways — Discover multi-hop connections

Samples probabilistic walks through the association graph. Reveals indirect
connections that single-hop queries cannot surface.

**Request:**
```json
{
  "space": "project:my-app",
  "start": "user:alice",
  "k": 5,
  "mode": "balanced"
}
```

**Fields:**

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `space` | yes | — | Space to explore |
| `start` | yes | — | Starting entity |
| `k` | no | 10 | Max paths and entities to return |
| `mode` | no | balanced | `fast` / `balanced` / `deep` |

**Mode trade-offs:**

| Mode | Latency | Thoroughness | Use when |
|------|---------|-------------|----------|
| `fast` | Low | Approximate | Quick lookups, low-latency decisions |
| `balanced` | Medium | Good | Default for most workflows |
| `deep` | Higher | Thorough | Planning, root-cause analysis, distant associations |

**Response:**
```json
{
  "paths": [
    {
      "nodes": ["user:alice", "topic:caching", "file:src/cache/Redis.java"],
      "score": 0.34
    }
  ],
  "topEntities": [
    { "id": "topic:caching", "score": 0.72 },
    { "id": "file:src/cache/Redis.java", "score": 0.51 }
  ],
  "asOf": "2026-03-06T10:06:00Z"
}
```

---

## POST /modulate — Suppress or amplify entity attention

Directly control which entities surface in query results without changing the
underlying memory. Use this when you only need to steer attention (no events
to emit). For zero extra latency, prefer embedding modulations in `POST /events`.

**Request:**
```json
{
  "space": "project:my-app",
  "modulations": [
    {"entity": "topic:bikes", "attention": 0.1, "decay": 50},
    {"entity": "topic:math", "attention": 3.0, "decay": 50}
  ]
}
```

**Fields:**

| Field | Required | Default | Description |
|-------|----------|---------|-------------|
| `space` | yes | — | Space containing the entities |
| `modulations` | yes | — | Array of modulations (at least one) |
| `modulations[].entity` | yes | — | Entity to modulate |
| `modulations[].attention` | yes | — | Multiplier: `0.0`=fully suppress, `0.1`=strongly suppress, `1.0`=reset (remove modulation), `3.0`=triple score |
| `modulations[].decay` | no | 0 | Number of events until the modulation fades to half-strength. `0`=persistent until explicitly reset |

**Response:** `200 OK`
```json
{
  "applied": 2,
  "asOf": "2026-03-06T10:06:00Z"
}
```

**When to use modulate vs. embedded modulations:**

| Scenario | Approach |
|----------|----------|
| Emitting events AND steering attention | Embed `modulations` in `POST /events` |
| Only steering attention (no new events) | Use `POST /modulate` |
| User says "stop thinking about X" | Either — whichever fits your current call |

---

## GET /actuator/health — Health check

No authentication required.

```bash
curl "$CAUDAL_URL/actuator/health"
```

Returns `{"status": "UP"}` when healthy.
