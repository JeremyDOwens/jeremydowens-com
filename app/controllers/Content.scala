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
class Content @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def createPostPage() = withAccessLevel(10) { user =>  implicit request =>
    Ok(views.html.createpost(Some(user))(None))
  }
}
