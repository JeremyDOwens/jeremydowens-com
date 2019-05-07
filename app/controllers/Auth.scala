package controllers

import play.api.mvc._
import models._
import org.mindrot.jbcrypt.BCrypt
import javax.inject._
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
    "contributor" -> 10,
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
import play.api.Configuration
class Auth @Inject()(val cc: ControllerComponents, config: Configuration) extends AbstractController(cc) with I18nSupport with Secured {
  //defining constants for generated strings
  val pwLength = 12
  val tempLinkLength = 32
  //POST call for requesting an account

  def createAccount = Action { implicit request =>
    request.body.asJson match {
      case Some(jsBody) => {
        val email = (jsBody \ "email").asOpt[String].getOrElse("")
        val uname = (jsBody \ "uname").asOpt[String].getOrElse("")
        if (email.split("@").length != 2)
          Ok(JsObject(Seq("error" -> JsString("Improper Email Address")))).as("application/json")
        else if (uname.length < 12 || 6 > uname.length)
          Ok(JsObject(Seq("error" -> JsString("Username must be between 6 and 12 characters")))).as("application/json")
        else if (Await.result(Datasource.db.run(Users.users.filter(_.uname === uname).result.headOption), Duration.Inf).isDefined)
          Ok(JsObject(Seq("error" -> JsString("Username is taken.")))).as("application/json")
        else {
          val tempPw = Auth.tempPassword(pwLength)
          val activationLink = Auth.tempPassword(tempLinkLength)
          //May factor out email sending into a standalone function in the future

          //attempt to send verification email
          try {

            val verification = new SimpleEmail()
            //Using environment variables to store email server and account information
            verification.setHostName(System.getenv("DNR_MAIL_SERVER"))
            verification.setSmtpPort(System.getenv("DNR_MAIL_PORT").toInt)
            verification.setAuthenticator(new DefaultAuthenticator(System.getenv("DNR_EMAIL"), System.getenv("DNR_PASSWORD")))
            verification.setSSLOnConnect(true)
            verification.setFrom(System.getenv("DNR_EMAIL"))
            //Simple email subject and message referencing application.conf for the siteAddress
            verification.setSubject("Welcome to "+ config.get("siteAddress") + ".")
            verification.setMsg("Thank you for setting up an account with " + config.get("siteAddress") + "\n\nYou can sign in with your email address and this password: " + tempPw + "" +
              "\n\nPlease use the following link to activate your account: https://" + config.get("siteAddress") + "/activate/" + activationLink)
            verification.addTo(email)
            verification.send()
          }
          catch {
            //If email fails, return an error message
            case e: EmailException =>
              Ok(JsObject(Seq("error" -> JsString("Unable to send activation email.")))).as("application/json")
          }
          Await.result(Datasource.db.run(DBIO.seq(
            Users.users += User(
              0,
              uname,
              BCrypt.hashpw(tempPw, BCrypt.gensalt()),
              email,
              "public",
              activationLink,
              new java.sql.Timestamp(new java.util.Date().getTime),
              active = false
            )
          )), Duration.Inf)
          Ok(JsObject(Seq("success" -> JsString("Check your email for your password.")))).as("application/json")
        }
      }
      case None => Ok(JsObject(Seq("error" -> JsString("No body sent with request.")))).as("application/json")
    }
  }


  //GET call to active a user account if they use the 32 char tempLink
  def activate(code: String) = Action { implicit request =>
    val query = for {
      u <- Users.users if u.tempLink === code  //check if the code matches a user's templink
    } yield u.active
    //update the users with the templink, to make them active users and return the number of rows affected
    val result = Await.result(Datasource.db.run(query.update(true)), Duration.Inf)
    if (result == 1) Ok(views.html.login(None)(Some("Thank you for activating your account. Sign in with your assigned password.")))
    else Ok(views.html.login(None)(Some("We are unable to activate your account at this time.")))
  }


  //GET call for serving the login page
  def login = Action { implicit request =>
    val uname = request.session.get("username")
    if (uname.isDefined)
      Ok(views.html.login(Users.findActive(uname.get))(None))
    else
      Ok(views.html.login(None)(None))
  }
  //POST call for sending login credentials
  def authenticate = Action {implicit request =>
    request.body.asJson match {
      case None => Ok(JsObject(Seq("error" -> JsString("No body sent with request.")))).as("application/json")
      case Some(jsBody) => {
        if (Auth.check((jsBody \ "email").asOpt[String].getOrElse(""), (jsBody \ "password").asOpt[String].getOrElse("")))
            Ok(JsObject(Seq("success" -> JsString("You have successfully logged in."))))
              .as("application/json")
              .withSession("username" -> (jsBody \ "email").as[String])
        else
            Ok(JsObject(Seq("error" -> JsString("Invalid Email or Password."))))
              .as("application/json")
      }
    }
  }

  def logout = withUser { user=> implicit request =>
    Redirect(routes.HomeController.index()).withNewSession.flashing(
      "success" -> s"You are now logged out, ${user.uname}."
    )
  }
}

object Auth {
  def check(email: String, password: String): Boolean = {
    Users.findActive(email) match {
      case None => false
      case Some(user) => BCrypt.checkpw(password, user.password)
    }
  }


  //set of characters to be used to generate passwords
  val pwChars: Array[Char] = "ijklmnopqr56789NOPQRS$-_+!*'()TUVWABCDEFGHIJKLMstuvwxyz01234XYZabcdefgh".toCharArray

  def tempPassword(x: Int) = {

    val sec = new java.security.SecureRandom()

    //Generate a x-length character array by randomly assigning values from pwChars

    Array.fill(x) {
      pwChars(Math.abs(sec.nextInt()) % pwChars.length)
    } mkString("")
  }
}