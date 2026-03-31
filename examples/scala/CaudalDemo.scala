//> using scala "3.5.2"
//> using dep "com.softwaremill.sttp.client3::core:3.9.8"
//> using dep "com.softwaremill.sttp.client3::circe:3.9.8"
//> using dep "io.circe::circe-generic:0.14.6"

package caudal.examples

import sttp.client3.*
import sttp.client3.circe.*
import sttp.model.Uri
import io.circe.*
import io.circe.generic.auto.*
import io.circe.syntax.*

object CaudalDemo extends App:

  val baseUrl = sys.env.getOrElse("CAUDAL_URL", "http://localhost:8080")
  val apiKey  = sys.env.getOrElse("CAUDAL_API_KEY", "changeme")

  private given Backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  private def authHeader: Map[String, String] = 
    Map("Authorization" -> s"Bearer $apiKey")

  private def post[T: Encoder.AsRoot](uri: Uri, body: T): String =
    val request = basicRequest
      .post(uri)
      .headers(authHeader)
      .contentType("application/json")
      .body(body.asJson.noSpaces)
    request.send().body.fold(sys.error, identity)

  private def get(uri: Uri): String =
    val request = basicRequest
      .get(uri)
      .headers(authHeader)
    request.send().body.fold(sys.error, identity)

  private def prettyPrint(label: String, json: String): Unit =
    println(s"\n=== $label ===")
    io.circe.jawn.decode[Json](json).foreach: j =>
      println(j.printWith(Printer.spaces2))

  val eventsUri   = Uri.parse(s"$baseUrl/api/v1/events").getOrElse(sys.error("Invalid URI"))
  val focusUri    = Uri.parse(s"$baseUrl/api/v1/focus").getOrElse(sys.error("Invalid URI"))
  val nextUri     = Uri.parse(s"$baseUrl/api/v1/next").getOrElse(sys.error("Invalid URI"))
  val pathwaysUri = Uri.parse(s"$baseUrl/api/v1/pathways").getOrElse(sys.error("Invalid URI"))
  val healthUri   = Uri.parse(s"$baseUrl/actuator/health").getOrElse(sys.error("Invalid URI"))

  val ingestRequest = IngestRequest(
    space = "demo",
    events = List(
      EventItem(src = "user:alice",   dst = "topic:machine-learning", intensity = 3.0, `type` = "interaction"),
      EventItem(src = "user:alice",   dst = "topic:python",            intensity = 2.0, `type` = "interaction"),
      EventItem(src = "user:bob",     dst = "topic:machine-learning", intensity = 5.0, `type` = "interaction"),
      EventItem(src = "user:bob",     dst = "topic:data-pipelines",   intensity = 1.0, `type` = "interaction"),
      EventItem(src = "user:carol",   dst = "topic:python",            intensity = 4.0, `type` = "interaction"),
      EventItem(src = "user:carol",   dst = "topic:machine-learning", intensity = 1.0, `type` = "interaction")
    )
  )

  prettyPrint("Ingest events", post(eventsUri, ingestRequest))

  prettyPrint("Focus (what matters now?)", 
    get(focusUri.withParams(Map("space" -> "demo", "k" -> "5"))))

  prettyPrint("Next hops from user:alice", 
    get(nextUri.withParams(Map("space" -> "demo", "src" -> "user:alice", "k" -> "5"))))

  val pathwaysRequest = PathwayQuery(space = "demo", start = "user:bob", k = 5, mode = "balanced")
  prettyPrint("Pathways from user:bob", post(pathwaysUri, pathwaysRequest))

  prettyPrint("Health", get(healthUri))

  println()

end CaudalDemo

case class IngestRequest(space: String, events: List[EventItem])
case class EventItem(src: String, dst: String, intensity: Double, `type`: String = "interaction")
case class PathwayQuery(space: String, start: String, k: Int, mode: String)
