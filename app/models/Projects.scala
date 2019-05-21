package models

import slick.jdbc.PostgresProfile.api._

//Define a case class (Struct) data type for the return values of table queries
case class Project(id: Int, title: String, git: String, description: String)

//Define the table within the database, and map it to the case class
class Projects(tag: Tag) extends Table[Project](tag, "projects") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc) //Serial column
  def title = column[String]("title")
  def git = column[String]("git")
  def description = column[String]("description")

  def * = (id, title, git, description) <> (Project.tupled, Project.unapply)
}


case class ProjectTag(id: Int, pID: Int, pTag: String)
//Table to store descriptors for projects, representing a a many-many relationship
class ProjectTags(tag: Tag) extends Table[ProjectTag](tag, "projecttags") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc)
  def pID = column[Int]("pid")
  def pTag = column[String]("tag")


  def * = (id, pID, pTag) <> (ProjectTag.tupled, ProjectTag.unapply)
}


/*
This object stores some simple queries that will commonly be used for the table.
 */
object Projects {
  val projects = TableQuery[Projects] //SELECT * FROM projects;
  val projectTags = TableQuery[ProjectTags] //SELECT * FROM projecttags;

  //SELECT * FROM projects WHERE id = x
  def getById(x: Int) = projects.filter(_.id === x)

}