
---

# TallerB2S15

## Descripción general

Este proyecto implementa un flujo de procesamiento (streaming) donde:

1. Se lee un archivo **CSV** ubicado en `src/main/resources/data/Estudiantes.csv`.
2. Cada fila se decodifica a un objeto Scala `Estudiante`.
3. Cada `Estudiante` se inserta en una base de datos **MySQL** usando **Doobie** (con **HikariCP** para el pool de conexiones).
4. Al final, se realiza una consulta `SELECT` y se imprime la lista de estudiantes desde la base.

El objetivo es aplicar un enfoque funcional:

* **Cats Effect (IO/Resource)** para controlar efectos y recursos.
* **FS2 Stream** para procesar datos en flujo (fila por fila).
* **fs2-data-csv** para decodificar CSV con encabezados.
* **Doobie** para acceso a datos funcional (ConnectionIO + transact).

---

## Estructura del proyecto

Estructura típica (puede variar levemente según tu repositorio, pero la lógica es la misma):

```
src/
  main/
    resources/
      application.conf
      data/
        Estudiantes.csv
    scala/
      config/
        DataBase.scala
      dao/
        EstudianteDAO.scala
      models/
        Estudiante.scala
      StreamingInsertMain.scala
```

**Paquetes principales:**

* `config`: configuración y creación del transactor (conexión/pool).
* `models`: modelos de dominio (case classes).
* `dao`: operaciones a base de datos (inserción y consulta).
* `StreamingInsertMain`: ejecución principal, lectura de CSV y orquestación.

---

## Requisitos

* Java 17+ (o el que estés usando en tu entorno SBT).
* Scala 3 (según tu código, por `given` y sintaxis).
* MySQL corriendo localmente.
* Base de datos creada (ej: `my_db1`).
* Tabla `estudiante` creada.

---

## Configuración de base de datos

En `src/main/resources/application.conf` se define el bloque `db`:

```hocon
db {
  driver = "com.mysql.cj.jdbc.Driver"
  url = "jdbc:mysql://localhost:3306/my_db1"
  user = "root"
  password = "root"
}

org.slf4j.simpleLogger.defaultLogLevel = "OFF"
```

**Notas:**

* `ConfigFactory.load().getConfig("db")` lee exactamente ese bloque.
* Se desactiva el log por defecto de `slf4j-simple` para no ensuciar la consola.

---

## Script SQL (tabla requerida)

Ejecuta esto en MySQL antes de correr el programa:

```sql
CREATE DATABASE IF NOT EXISTS my_db1;
USE my_db1;

CREATE TABLE IF NOT EXISTS estudiante (
  id INT AUTO_INCREMENT PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  edad INT NOT NULL,
  calificacion INT NOT NULL,
  genero VARCHAR(10) NOT NULL
);
```

---

## Archivo CSV

Ubicación usada por el programa:

```
src/main/resources/data/Estudiantes.csv
```

Contenido (con encabezados):

```csv
nombre,edad,calificacion,genero
Andrés,10,20,M
Ana,11,19,F
Luis,9,18,M
Cecilia,9,18,F
Katy,11,15,F
Jorge,8,17,M
Rosario,11,18,F
Nieves,10,20,F
Pablo,9,19,M
Daniel,10,20,M
```

---

## Explicación del código y la lógica

### 1) `models/Estudiante.scala`

```scala
package models

case class Estudiante(
  nombre: String,
  edad: Int,
  calificacion: Int,
  genero: String
)
```

* Es el **modelo de dominio**.
* `case class` permite crear objetos inmutables y facilita mapeo desde CSV y consultas SQL.

---

### 2) `config/DataBase.scala` (Transactor / Pool de conexiones)

```scala
package config

import cats.effect.{IO, Resource}
import com.typesafe.config.ConfigFactory
import doobie.hikari.HikariTransactor
import scala.concurrent.ExecutionContext

object DataBase {
  private val connectEC: ExecutionContext = ExecutionContext.global

  def transactor: Resource[IO, HikariTransactor[IO]] = {
    val config = ConfigFactory.load().getConfig("db")
    HikariTransactor.newHikariTransactor[IO](
      config.getString("driver"),
      config.getString("url"),
      config.getString("user"),
      config.getString("password"),
      connectEC
    )
  }
}
```

**Qué hace y por qué así:**

* `Resource[IO, HikariTransactor[IO]]` representa un recurso que **debe abrirse y cerrarse correctamente**.
* `HikariTransactor` crea un **pool de conexiones** (HikariCP), más eficiente que abrir/cerrar conexión en cada query.
* `ExecutionContext.global` se usa para las operaciones JDBC que necesitan un `ExecutionContext`.

**Idea clave:**
El transactor es la “puerta” para convertir programas `ConnectionIO[A]` (Doobie) en `IO[A]` ejecutable, usando `.transact(xa)`.

---

### 3) `dao/EstudianteDAO.scala` (operaciones SQL)

```scala
package dao

import doobie.free.connection.ConnectionIO
import cats.effect.IO
import cats.implicits.*
import config.DataBase
import doobie.implicits.*
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
    Stream.resource(DataBase.transactor).flatMap { xa =>
      sql"SELECT nombre, edad, calificacion, genero FROM estudiante"
        .query[Estudiante]
        .stream
        .transact(xa)
    }
  }
}
```

#### `insert(estudiante): ConnectionIO[Int]`

* Devuelve un programa Doobie **sin ejecutar todavía**.
* `ConnectionIO[Int]` significa: “esto, cuando se ejecute con una conexión, devuelve un Int”.
* El `Int` suele ser el número de filas afectadas (normalmente 1 por inserción).

#### `insertAll(List[Estudiante]): IO[List[Int]]`

* Aquí sí se ejecuta, porque:

  * Se abre el transactor con `DataBase.transactor.use { xa => ... }`
  * Se transforma cada `ConnectionIO` a `IO` usando `.transact(xa)`
  * `traverse` ejecuta la inserción para cada elemento y acumula resultados.

#### `findAllStream: Stream[IO, Estudiante]`

* Crea un stream que consulta estudiantes y los emite como flujo.
* Usa `Stream.resource(DataBase.transactor)` para obtener el transactor como recurso dentro del stream.
* `.query[Estudiante]` mapea cada fila del `SELECT` a `Estudiante`.

---

### 4) `StreamingInsertMain.scala` (orquestación: CSV → INSERT → SELECT → Print)

```scala
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

  private val path2DataFile2 = "src/main/resources/data/Estudiantes.csv"

  given CsvRowDecoder[Estudiante, String] = deriveCsvRowDecoder

  private def estudianteStream: fs2.Stream[IO, Estudiante] =
    Files[IO]
      .readAll(Path(path2DataFile2))
      .through(decodeUsingHeaders[Estudiante](','))

  def imprimirEstudiantes(): IO[Unit] = {
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

  override def run: IO[Unit] = {
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
    .flatMap(_ => IO.println("--- Inserción completada, iniciando lectura ---"))
    .handleErrorWith(e => IO.println(s"Error crítico durante la ejecución: ${e.getMessage}"))
    .flatMap(_ => imprimirEstudiantes())
    .flatMap(_ => IO.println("Proceso completo con éxito."))
    .handleErrorWith(e => IO.println(s"Error crítico: ${e.getMessage}"))
  }
}
```

#### Lectura del CSV en stream

* `Files[IO].readAll(Path(...))` produce un `Stream[IO, Byte]` (bytes del archivo).
* `.through(decodeUsingHeaders[Estudiante](','))`:

  * Interpreta el archivo como CSV con encabezados.
  * Usa `,` como separador.
  * Convierte cada fila a `Estudiante` mediante el `CsvRowDecoder`.

#### `given CsvRowDecoder[Estudiante, String] = deriveCsvRowDecoder`

* Genera automáticamente el decodificador basado en los nombres de columnas.
* Requiere que el encabezado tenga campos compatibles con `Estudiante`:

  * `nombre`, `edad`, `calificacion`, `genero`.

#### Inserción fila por fila (streaming real)

Dentro de `run`:

* `DataBase.transactor.use { xa => ... }` abre el transactor una vez y lo cierra al final.
* `estudianteStream.evalMap { es => ... }`:

  * `evalMap` permite ejecutar efectos (`IO`) por cada elemento del stream.
  * Por cada estudiante:

    1. `EstudianteDAO.insert(es)` crea `ConnectionIO[Int]`.
    2. `.transact(xa)` lo convierte a `IO[Int]` ejecutable.
    3. Luego imprime confirmación.

#### Compilar y ejecutar el Stream

* `.compile.drain` significa:

  * “ejecuta el stream completo”
  * “ignora el resultado final” (solo importa el efecto: insertar e imprimir)

#### Lectura final desde la BD

`imprimirEstudiantes()`:

* Consume `findAllStream` y usa `evalMap` para imprimir cada estudiante.
* Al final hace `.compile.drain` para ejecutar el stream.

---

## Cómo ejecutar

### Con SBT

En la raíz del proyecto:

```bash
sbt run
```

Salida esperada (aproximada):

* Mensajes de “Fila procesada e insertada: ...”
* Luego el encabezado y la tabla con estudiantes consultados desde MySQL.
* Mensaje final de “Proceso completo con éxito.”

---
