#!/usr/bin/env node
/**
 * Caudal API demo — run against a local instance
 * Usage: node CaudalDemo.js [base_url] [api_key]
 * Or set environment variables: CAUDAL_URL, CAUDAL_API_KEY
 */

const BASE_URL = process.env.CAUDAL_URL || process.argv[2] || "http://localhost:8080";
const API_KEY = process.env.CAUDAL_API_KEY || process.argv[3] || "changeme";

const headers = {
  Authorization: `Bearer ${API_KEY}`,
  "Content-Type": "application/json",
};

async function get(url) {
  const response = await fetch(url, { method: "GET", headers });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${await response.text()}`);
  }
  return response.json();
}

async function post(url, body) {
  const response = await fetch(url, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${await response.text()}`);
  }
  return response.json();
}

function prettyPrint(label, data) {
  console.log(`\n=== ${label} ===`);
  console.log(JSON.stringify(data, null, 2));
}

async function main() {
  try {
    const eventsPayload = {
      space: "demo",
      events: [
        { src: "user:alice", dst: "topic:machine-learning", intensity: 3.0, type: "interaction" },
        { src: "user:alice", dst: "topic:python", intensity: 2.0, type: "interaction" },
        { src: "user:bob", dst: "topic:machine-learning", intensity: 5.0, type: "interaction" },
        { src: "user:bob", dst: "topic:data-pipelines", intensity: 1.0, type: "interaction" },
        { src: "user:carol", dst: "topic:python", intensity: 4.0, type: "interaction" },
        { src: "user:carol", dst: "topic:machine-learning", intensity: 1.0, type: "interaction" },
      ],
    };
    prettyPrint("Ingest events", await post(`${BASE_URL}/api/v1/events`, eventsPayload));

    prettyPrint("Focus (what matters now?)", await get(`${BASE_URL}/api/v1/focus?space=demo&k=5`));

    prettyPrint("Next hops from user:alice", await get(`${BASE_URL}/api/v1/next?space=demo&src=user:alice&k=5`));

    const pathwaysPayload = { space: "demo", start: "user:bob", k: 5, mode: "balanced" };
    prettyPrint("Pathways from user:bob", await post(`${BASE_URL}/api/v1/pathways`, pathwaysPayload));

    prettyPrint("Health", await get(`${BASE_URL}/actuator/health`));

    console.log();
  } catch (error) {
    console.error(`Error: ${error.message}`);
    process.exit(1);
  }
}

main();
