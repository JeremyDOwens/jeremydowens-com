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
import com.jeremydowens.responseutils.JsonResponses

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
  val siteName = config.underlying.getString("sitevars.siteAddress")

  //GET request for page to change passwords
  def pwChangePage = Action { implicit request =>
    Ok(views.html.changepass(Users.findActive(request.session.get("username").getOrElse("")))(None))
  }

  //POST request for changing passwords
  def changePassword = withUser { user => implicit request =>
    request.body.asJson match {
      case Some(jsBody) => {
        val oldpw = (jsBody \ "oldpw").asOpt[String].getOrElse("")
        val newpw = (jsBody \ "newpw").asOpt[String].getOrElse("")
        //Prefer not to have the word password, or variants in a password
        val pass = "(P|p)(A|a|4)(S|s|\\$)(S|s|\\$)".r
        //General password quality, 1 lowercase, 1 uppercase, 1 digit, 8-12 chars long
        val qualCheck = "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z]).{8,12}$".r

        if (pass.findFirstMatchIn(newpw).isDefined){
          Ok(JsonResponses.error("Password cannot contain the word \"pass\"."))
            .as("application/json")
        }
        else if (qualCheck.findFirstMatchIn(newpw).isEmpty)
          Ok(JsonResponses.error("Password must be between 8 and 12 characters long, and contain at least one upper case letter, lower case letter, and numeric digit."))
            .as("application/json")
        else if (oldpw != "" && newpw != "")
          if (Auth.check(user.uname, oldpw)){
            val result = Await.result(Users.updatePw(user.id, BCrypt.hashpw(newpw, BCrypt.gensalt())), Duration.Inf)
            if (result == 1)
              Ok(JsonResponses.success("Password has been successfully changed."))
                .as("application/json")
            else
              Ok(JsonResponses.error("Unable to update password. Contact administrator."))
                .as("application/json")
          }
          else
            Ok(JsonResponses.error("Incorrect old password, please try again."))
              .as("application/json")
        else
          Ok(JsonResponses.error("Invalid entries."))
            .as("application/json")
      }
      case None => Ok(JsonResponses.error("No body sent with request."))
        .as("application/json")
    }
  }


  //POST request for requesting an account
  def createAccount = Action { implicit request =>
    request.body.asJson match {
      case Some(jsBody) => {
        val email = (jsBody \ "email").asOpt[String].getOrElse("")
        val uname = (jsBody \ "uname").asOpt[String].getOrElse("")
        if (email.split("@").length != 2)
          Ok(JsonResponses.error("Improper Email Address")).as("application/json")
        else if (uname.length < 12 || 6 > uname.length)
          Ok(JsonResponses.error("Username must be between 6 and 12 characters")).as("application/json")
        else if (Await.result(Datasource.db.run(Users.users.filter(_.uname === uname).result.headOption), Duration.Inf).isDefined)
          Ok(JsonResponses.error("Username is taken.")).as("application/json")
        else {
          val tempPw = Auth.tempPassword(pwLength)
          val activationLink = Auth.tempPassword(tempLinkLength)
          //May factor out email sending into a standalone function in the future

          //attempt to send verification email
          try {
            val verification = Auth.buildBaseAuthEmail()
            //Simple email subject and message referencing application.conf for the siteAddress
            verification.setSubject(s"Welcome to ${siteName}.")
            val sb = new StringBuilder()
            sb.append(s"Thank you for setting up an account with ${siteName}\n\n")
            sb.append(s"You can sign in with your email address and this password: ${tempPw}\n\n")
            sb.append(s"Please use the following link to activate your account: https://${siteName}/activate/${activationLink}")
            verification.setMsg(sb.mkString)
            verification.addTo(email)
            verification.send()
          }
          catch {
            //If email fails, return an error message
            case e: EmailException =>
              Ok(JsonResponses.error("Unable to send activation email.")).as("application/json")
            case e: Exception =>
              Ok(JsonResponses.error("Unknown error.")).as("application/json")
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
          Ok(JsonResponses.success("Check your email for your password.")).as("application/json")
        }
      }
      case None => Ok(JsonResponses.error("No body sent with request.")).as("application/json")
    }
  }


  //GET request to active a user account if they use the 32 char tempLink
  def activate(code: String) = Action { implicit request =>
    val query = for {
      u <- Users.users if u.tempLink === code  //check if the code matches a user's templink
    } yield u.active
    //update the users with the templink, to make them active users and return the number of rows affected
    val result = Await.result(Datasource.db.run(query.update(true)), Duration.Inf)
    if (result == 1) Ok(views.html.login(None)(Some("Thank you for activating your account. Sign in with your assigned password.")))
    else Ok(views.html.login(None)(Some("We are unable to activate your account at this time.")))
  }


  //GET request for serving the login page
  def login = Action { implicit request =>
    val uname = request.session.get("username")
    if (uname.isDefined)
      Ok(views.html.login(Users.findActive(uname.get))(None))
    else
      Ok(views.html.login(None)(None))
  }
  //POST request for sending login credentials
  def authenticate = Action {implicit request =>
    request.body.asJson match {
      case None => Ok(JsonResponses.error("No body sent with request.")).as("application/json")
      case Some(jsBody) =>
        if (Auth.check((jsBody \ "email").asOpt[String].getOrElse(""), (jsBody \ "password").asOpt[String].getOrElse("")))
            Ok(JsonResponses.success("You have successfully logged in."))
              .as("application/json")
              .withSession("username" -> (jsBody \ "email").as[String])
        else
            Ok(JsonResponses.error("Invalid Email or Password."))
              .as("application/json")
    }
  }

  //GET request for logging out of the site
  def logout = withUser { user=> implicit request =>
    Redirect(routes.HomeController.index()).withNewSession.flashing(
      "success" -> s"You are now logged out, ${user.uname}."
    )
  }
  //POST request for getting a new password
  def getNewPassword = Action { implicit request =>
    request.body.asJson match {
      case Some(jsBody) => {
        val email = (jsBody \ "email").asOpt[String]
        val user = Users.findActive(email.getOrElse(""))
        if (user.isDefined) {
          val tempPw = Auth.tempPassword(pwLength)
          val updateResult = Await.result(Users.updatePw(user.get.id, BCrypt.hashpw(tempPw,BCrypt.gensalt())), Duration. Inf)
          try {

            val newPwEmail = Auth.buildBaseAuthEmail()
            //Simple email subject and message referencing application.conf for the siteAddress
            newPwEmail.setSubject(s"Welcome to ${siteName}.")
            val str = new StringBuilder()
            str.append(s"Your new password for ${siteName} is: ${tempPw}\n\n")
            str.append(s"You can log in at: https://${siteName}/login \n\n")
            newPwEmail.setMsg(str.toString())
            newPwEmail.addTo(user.get.uname)
            newPwEmail.send()
          }
          catch {
            //If email fails, return an error message
            case e: EmailException =>
              Ok(JsonResponses.error("Unable to send activation email.")).as("application/json")
            case e: Exception =>
              Ok(JsonResponses.error("Unknown error.")).as("application/json")
          }
          if (updateResult == 1) Ok(JsonResponses.success("Check your email for your new password.")).as("application/json")
          else Ok(JsonResponses.error("Unable to change password.")).as("application/json")
        } else {
          Ok(JsonResponses.error("Error validating user. Did you activate the account?")).as("application/json")
        }
      }
      case None =>
        Ok(JsonResponses.error("No email address supplied.")).as("application/json")
    }
  }
}

object Auth {
  def check(email: String, password: String): Boolean = {
    Users.findActive(email) match {
      case None => false
      case Some(user) => BCrypt.checkpw(password, user.password)
    }
  }

  //Setup for SimpleEmail using environment variables for auth-related emails
  def buildBaseAuthEmail(): SimpleEmail = {
    val baseEmail = new SimpleEmail()
    //Using environment variables to store email server and account information
    baseEmail.setHostName(System.getenv("DNR_MAIL_SERVER"))
    baseEmail.setSmtpPort(System.getenv("DNR_MAIL_PORT").toInt)
    baseEmail.setAuthenticator(new DefaultAuthenticator(System.getenv("DNR_EMAIL"), System.getenv("DNR_PASSWORD")))
    baseEmail.setSSLOnConnect(true)
    baseEmail.setFrom(System.getenv("DNR_EMAIL"))
    baseEmail
  }


  //set of characters to be used to generate passwords
  val pwChars: Array[Char] = "ijklmnopqr56789NOPQRS$-_+!*'()TUVWABCDEFGHIJKLMstuvwxyz01234XYZabcdefgh".toCharArray

  def tempPassword(x: Int) = {

    val sec = new java.security.SecureRandom()

    //Generate a x-length character array by randomly assigning values from pwChars

    Array.fill(x) {
      pwChars(Math.abs(sec.nextInt()) % pwChars.length)
    } mkString ""
  }
}