package models

import slick.jdbc.PostgresProfile.api._

//Define a case class (Struct) data type for the return values of table queries
case class Post(id: Int, title: String, date: java.sql.Timestamp, section: Int, body: String, author: Int)
//Define the table within the database, and map it to the case class
class Posts(tag: Tag) extends Table[Post](tag, "post") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc) //Serial column
  def title = column[String]("title")
  def date = column[java.sql.Timestamp]("date")
  def project = column[Int]("project")
  def body = column[String]("body")
  def author = column[Int]("author")

  def * = (id, title, date, project, body, author) <> (Post.tupled, Post.unapply)
}

/*
This object stores some simple queries that will commonly be used for the table.
 */
object Posts {
  val posts = TableQuery[Posts] //SELECT * FROM posts;

  //SELECT * FROM posts WHERE id = x
  def getById(x: Int) = posts.filter(_.id === x)

  //SELECT * FROM posts ORDER BY date DESC LIMIT x;
  def getMostRecent(x: Int) = posts.sortBy(_.date.desc).take(x)
}