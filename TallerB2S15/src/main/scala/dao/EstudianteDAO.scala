package dao

import doobie.free.connection.ConnectionIO
import cats.effect.IO
import cats.implicits.*
import config.DataBase
import doobie.implicits.*
import config.DataBase
import models.Estudiante
import fs2.Stream

object EstudianteDAO {
  def insert(estudiante: Estudiante): ConnectionIO[Int] = {
    sql"""
     INSERT INTO estudiante (nombre, edad, calificacion, genero)
     VALUES (
       ${estudiante.nombre},
       ${estudiante.edad},
       ${estudiante.calificacion},
       ${estudiante.genero}
     )
   """.update.run
  }

  def insertAll(estudiante: List[Estudiante]): IO[List[Int]] = {
    DataBase.transactor.use { xa =>
      estudiante.traverse(e => insert(e).transact(xa))
    }
  }


  def findAllStream: Stream[IO, Estudiante] = {
    // Creamos un stream a partir del recurso del transactor
    Stream.resource(DataBase.transactor).flatMap { xa =>
      sql"SELECT nombre, edad, calificacion, genero FROM estudiante"
        .query[Estudiante]
        .stream
        .transact(xa)
    }
  }

}