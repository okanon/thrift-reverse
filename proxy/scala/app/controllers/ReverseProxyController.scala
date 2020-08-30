package controllers


import play.api._
import play.api.mvc._
import play.api.libs.ws._

import javax.inject.{ Inject, Singleton }
import scala.concurrent.ExecutionContext


@Singleton
class ReverseProxyController @Inject()
  (cc: ControllerComponents, ws: WSClient)(implicit ec: ExecutionContext) extends RestTemplateController(cc, ws) {

  
  override def reverseProxy(path: String) = Action.async(parse.raw) { 
    implicit request: Request[RawBuffer] => super.reverseProxy(path).apply(request) 
  }
}
