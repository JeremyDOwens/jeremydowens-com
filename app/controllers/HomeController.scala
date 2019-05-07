package controllers

import javax.inject._
import play.api.mvc._
import slick.jdbc.PostgresProfile.api._
import models._
import scala.concurrent._
import scala.concurrent.duration._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action {  implicit request =>
    Await.result(Datasource.db.run(Posts.getMostRecent(3).result), Duration.Inf) match {
      case Nil => BadRequest("This didn't work out how I wanted.")
      case post => {
        val uname = request.session.get("username")
        if (uname.isDefined)
          Ok(views.html.index(post)(Users.findActive(uname.get)))
        else
          Ok(views.html.index(post)(None))
      }
    }
  }
}
