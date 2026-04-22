package org.reportflow.aboutMe

import zio._

import zio.http._

object Main extends ZIOAppDefault {

  /**
   * Creates an HTTP app that only serves static files from resources via
   * "/static". For paths other than the resources directory, see
   * [[zio.http.Middleware.serveDirectory]].
   */

  private val appRoutes: Routes[Any, Response] = Routes(
    Method.GET / Root -> handler(Response.redirect(URL.root / "index.html")),
    Method.GET / "ping" -> handler(Response.text("pong"))


      )


  private val routes = appRoutes @@ Middleware.serveResources(Path.empty, "public")

  override def run: ZIO[Any, Throwable, Nothing] = Server
    .serve(routes)
    .provide(Server.defaultWithPort(10000))
}
