package models

import slick.jdbc.PostgresProfile.api._

case class User(
                 id: Int,
                 name: String,
                 email: String,
                 role:String,
                 tempLink: String,
                 linkDate: java.sql.Timestamp,
                 active: Boolean
               )