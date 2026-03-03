#!/usr/bin/env bash
# Caudal API demo — run against a local instance
# Usage: ./demo.sh [base_url] [api_key]

BASE="${1:-http://localhost:8080}"
KEY="${2:-changeme}"
AUTH="Authorization: Bearer $KEY"

echo "=== Ingest events ==="
curl -s -X POST "$BASE/api/v1/events" \
  -H "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "demo",
    "events": [
      {"src": "user:alice", "dst": "topic:machine-learning", "weight": 3.0, "type": "interaction"},
      {"src": "user:alice", "dst": "topic:python", "weight": 2.0, "type": "interaction"},
      {"src": "user:bob", "dst": "topic:machine-learning", "weight": 5.0, "type": "interaction"},
      {"src": "user:bob", "dst": "topic:data-pipelines", "weight": 1.0, "type": "interaction"},
      {"src": "user:carol", "dst": "topic:python", "weight": 4.0, "type": "interaction"},
      {"src": "user:carol", "dst": "topic:machine-learning", "weight": 1.0, "type": "interaction"}
    ]
  }' | jq .

echo ""
echo "=== Focus (what matters now?) ==="
curl -s "$BASE/api/v1/focus?space=demo&k=5" \
  -H "$AUTH" | jq .

echo ""
echo "=== Next hops from user:alice ==="
curl -s "$BASE/api/v1/next?space=demo&src=user:alice&k=5" \
  -H "$AUTH" | jq .

echo ""
echo "=== Pathways from user:bob ==="
curl -s -X POST "$BASE/api/v1/pathways" \
  -H "$AUTH" \
  -H "Content-Type: application/json" \
  -d '{
    "space": "demo",
    "start": "user:bob",
    "k": 5,
    "mode": "balanced"
  }' | jq .

echo ""
echo "=== Health ==="
curl -s "$BASE/actuator/health" | jq .
