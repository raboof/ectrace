import $ivy.`org.slf4j:slf4j-nop:1.7.22`
import $ivy.`com.github.fommil::spray-json-shapeless:1.3.0`

import java.time.{ Instant, OffsetDateTime, ZoneId }
import java.time.format.DateTimeFormatter

import scala.io.Source
import scala.util.Try

import spray.json._

object ToLong {
  def unapply(in: String): Option[Long] = Try(in.toLong).toOption
}
implicit class RegexHelper(val sc: StringContext) extends AnyVal {
  def re: scala.util.matching.Regex = sc.parts.mkString.r
}

case class Creator(
  name: String = "timeline2har",
  version: String = "0.0.1",
  comment: String = ""
)
case class Browser(
  name: String = "timeline2har",
  version: String = "0.0.1",
  comment: String = ""
)
case class Page(
  /* ISO 8601 - YYYY-MM-DDThh:mm:ss.sTZD, e.g. 2009-07-24T19:20:30.45+01:00 */
  startedDateTime: String,
  id: String,
  title: String,
  pageTimings: PageTimings,
  comment: String = ""
)
case class PageTimings(onContentLoad: Long, onLoad: Long, comment: String = "")
case class Request(
  method: String = "",
  url: String,
  httpVersion: String = "",
  cookies: List[String] = Nil,
  headers: List[String] = Nil,
  queryString: List[String] = Nil,
  postData: Option[PostData] = None,
  headersSize: Long = -1,
  bodySize: Long = -1,
  comment: Option[String] = None
)
case class PostData()
case class Response(
  status: Int = 200,
  statusText: String = "OK",
  httpVersion: String = "HTTP/1.1",
  cookies: List[String] = Nil,
  headers: List[String] = Nil,
  content: Content = Content(),
  redirectURL: String = "",
  headersSize: Long = -1,
  bodySize: Long = -1,
  comment: Option[String] = None
)
case class Content(
  size: Long = -1,
  compression: Option[Long] = None,
  mimeType: String = "",
  text: Option[String] = None,
  comment: Option[String] = None
)
case class Cache()
case class Entry(
  pageref: Option[String],
  /* ISO 8601 - YYYY-MM-DDThh:mm:ss.sTZD, e.g. 2009-07-24T19:20:30.45+01:00 */
  startedDateTime: String,
  /* Total elapsed time of the request in milliseconds. This is the sum of all timings available in the timings object (i.e. not including -1 values) */
  time: Long,
  request: Request,
  response: Response,
  cache: Cache,
  timings: Timings,
  comment: String
)
object Entry{
    def apply(
      startedDateTime: Long,
      time: Long,
      request: Request,
      response: Response = Response(),
      cache: Cache = Cache(),
      timings: Timings,
      comment: String = ""): Entry =
      Entry(
        pageref = None,
        OffsetDateTime
          .ofInstant(Instant.ofEpochMilli(startedDateTime), ZoneId.of("Z"))
          .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        time,
        request,
        response,
        cache,
        timings,
        comment)
}

case class Timings(
  blocked: Long = -1,
  dns: Long = -1,
  connect: Long = -1,
  send: Long = -1,
  // 'wait' cannot override 'wait' in java.lang.Object :(
  waitt: Long = -1,
  receive: Long = -1,
  ssl: Long = -1,
  comment: String = ""
)

case class Log(
  version: String = "1.2",
  creator: Creator = Creator(),
  browser: Browser = Browser(),
  pages: List[Page] = Nil,
  entries: List[Entry] = Nil,
  comment: String = ""
)
case class HttpArchive(log: Log)

val entries = Source
  .fromInputStream(System.in)
  .getLines
  .toList
  .reverse
  .map {
    case re"(\d+)${ToLong(prepare)} (\d+)${ToLong(schedule)} (\d+)${ToLong(start)} (\d+)${ToLong(end)} (.*)$description" =>
      Entry(
        startedDateTime = prepare,
        time = end - prepare,
        Request(url = description),
        timings = Timings(
          connect = schedule - prepare,
          waitt = start - schedule,
          receive = end - start
        ),
        comment = description
      )
  }

object MyObj extends DefaultJsonProtocol with fommil.sjs.FamilyFormats {
  implicit object TimingsFormat extends JsonFormat[Timings] {
    def read(json: JsValue): Timings = ???
    def write(timings: Timings): JsValue = JsObject(
      "blocked" -> JsNumber(timings.blocked),
      "dns" -> JsNumber(timings.dns),
      "connect" -> JsNumber(timings.connect),
      "send" -> JsNumber(timings.send),
      "wait" -> JsNumber(timings.waitt),
      "receive" -> JsNumber(timings.receive),
      "ssl" -> JsNumber(timings.ssl),
      "comment" -> JsString(timings.comment)
    )
  }

  val json = HttpArchive(Log(
    entries = entries
  )).toJson
}

println(MyObj.json)
