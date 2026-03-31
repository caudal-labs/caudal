#!/usr/bin/env python3
"""
Caudal API demo — run against a local instance
Usage: python CaudalDemo.py [base_url] [api_key]
Or set environment variables: CAUDAL_URL, CAUDAL_API_KEY
"""

import json
import os
import sys
from urllib.parse import urlencode

import requests


def get_base_url_and_key() -> tuple[str, str]:
    """Get base URL and API key from args or environment."""
    base_url = os.environ.get("CAUDAL_URL", "http://localhost:8080")
    api_key = os.environ.get("CAUDAL_API_KEY", "changeme")

    if len(sys.argv) > 1:
        base_url = sys.argv[1]
    if len(sys.argv) > 2:
        api_key = sys.argv[2]

    return base_url, api_key


def get(client: requests.Session, url: str, headers: dict) -> dict:
    """Send GET request and return JSON response."""
    response = client.get(url, headers=headers)
    response.raise_for_status()
    return response.json()


def post(client: requests.Session, url: str, headers: dict, data: dict) -> dict:
    """Send POST request with JSON body and return JSON response."""
    response = client.post(url, headers=headers, json=data)
    response.raise_for_status()
    return response.json()


def pretty_print(label: str, data: dict) -> None:
    """Print label with formatted JSON."""
    print(f"\n=== {label} ===")
    print(json.dumps(data, indent=2))


def main() -> None:
    base_url, api_key = get_base_url_and_key()
    headers = {"Authorization": f"Bearer {api_key}"}

    client = requests.Session()

    try:
        events_payload = {
            "space": "demo",
            "events": [
                {
                    "src": "user:alice",
                    "dst": "topic:machine-learning",
                    "intensity": 3.0,
                    "type": "interaction",
                },
                {
                    "src": "user:alice",
                    "dst": "topic:python",
                    "intensity": 2.0,
                    "type": "interaction",
                },
                {
                    "src": "user:bob",
                    "dst": "topic:machine-learning",
                    "intensity": 5.0,
                    "type": "interaction",
                },
                {
                    "src": "user:bob",
                    "dst": "topic:data-pipelines",
                    "intensity": 1.0,
                    "type": "interaction",
                },
                {
                    "src": "user:carol",
                    "dst": "topic:python",
                    "intensity": 4.0,
                    "type": "interaction",
                },
                {
                    "src": "user:carol",
                    "dst": "topic:machine-learning",
                    "intensity": 1.0,
                    "type": "interaction",
                },
            ],
        }
        pretty_print(
            "Ingest events",
            post(client, f"{base_url}/api/v1/events", headers, events_payload),
        )

        pretty_print(
            "Focus (what matters now?)",
            get(client, f"{base_url}/api/v1/focus?space=demo&k=5", headers),
        )

        pretty_print(
            "Next hops from user:alice",
            get(
                client, f"{base_url}/api/v1/next?space=demo&src=user:alice&k=5", headers
            ),
        )

        pathways_payload = {
            "space": "demo",
            "start": "user:bob",
            "k": 5,
            "mode": "balanced",
        }
        pretty_print(
            "Pathways from user:bob",
            post(client, f"{base_url}/api/v1/pathways", headers, pathways_payload),
        )

        pretty_print("Health", get(client, f"{base_url}/actuator/health", headers))

        print()

    except requests.exceptions.HTTPError as e:
        print(f"HTTP Error: {e}", file=sys.stderr)
        sys.exit(1)
    except requests.exceptions.ConnectionError as e:
        print(f"Connection Error: {e}", file=sys.stderr)
        sys.exit(1)
    finally:
        client.close()


if __name__ == "__main__":
    main()
