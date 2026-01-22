import cats.effect.{IO, IOApp}
import fs2.io.file.{Files, Path}
import fs2.data.csv.*
import fs2.data.csv.generic.semiauto.*
import fs2.data.text.utf8.*
import doobie.implicits.*
import models.Estudiante
import dao.EstudianteDAO
import config.DataBase

object StreamingInsertMain extends IOApp.Simple {
  // Ruta al archivo CSV definida en los recursos [cite: 2, 7]
  private val path2DataFile2 = "src/main/resources/data/Estudiantes.csv"

  // Decodificador automático para convertir filas CSV a objetos Temperatura [cite: 4, 5]
  given CsvRowDecoder[Estudiante, String] = deriveCsvRowDecoder

  /**
   * Crea un flujo (Stream) que lee el archivo y lo decodifica fila a fila
   */
  private def estudianteStream: fs2.Stream[IO, Estudiante] =
    Files[IO]
      .readAll(Path(path2DataFile2))
      .through(decodeUsingHeaders[Estudiante](','))

  def imprimirEstudiantes(): IO[Unit] = {
    // Encabezado elegante
    val header = f"${"NOMBRE"}%-20s | ${"EDAD"}%-5s | ${"CALIF."}%-7s | ${"GÉNERO"}%-10s"
    val separator = "-" * header.length

    IO.println(s"\n$separator") *>
      IO.println(header) *>
      IO.println(s"$separator") *>
      EstudianteDAO.findAllStream
        .evalMap { e =>
          IO.println(f"${e.nombre}%-20s | ${e.edad}%5d | ${e.calificacion}%5d | ${e.genero}%-10s")
        }
        .compile
        .drain *>
      IO.println(s"$separator\n")
  }
  /**
   * Ejecución principal del programa
   */
  override def run: IO[Unit] = {
    // 1. Proceso de inserción desde el CSV
    DataBase.transactor.use { xa =>
        estudianteStream
          .evalMap { es =>
            EstudianteDAO.insert(es)
              .transact(xa)
              .flatMap(_ => IO.println(s"Fila procesada e insertada: ${es.nombre}"))

          }
          .compile
          .drain
      }
      // 2. Encadenamos la impresión usando flatMap (o *> para obviar el resultado)
      .flatMap(_ => IO.println("--- Inserción completada, iniciando lectura ---"))
      .handleErrorWith(e => IO.println(s"Error crítico durante la ejecución: ${e.getMessage}"))
      .flatMap(_ => imprimirEstudiantes())
      .flatMap(_ => IO.println("Proceso completo con éxito."))
      .handleErrorWith(e => IO.println(s"Error crítico: ${e.getMessage}"))
  }

}