package controllers

import javax.inject._
import play.api.mvc._
import slick.jdbc.PostgresProfile.api._
import models._
import scala.concurrent._
import scala.concurrent.duration._
import models._
/**
 * This controller will handle the requests for creating and editing content on the site.
 */
@Singleton
class Content @Inject()(cc: ControllerComponents) extends AbstractController(cc) with Secured {

  val privs = 10; // defining the minimum access level for managing content

  //Serve pages for content creation

  //GET a page used to define and create a new post
  def createPostPage = withAccessLevel(privs) { user =>  implicit request =>
    Ok(views.html.createpost(Some(user))(None))
  }

  //GET a page used to define and create a new project
  def createProjectPage = withAccessLevel(privs) { user =>  implicit request =>
    Ok(views.html.createproject(Some(user))(None))
  }

  //Serve pages for content viewing and/or editing

  //GET a public view page for the post, which allows owners and admins to edit
  def viewPostPage(id: Int) = Action {  implicit request =>
    Await.result(Datasource.db.run(Posts.posts.filter(_.id === id).result.headOption), Duration.Inf) match {
      case None => BadRequest("This didn't work out how I wanted.")
      case Some(post) => {
        val uname = request.session.get("username")
        if (uname.isDefined)
          Ok(views.html.viewpost(Users.findActive(uname.get))(post))
        else
          Ok(views.html.viewpost(None)(post))
      }
    }
  }

  //GET a public view page for the project, which allows owners and admins to edit
  def viewProjectPage(id: Int) = Action {  implicit request =>
    Await.result(Datasource.db.run(Projects.projects.filter(_.id === id).result.headOption), Duration.Inf) match {
      case None => BadRequest("This didn't work out how I wanted.")
      case Some(project) => {
        val uname = request.session.get("username")
        if (uname.isDefined)
          Ok(views.html.viewproject(Users.findActive(uname.get))(project))
        else
          Ok(views.html.viewproject(None)(project))
      }
    }
  }

  //Json Controllers
  import com.jeremydowens.responseutils.JsonResponses


  //POST method for creating a new post
  def newPost = withAccessLevel(privs) { user => implicit request =>
    Ok(JsonResponses.success("Placeholder")).as("application/json")
  }

  //POST method for editing an existing post
  def editPost(id: Int) = withAccessLevel(privs) { user => implicit request =>
    Ok(JsonResponses.success("Placeholder")).as("application/json")
  }

  //POST method for creating a new project
  def newProject = withAccessLevel(privs) { user => implicit request =>
    Ok(JsonResponses.success("Placeholder")).as("application/json")
  }

  //POST method for editing an existing project
  def editProject(id: Int) = withAccessLevel(privs) { user => implicit request =>
    Ok(JsonResponses.success("Placeholder")).as("application/json")
  }
}
