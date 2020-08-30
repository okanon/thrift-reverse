package controllers


import play.api._
import play.api.mvc._
import play.api.http._
import play.api.libs.ws._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.{ Future, ExecutionContext }


@Singleton
abstract class RestTemplateController @Inject()
  (cc: ControllerComponents, ws: WSClient)(implicit ec: ExecutionContext) extends AbstractController(cc) {


  def reverseProxy(path: String) = Action.async(parse.raw) { request: Request[RawBuffer] =>

    var proxyRequest =
      ws.url("http://localhost:9090" + request.uri)
        .withFollowRedirects(false)
        .withMethod(request.method)
        .withHttpHeaders(
          request.headers.replace(HOST -> "localhost:9090").toSimpleMap.toSeq: _*)
        .addHttpHeaders(X_FORWARDED_FOR -> request.remoteAddress)
        .withQueryStringParameters(request.queryString.mapValues(_.head).toSeq: _*)

    if (request.hasBody) proxyRequest = proxyRequest.withBody(request.body.asBytes().get)

    proxyRequest.stream().map { response: WSResponse =>

      /**
       * WARN  akka.actor.ActorSystemImpl
       * Content-Type, Content-Length and Transfer-Encoding 
       */
      val headers = response.headers
                      .filterKeys(_ != TRANSFER_ENCODING)
                      .filterNot(v => v._1.equals(CONTENT_TYPE) || 
                                      v._1.equals(CONTENT_LENGTH) ||
                                      v._1.equals(DATE))

      response.status match {
        case 200 =>
          response.headers.get(CONTENT_LENGTH) match {
            case Some(Seq(length)) =>
              Ok.sendEntity(
                HttpEntity.Streamed(response.bodyAsSource, Some(length.toLong), Some(response.contentType)))
                  .withHeaders(headers.mapValues(_.mkString(",")).toSeq: _*)
            case _ =>
              Ok.chunked(response.bodyAsSource, Some(response.contentType))
                .withHeaders(headers.mapValues(_.mkString(",")).toSeq: _*)
          }
        case 304 =>
          NotModified
            .withHeaders(headers.mapValues(_.mkString(",")).toSeq: _*)
        case _ =>
          Status(response.status)
      }
    }
    .recover {
      case _ => BadGateway
    }
  }
}
