ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.7"

val circeVersion = "0.14.10"

lazy val root = (project in file("."))
  .settings(
    name := "TallerB2S15",
    libraryDependencies ++= Seq(
      "org.gnieh" %% "fs2-data-csv" % "1.11.1",
      "org.gnieh" %% "fs2-data-csv-generic" % "1.11.1", // Para derivación automática
      "co.fs2" %% "fs2-core" % "3.12.2",
      "co.fs2" %% "fs2-io" % "3.12.2",
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
      "org.tpolecat" %% "doobie-core" % "1.0.0-RC11",      // Dependencias de doobie
      "org.tpolecat" %% "doobie-hikari" % "1.0.0-RC11",    // Para gestión de conexiones
      "com.mysql" % "mysql-connector-j" % "9.1.0",       // Driver para MySQL
      "com.typesafe" % "config"           % "1.4.2",       // Para gestión de archivos de configuración
      "org.slf4j" % "slf4j-simple" % "2.0.16"              // Implementaciónd de loggin
    )
  )

