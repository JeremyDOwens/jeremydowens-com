package models

import slick.jdbc.PostgresProfile.api._

//Define a case class (Struct) data type for the return values of table queries
case class PostSection(id: Int, name: String, description: String)
//Define the table within the database, and map it to the case class
class PostSections(tag: Tag) extends Table[PostSection](tag, "postsections") {
  def id = column[Int]("id", O.PrimaryKey, O.AutoInc) //Serial column
  def name = column[String]("name")
  def description = column[String]("description")

  def * = (id, name, description) <> (PostSection.tupled, PostSection.unapply)
}

object PostSections {
  val postSections = TableQuery[PostSections] //SELECT * FROM postsections;

}