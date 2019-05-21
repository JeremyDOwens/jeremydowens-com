package models

import slick.jdbc.PostgresProfile.api._

//Define a case class (Struct) data type for the return values of table queries
case class SearchTag(id: Int, name: String, description: String)
//Define the table within the database, and map it to the case class
class Tags(tag: Tag) extends Table[SearchTag](tag, "tag") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc) //Serial column
  def name = column[String]("name")
  def description = column[String]("description")

  def * = (id, name, description) <> (SearchTag.tupled, SearchTag.unapply)
}

case class ProjTag(project: Int, ptag: Int)
class ProjectTags(tag: Tag) extends Table[ProjTag](tag, "project_tag") {
  def project = column[Int]("project")
  def ptag = column[Int]("tag")

  def * = (project, ptag) <> (ProjTag.tupled, ProjTag.unapply)
}

case class PostTag(post: Int, ptag: Int)
class PostTags(tag: Tag) extends Table[PostTag](tag, "post_tag") {
  def post = column[Int]("project")
  def ptag = column[Int]("tag")

  def * = (post, ptag) <> (PostTag.tupled, PostTag.unapply)
}



/*
This object stores some simple queries that will commonly be used for the table.
 */
object Tags {
  val tags = TableQuery[Tags] //SELECT * FROM tag;
  val projTags = TableQuery[ProjectTags] // SELECT * FROM project_tag
  val postTags = TableQuery[PostTags]  // SELECT * FROM post_tag
}