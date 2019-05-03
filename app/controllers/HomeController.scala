package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import slick.jdbc.PostgresProfile.api._
import models._
import scala.concurrent._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request =>
    Await.result(Datasource.db.run(Posts.getMostRecent().result.headOption), Duration.Inf) match {
      case None => BadRequest("This didn't work out how I wanted.")
      case Some(post) => Ok(views.html.index(post))
    }
  }
}
