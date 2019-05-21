package models

import slick.jdbc.PostgresProfile.api._

//Define a case class (Struct) data type for the return values of table queries
case class Project(id: Int, title: String, git: String, description: String, owner: Int)

//Define the table within the database, and map it to the case class
class Projects(tag: Tag) extends Table[Project](tag, "project") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc) //Serial column
  def title = column[String]("title")
  def git = column[String]("git")
  def description = column[String]("description")
  def owner = column[Int]("owner")

  def * = (id, title, git, description, owner) <> (Project.tupled, Project.unapply)
}

/*
This object stores some simple queries that will commonly be used for the table.
 */
object Projects {
  val projects = TableQuery[Projects] //SELECT * FROM projects;

  //SELECT * FROM projects WHERE id = x
  def getById(x: Int) = projects.filter(_.id === x)

}