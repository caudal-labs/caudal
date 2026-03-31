#!/usr/bin/env node
/**
 * Caudal API demo — run against a local instance
 * Usage: npx tsx CaudalDemo.ts [base_url] [api_key]
 * Or set environment variables: CAUDAL_URL, CAUDAL_API_KEY
 */

interface EventItem {
  src: string;
  dst: string;
  intensity: number;
  type: string;
}

interface EventsPayload {
  space: string;
  events: EventItem[];
}

interface PathwayPayload {
  space: string;
  start: string;
  k: number;
  mode: string;
}

const BASE_URL = process.env.CAUDAL_URL || process.argv[2] || "http://localhost:8080";
const API_KEY = process.env.CAUDAL_API_KEY || process.argv[3] || "changeme";

const headers = {
  Authorization: `Bearer ${API_KEY}`,
  "Content-Type": "application/json",
};

async function get<T>(url: string): Promise<T> {
  const response = await fetch(url, { method: "GET", headers });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${await response.text()}`);
  }
  return response.json() as Promise<T>;
}

async function post<T>(url: string, body: unknown): Promise<T> {
  const response = await fetch(url, {
    method: "POST",
    headers,
    body: JSON.stringify(body),
  });
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${await response.text()}`);
  }
  return response.json() as Promise<T>;
}

function prettyPrint<T>(label: string, data: T): void {
  console.log(`\n=== ${label} ===`);
  console.log(JSON.stringify(data, null, 2));
}

async function main(): Promise<void> {
  try {
    const eventsPayload: EventsPayload = {
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

    const pathwaysPayload: PathwayPayload = { space: "demo", start: "user:bob", k: 5, mode: "balanced" };
    prettyPrint("Pathways from user:bob", await post(`${BASE_URL}/api/v1/pathways`, pathwaysPayload));

    prettyPrint("Health", await get(`${BASE_URL}/actuator/health`));

    console.log();
  } catch (error) {
    console.error(`Error: ${(error as Error).message}`);
    process.exit(1);
  }
}

main();
