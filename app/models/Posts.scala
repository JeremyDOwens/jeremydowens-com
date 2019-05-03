package models

import slick.jdbc.PostgresProfile.api._

//Define a case class (Struct) data type for the return values of table queries
case class Post(id: Int, title: String, date: java.sql.Timestamp, section: Int, body: String)
//Define the table within the database, and map it to the case class
class Posts(tag: Tag) extends Table[Post](tag, "posts") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc) //Serial column
  def title = column[String]("title")
  def date = column[java.sql.Timestamp]("date")
  def section = column[Int]("section")
  def body = column[String]("body")

  def * = (id, title, date,section,body) <> (Post.tupled, Post.unapply)
}

/*
This object stores some simple queries that will commonly be used for the table.
 */
object Posts {
  val posts = TableQuery[Posts] //SELECT * FROM posts;

  //SELECT * FROM posts WHERE id = x
  def getById(x: Int) = posts.filter(_.id === x)

  //SELECT * FROM posts ORDER BY date DESC LIMIT 1;
  def getMostRecent() = posts.sortBy(_.date.desc).take(1)
}