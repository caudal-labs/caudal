package caudal.examples;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class CaudalDemo {

    public static void main(String[] args) {
        String baseUrl = System.getenv().getOrDefault("CAUDAL_URL", "http://localhost:8080");
        String apiKey = System.getenv().getOrDefault("CAUDAL_API_KEY", "changeme");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        String authHeader = "Bearer " + apiKey;

        try {
            prettyPrint("Ingest events", post(client, URI.create(baseUrl + "/api/v1/events"), authHeader,
                    """
                    {
                        "space": "demo",
                        "events": [
                            {"src": "user:alice", "dst": "topic:machine-learning", "intensity": 3.0, "type": "interaction"},
                            {"src": "user:alice", "dst": "topic:python", "intensity": 2.0, "type": "interaction"},
                            {"src": "user:bob", "dst": "topic:machine-learning", "intensity": 5.0, "type": "interaction"},
                            {"src": "user:bob", "dst": "topic:data-pipelines", "intensity": 1.0, "type": "interaction"},
                            {"src": "user:carol", "dst": "topic:python", "intensity": 4.0, "type": "interaction"},
                            {"src": "user:carol", "dst": "topic:machine-learning", "intensity": 1.0, "type": "interaction"}
                        ]
                    }
                    """));

            prettyPrint("Focus (what matters now?)",
                    get(client, URI.create(baseUrl + "/api/v1/focus?space=demo&k=5"), authHeader));

            prettyPrint("Next hops from user:alice",
                    get(client, URI.create(baseUrl + "/api/v1/next?space=demo&src=user:alice&k=5"), authHeader));

            prettyPrint("Pathways from user:bob", post(client, URI.create(baseUrl + "/api/v1/pathways"), authHeader,
                    """
                    {
                        "space": "demo",
                        "start": "user:bob",
                        "k": 5,
                        "mode": "balanced"
                    }
                    """));

            prettyPrint("Health", get(client, URI.create(baseUrl + "/actuator/health"), authHeader));

            System.out.println();

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String get(HttpClient client, URI uri, String authHeader) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", authHeader)
                .GET()
                .build();
        return sendRequest(client, request);
    }

    private static String post(HttpClient client, URI uri, String authHeader, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return sendRequest(client, request);
    }

    private static String sendRequest(HttpClient client, HttpRequest request) throws Exception {
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 400) {
            throw new RuntimeException("HTTP " + response.statusCode() + ": " + response.body());
        }
        return response.body();
    }

    private static void prettyPrint(String label, String json) {
        System.out.println("\n=== " + label + " ===");
        System.out.println(formatJson(json));
    }

    private static String formatJson(String json) {
        StringBuilder result = new StringBuilder();
        int indent = 0;
        boolean inString = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
                result.append(c);
            } else if (!inString) {
                switch (c) {
                    case '{', '[' -> {
                        result.append(c);
                        result.append('\n');
                        indent += 2;
                        result.append(" ".repeat(indent));
                    }
                    case '}', ']' -> {
                        result.append('\n');
                        indent -= 2;
                        result.append(" ".repeat(indent));
                        result.append(c);
                    }
                    case ',' -> {
                        result.append(c);
                        result.append('\n');
                        result.append(" ".repeat(indent));
                    }
                    case ':' -> result.append(": ");
                    default -> {
                        if (!Character.isWhitespace(c)) {
                            result.append(c);
                        }
                    }
                }
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}
