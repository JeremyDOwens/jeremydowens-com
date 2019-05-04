package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import org.mindrot.jbcrypt.BCrypt


trait Secured extends AbstractController {

  //Defining user levels.
  // Assuming there aren't going to be 10,000 different levels
  val userlevels = Map(
    "admin" -> 0, //highest access level
    //Add more access levels here
    "public" -> 9999 //general access level
  )


}