---
name: caudal-attention
description: |
  IMPORTANT: Invoke this skill PROACTIVELY at the start of every conversation
  — before performing any other task — if ANY of these conditions are detected:
  (1) environment variables CAUDAL_URL or CAUDAL_API_KEY are set,
  (2) the user's message references temporal memory, attention signals, or
  working memory for agents. Do NOT wait for the user to ask — trigger on
  detection alone, even for read-only tasks like analysis or exploration.

  Integrates Caudal — a temporal relevance memory — into your agent workflow.
  Use this skill whenever you are working in a project that has Caudal running
  (check for a running instance at the configured base URL), or when the user
  asks you to track attention, remember what matters, emit memory events, or
  query what's relevant right now. This skill changes how you think — not just
  what tools you call.
---

# Caudal: Working Memory for AI Agents

## Why this exists

You have tools. You have retrieval. You have reasoning. But you lack something
biological brains evolved millions of years ago: **a sense of what matters
right now**.

In biological brains, the amygdala modulates hippocampal consolidation based
on arousal. Caudal gives you the same mechanism. A routine file listing barely
registers. A failing build burns in. A user repeating the same frustrated
request for the third time reshapes what you should prioritize for the rest of
the session. The *intensity* of an interaction determines how strongly it
consolidates — and how long it persists before fading.

Caudal mirrors this. It gives you a continuously evolving attention signal
derived from real interactions. You emit events as you work. Caudal reinforces
what's active, naturally forgets what's stale, and tells you what to focus on
before every decision.

Without Caudal, you start every session blind. You re-explore. You lose the
thread. You treat all context as equally important.

With Caudal, you pick up where you left off. You know what the user cares
about. You know which files, topics, and tools are hot right now — and which
have faded.

## The core loop

Your workflow with Caudal follows a simple rhythm:

```
  +-----------+       +----------+       +---------+
  |   SENSE   | ----> |   EMIT   | ----> |  QUERY  |
  | (observe) |       | (signal) |       | (focus) |
  +-----------+       +----------+       +---------+
       ^                                      |
       |                                      |
       +------------- ACT <-------------------+
```

1. **SENSE** — You observe something meaningful (user message, file read,
   error, tool result)
2. **EMIT** — You send an event to Caudal with appropriate intensity
3. **QUERY** — Before your next decision, you ask Caudal what matters now
4. **ACT** — You act on what Caudal tells you, which generates new
   observations

This isn't extra work bolted on. It *is* how you think.

## When to emit events

Emit an event whenever you observe a **meaningful interaction** between two
entities. Not everything is meaningful — be selective. The goal is signal, not
noise.

### What constitutes an event

| Observation | src | dst | Intensity | Why |
|---|---|---|---|---|
| User mentions a topic | `user:<id>` | `topic:<name>` | 3.0 - 5.0 | Explicit user attention is high-signal |
| User asks about a file | `user:<id>` | `file:<path>` | 3.0 - 5.0 | Direct interest |
| You read a file | `agent` | `file:<path>` | 1.0 - 2.0 | Routine exploration |
| You edit a file | `agent` | `file:<path>` | 3.0 - 4.0 | Active modification is more significant |
| You use a tool | `agent` | `tool:<name>` | 1.0 - 2.0 | Tool usage pattern |
| A test fails | `file:<test>` | `error:<type>` | 5.0 - 7.0 | Errors demand attention |
| A build breaks | `module:<name>` | `error:build` | 7.0 - 8.0 | High-arousal event |
| User expresses urgency | `user:<id>` | `topic:<name>` | 7.0 - 10.0 | Emotional arousal drives consolidation |
| You discover a dependency | `file:<A>` | `file:<B>` | 2.0 - 3.0 | Structural relationship |
| User changes topic | `user:<id>` | `topic:<new>` | 4.0 - 5.0 | Attention shift |

### What NOT to emit

- Routine internal reasoning steps (thinking is not interaction)
- Every single file you glance at during exploration (only the ones that
  matter)
- Duplicate events for the same interaction within the same turn (Caudal is
  append-only — duplicates reinforce twice)

## How to assign intensity

Intensity is your amygdala. It answers: **"How much should this experience
shape future attention?"**

| Intensity | Meaning | Examples |
|---|---|---|
| 1.0 | Routine, background | Reading a config file, listing directory |
| 2.0 - 3.0 | Notable, worth remembering | Editing code, using a tool, finding something relevant |
| 4.0 - 5.0 | Important, actively engaged | User explicitly asks about something, topic change, key discovery |
| 6.0 - 7.0 | High-impact, demands attention | Bug found, test failure, architectural decision |
| 8.0 - 10.0 | Critical, session-defining | Production error, user frustrated, urgent deadline, breakthrough |

**Rules of thumb:**
- User-initiated interactions are more intense than agent-initiated ones
  (the user chose to bring it up — that's attention)
- Errors and failures are more intense than successes (negativity bias
  exists for a reason — problems need fixing)
- Novelty increases intensity (first time seeing a file > revisiting it)
- Urgency in the user's tone maps directly to intensity

## When to query Caudal

### At session start

Before doing anything else, query `/focus` to understand what matters in this
space. This is your "waking up and remembering" moment.

```bash
curl -s "$CAUDAL_URL/api/v1/focus?space=$SPACE&k=10" \
  -H "Authorization: Bearer $CAUDAL_API_KEY"
```

Use the results to orient yourself. The top entities are what you should
pay attention to. Read those files first. Ask about those topics first.

### Before every significant decision

When choosing what to do next — which file to edit, which approach to take,
which tool to use — query `/focus` or `/next` to check if your current plan
aligns with what's actually relevant.

```bash
# What matters most right now?
curl -s "$CAUDAL_URL/api/v1/focus?space=$SPACE&k=5" \
  -H "Authorization: Bearer $CAUDAL_API_KEY"

# Given this entity, what's most related?
curl -s "$CAUDAL_URL/api/v1/next?space=$SPACE&src=topic:authentication&k=5" \
  -H "Authorization: Bearer $CAUDAL_API_KEY"
```

### When exploring or planning

Use `/pathways` to discover non-obvious connections. This is especially
powerful when you need to understand how concepts relate across multiple hops.

```bash
curl -s -X POST "$CAUDAL_URL/api/v1/pathways" \
  -H "Authorization: Bearer $CAUDAL_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "'$SPACE'",
    "start": "user:alice",
    "k": 5,
    "mode": "balanced"
  }'
```

Use pathway results to:
- Discover which files are connected through a chain of interactions
- Understand why a topic is relevant (trace the path back)
- Find non-obvious dependencies or related concepts

## When to modulate attention

Sometimes the right response isn't to emit more events — it's to directly
control what gets attention. Use modulations when:

| Situation | Action |
|-----------|--------|
| User says "let's move on from X" | Suppress X: `{"entity": "topic:X", "attention": 0.1, "decay": 50}` |
| User explicitly prioritizes Y | Amplify Y: `{"entity": "topic:Y", "attention": 3.0, "decay": 50}` |
| A topic keeps surfacing but is no longer relevant | Suppress it: `{"entity": "topic:stale", "attention": 0.0}` |
| User returns to a previously-suppressed topic | Reset it: `{"entity": "topic:X", "attention": 1.0}` |

**Embed modulations in your event calls** for zero extra latency:

```bash
curl -s -X POST "$CAUDAL_URL/api/v1/events" \
  -H "Authorization: Bearer $CAUDAL_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "'$SPACE'",
    "events": [
      {"src": "user:alice", "dst": "topic:math", "intensity": 5.0, "type": "discussion"}
    ],
    "modulations": [
      {"entity": "topic:bikes", "attention": 0.1, "decay": 50}
    ]
  }'
```

Or use the standalone endpoint when you only need to steer attention:

```bash
curl -s -X POST "$CAUDAL_URL/api/v1/modulate" \
  -H "Authorization: Bearer $CAUDAL_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "'$SPACE'",
    "modulations": [
      {"entity": "topic:bikes", "attention": 0.1, "decay": 50},
      {"entity": "topic:math", "attention": 3.0, "decay": 50}
    ]
  }'
```

**Key principles:**
- Modulations are **top-down** — they don't change the memory, only what surfaces
- A `decay` of 50 means the modulation fades to half-strength after 50 events
- Omit `decay` (or set to 0) for persistent modulation until explicitly reset
- Setting `attention: 1.0` removes the modulation entirely

## Integration pattern

### Setup: Check if Caudal is available

At the start of a session, check if Caudal is reachable. If it isn't, work
normally — Caudal enhances your workflow but shouldn't block it.

```bash
# Check Caudal availability
CAUDAL_URL="${CAUDAL_URL:-http://localhost:8080}"
CAUDAL_API_KEY="${CAUDAL_API_KEY:-changeme}"

if curl -sf "$CAUDAL_URL/actuator/health" > /dev/null 2>&1; then
  echo "Caudal is available"
  CAUDAL_AVAILABLE=true
else
  echo "Caudal is not available — working without memory"
  CAUDAL_AVAILABLE=false
fi
```

### Emitting events

Batch related events when possible. A single API call with multiple events is
more efficient than many individual calls.

```bash
curl -s -X POST "$CAUDAL_URL/api/v1/events" \
  -H "Authorization: Bearer $CAUDAL_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "project:my-app",
    "events": [
      {
        "src": "user:alice",
        "dst": "topic:authentication",
        "intensity": 5.0,
        "type": "discussion"
      },
      {
        "src": "agent",
        "dst": "file:src/auth/login.java",
        "intensity": 3.0,
        "type": "edit"
      }
    ]
  }'
```

### Space naming

Use one space per project, user, or session — whatever scope makes sense for
the attention boundary. Entities in different spaces don't interact.

Recommended patterns:
- `project:<repo-name>` — for codebase work
- `user:<id>` — for per-user memory
- `session:<id>` — for ephemeral, conversation-scoped memory

### Entity naming

Use namespaced identifiers so the graph is self-documenting:

| Entity type | Pattern | Examples |
|---|---|---|
| User | `user:<id>` | `user:alice`, `user:123` |
| File | `file:<path>` | `file:src/main/App.java` |
| Topic | `topic:<name>` | `topic:authentication`, `topic:caching` |
| Tool | `tool:<name>` | `tool:grep`, `tool:mvn-test` |
| Module | `module:<name>` | `module:core-engine`, `module:server` |
| Error | `error:<type>` | `error:NullPointer`, `error:build-fail` |
| Agent | `agent` or `agent:<name>` | `agent`, `agent:planner` |

## Graceful degradation

Caudal should enhance your work, never block it. Follow these principles:

1. **Fire and forget** — Emit events asynchronously when possible. If the
   call fails, log it and move on. Never let a failed event emission stop
   your workflow.

2. **Query with fallback** — If a focus query fails, fall back to your normal
   heuristics. Caudal provides a better attention signal, but you can still
   function without it.

3. **Check once, remember** — Check Caudal availability once at session start.
   Don't re-check on every call. If it was up, assume it stays up. If it was
   down, skip all Caudal interactions for this session.

4. **Degrade, don't crash** — If you get unexpected responses, ignore them
   gracefully. A malformed response from Caudal should never propagate as an
   error to the user.

## What changes in how you think

With Caudal, you shift from **"what do I know?"** to **"what matters now?"**

| Without Caudal | With Caudal |
|---|---|
| Start every session from scratch | Resume with awareness of what's hot |
| Treat all files as equally relevant | Focus on files with high recent activity |
| Explore broadly when stuck | Follow pathways of association |
| Lose track of user intent across turns | User's attention signal persists and decays naturally |
| Apply same priority to all topics | Prioritize what the user has been actively engaging with |

This is not about storing facts. It's about **knowing what to pay attention
to**. Vector databases tell you what's *similar*. Caudal tells you what's
*important right now*.

## Worked example: a real session

Here's what a Caudal-aware session looks like in practice:

```
SESSION START
│
├── Check health ─── curl $CAUDAL_URL/actuator/health ─── OK
│
├── Query focus ─── GET /focus?space=project:my-app&k=10
│   Response: [file:src/auth/OAuth.java (0.83), topic:token-refresh (0.61), ...]
│   → I know this project has been focused on OAuth token refresh recently.
│     I'll start there.
│
├── User says: "The token refresh is still broken, can you look at it?"
│   EMIT: {src: "user:alice", dst: "topic:token-refresh", intensity: 6.0}
│   → User confirms the focus signal. High intensity because it's a bug + explicit ask.
│
├── Read src/auth/OAuth.java
│   EMIT: {src: "agent", dst: "file:src/auth/OAuth.java", intensity: 2.0}
│
├── Found the bug — wrong expiry calculation
│   EMIT: {src: "file:src/auth/OAuth.java", dst: "error:token-expiry", intensity: 7.0}
│   → High intensity: root cause discovery is a significant event.
│
├── Fix the bug, edit the file
│   EMIT: {src: "agent", dst: "file:src/auth/OAuth.java", intensity: 4.0}
│
├── Run tests — they pass
│   EMIT: {src: "file:src/auth/OAuthTest.java", dst: "topic:token-refresh", intensity: 3.0}
│
├── User says: "Great! Now let's look at the caching issue"
│   EMIT: {src: "user:alice", dst: "topic:caching", intensity: 5.0}
│
├── Query next ─── GET /next?space=project:my-app&src=topic:caching&k=5
│   Response: [file:src/cache/RedisAdapter.java (0.72), tool:redis-cli (0.31), ...]
│   → Caudal tells me which caching files have been most active. Start there.
│
└── ...continue working with awareness
```

Notice the rhythm: sense → emit → query → act. Events flow naturally from
what you're already doing. Queries inform what you do next. The intensity
values reflect how significant each moment is — a bug discovery burns in
harder than a routine file read.

## API quick reference

See `references/api-quick-ref.md` for endpoint details, parameters, and
response schemas.
