package controllers

import play.api.mvc._
import models._
import play.api.data._
import org.mindrot.jbcrypt.BCrypt
import javax.inject._
import play.api.data.Forms._
import play.api.i18n.I18nSupport
import play.api.libs.json._
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.Await
import scala.concurrent.duration._
import org.apache.commons.mail._

trait Secured extends AbstractController {

  //Defining user levels.
  // Assuming there aren't going to be 10,000 different levels
  val userLevels = Map(
    "admin" -> 0, //highest access level
    //Add more access levels here
    "public" -> 9999 //general access level
  )

  def username(request: RequestHeader) = request.session.get("username")

  def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Auth.login())


  /*
  Defining a function that executes a security check before running a standard
  controller function. In this case, withAuth takes a function mapping a String to
  a function that maps a Request to a Result - a standard controller. This then
  replaces the Action call within a standard controller function.

  For example: A controller function without an auth check looks like this
  def index = Action {implicit request =>
    Ok() // return a Result
  }
  We extend this using withAuth, and the function looks like this
  def index = withAuth { user => implicit request =>
    Ok()
  }

  This succeeds if it passes the Security.Authenticated check, and passes the username
  forward from the cookie.
  */

  def withAuth(f: => String => Request[AnyContent] => Result) = {
    Security.Authenticated(username, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }


  /*
  Extending the withAuth function to allow for checking that the username in the cookie
  matches an active account. If so, it passes the User object forward. Otherwise it redirects
  to the onUnauthorized option.
   */
  def withUser(f: User => Request[AnyContent] => Result) = withAuth { username => implicit request =>
    Users.findActive(username).map { user =>
      f(user)(request)
    }.getOrElse(onUnauthorized(request))
  }

  /*
  Extending with User to check for specific access levels
  Calls withUser, and verifies that the user's role is low enough to access
   */

  def withAccessLevel(i: Int)(f: User => Request[AnyContent] => Result) = withUser { user => implicit request =>
    userLevels.get(user.role) match {
        //if role name can be mapped to a value, and that value is at or below
        //the required threshold, pass through
      case Some(level) if level <= i => f(user)(request)
      case _ => onUnauthorized(request)
    }
  }

  // Require user to be member of a list, regardless of access level

  def asUser(unames: List[String])(f: User => Request[AnyContent] => Result) = withUser { user => implicit request =>
    if (unames.contains(user.uname)) f(user)(request)
    else onUnauthorized(request)
  }

  //Require user to have one of the specific roles in a list
  def withRole(roles: List[String])(f: User => Request[AnyContent] => Result)= withUser { user => implicit request =>
    if (roles.contains(user.role)) f(user)(request)
    else onUnauthorized(request)
  }


}

class Auth @Inject()(val cc: ControllerComponents) extends AbstractController(cc) with I18nSupport {

  val loginForm = Form(
    tuple(
      "username" -> text,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (name, password) => Auth.check(name, password)
    })
  )

  //POST call for requesting an account
  def createAccount(uname: String, email: String) = Action { implicit request =>
    val emailtest = email.split("@")
    if (email.split("@").length != 2)
      Ok(JsObject(Seq("error" -> JsString("Improper Email")))).as("application/json")
    else {
      val verification = new SimpleEmail()
      val tempPw = Auth.tempPassword(12);
      verification.setHostName("smtp.gmail.com")
      verification.setSmtpPort(465)
      verification.setAuthenticator(new DefaultAuthenticator(System.getenv("DNR_EMAIL"), System.getenv("DNR_PASSWORD")))
      verification.setSSLOnConnect(true)
      verification.setFrom(System.getenv("DNR_EMAIL"))
      verification.setSubject("Your www.jeremydowens.com password.")
      verification.setMsg("Thank you for setting up an account with www.jeremydowens.com.\n\nYou can sign in with your email address and this password: " + tempPw)
      verification.addTo(email)
      verification.send()
      Await.result(Datasource.db.run(DBIO.seq(
        Users.users += User(
          0,
          uname,
          BCrypt.hashpw(tempPw, BCrypt.gensalt()),
          email,
          "public",
          Auth.tempPassword(128),
          new java.sql.Timestamp(new java.util.Date().getTime()),
          false
        )
      )), Duration.Inf)
      Ok(JsObject(Seq("success" -> JsString("Check your email for your password.")))).as("application/json")
    }

  }
  //GET call for serving the login page
  def login = Action { implicit request =>
    Ok(views.html.login(loginForm,"Log In Please"))
  }
  //POST call for sending login credentials
  def authenticate = Action {implicit request =>
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors, "Invalid Username or Password")),
      user => Redirect(routes.HomeController.index()).withSession("username" -> user._1)
    )
  }

  def logout = Action {
    Redirect(routes.HomeController.index()).withNewSession.flashing(
      "success" -> "You are now logged out."
    )
  }
}

object Auth {
  def check(username: String, password: String): Boolean = {
    Users.findActive(username) match {
      case None => false
      case Some(user) => BCrypt.checkpw(password, user.password)
    }
  }


  //set of characters to be used to generate passwords
  val pwChars: Array[Char] = "ijklmnopqr&^@56789#NOPQRSTUVWABCD!*EFGHIJKLMstuvwxyz01234XYZabcdefgh".toCharArray

  def tempPassword(x: Int) = {

    val sec = new java.security.SecureRandom()

    //Generate a x-length character array by randomly assigning values from pwChars

    Array.fill(x) {
      pwChars(Math.abs(sec.nextInt()) % pwChars.length)
    } mkString("")
  }
}