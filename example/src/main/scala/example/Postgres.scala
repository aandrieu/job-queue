package example

import org.postgresql.ds.PGSimpleDataSource

object Postgres {
  val dataSource: PGSimpleDataSource = new PGSimpleDataSource()
  dataSource.setServerName("localhost")
  dataSource.setPortNumber(5432)
  dataSource.setUser("postgres")
  dataSource.setDatabaseName("postgres")
  dataSource.setCurrentSchema("public")
}
