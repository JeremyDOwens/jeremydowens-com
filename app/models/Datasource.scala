package models

import slick.jdbc.PostgresProfile.api._

object Datasource {
  //construct a URI FROM heroku's environment variable
  val dbUri = new java.net.URI(System.getenv("DATABASE_URL"))
  val dbUrl = System.getenv("JDBC_DATABASE_URL")
  val connectionPool = new slick.jdbc.DatabaseUrlDataSource()

  //Extract values from Uri to set the auth info for the data source
  if (dbUri.getUserInfo != null) {
    connectionPool.setUser(dbUri.getUserInfo.split(":")(0))
    connectionPool.setPassword(dbUri.getUserInfo.split(":")(1))
  }

  connectionPool.setDriverClassName("org.postgresql.Driver")
  connectionPool.setUrl(dbUrl)

  //set static database reference using connectionPool
  //connections are limited at 20 by the Heroku plan
  val db = Database.forDataSource(Datasource.connectionPool, Option(20))
}
